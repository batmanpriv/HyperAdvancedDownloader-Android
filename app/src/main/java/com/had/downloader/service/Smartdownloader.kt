package com.had.downloader.service

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import androidx.documentfile.provider.DocumentFile
import kotlin.math.min

data class ChunkInfo(
    val index: Int,
    val start: Long,
    val end: Long,
    var downloaded: Long = 0L,
    var status: ChunkStatus = ChunkStatus.PENDING,
    var speedBps: Long = 0L,
    var retryCount: Int = 0,
    var lastError: String? = null
)

enum class ChunkStatus { PENDING, DOWNLOADING, DONE, FAILED, RETRYING }

data class DownloadConfig(
    val url: String,
    val outputPath: String,
    val threads: Int = 4,
    val proxy: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val cookies: String? = null,
    val userAgent: String = DEFAULT_USER_AGENT,
    val method: String = "GET",
    val useResume: Boolean = true,
    val maxRetries: Int = 5,
    val timeoutMs: Int = 30_000,
    val mirrors: List<String> = emptyList(),
    val checksumAlgo: String? = null,
    val checksumExpected: String? = null,
    val scheduleFrom: String? = null,
    val scheduleTo: String? = null,
    val maxSpeedBps: Long = 0L,
    val sessionDir: String? = null
)

data class DownloadProgress(
    val totalBytes: Long,
    val downloadedBytes: Long,
    val speedBps: Long,
    val etaSeconds: Int,
    val percent: Float,
    val chunks: List<ChunkInfo>,
    val activeThreads: Int,
    val status: String,
    val mirrors: List<MirrorInfo> = emptyList(),
    val retryAttempts: Int = 0
)

data class MirrorInfo(val url: String, val latencyMs: Long, val available: Boolean)

data class MetaInfo(
    val fileName: String,
    val contentLength: Long,
    val acceptsRanges: Boolean,
    val contentType: String,
    val redirectUrl: String?,
    val finalUrl: String
)

private const val TAG = "SmartDownloader"
const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

private const val MIN_BUFFER = 8 * 1024
private const val MAX_BUFFER = 4 * 1024 * 1024
private const val INITIAL_BUFFER = 64 * 1024
private const val SPEED_SAMPLE_MS = 500L
private const val SESSION_EXT = ".hadsess"
private const val RETRY_INTERVAL_MS = 10_000L
private const val MAX_SMART_RETRIES = 20
private const val SESSION_SAVE_INTERVAL_MS = 5000L

class TokenBucket(private val ratePerSecond: Long) {
    private val tokens = AtomicLong(ratePerSecond)
    private var lastRefill = System.currentTimeMillis()
    private val lock = Mutex()

    suspend fun consume(bytes: Int) {
        if (ratePerSecond <= 0L) return
        lock.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRefill
            if (elapsed > 0) {
                val newTokens = (ratePerSecond * elapsed / 1000L).coerceAtMost(ratePerSecond)
                val current = tokens.get()
                val updated = (current + newTokens).coerceAtMost(ratePerSecond)
                tokens.set(updated)
                lastRefill = now
            }
            val needed = bytes.toLong()
            var available = tokens.get()
            if (available < needed) {
                val deficit = needed - available
                val waitMs = (deficit * 1000L / ratePerSecond).coerceAtLeast(1L)
                tokens.set(0L)
                delay(waitMs)
                available = 0L
            } else {
                tokens.addAndGet(-needed)
            }
        }
    }
}

data class DownloadSession(
    val url: String,
    val outputPath: String,
    val totalBytes: Long,
    val chunks: List<ChunkInfo>,
    val lastSaveTime: Long = System.currentTimeMillis()
) {
    fun serialize(): String = buildString {
        appendLine(url)
        appendLine(outputPath)
        appendLine(totalBytes)
        appendLine(System.currentTimeMillis())
        chunks.forEach { c ->
            appendLine("${c.index},${c.start},${c.end},${c.downloaded},${c.status.name},${c.retryCount}")
        }
    }

    companion object {
        fun deserialize(data: String): DownloadSession? = runCatching {
            val lines = data.lines().filter { it.isNotBlank() }
            if (lines.size < 5) return null
            val url = lines[0]
            val outputPath = lines[1]
            val totalBytes = lines[2].toLong()
            val lastSaveTime = lines[3].toLongOrNull() ?: System.currentTimeMillis()
            val chunks = lines.drop(5).map { line ->
                val p = line.split(",")
                ChunkInfo(
                    p[0].toInt(),
                    p[1].toLong(),
                    p[2].toLong(),
                    p[3].toLong(),
                    if (p[4] == "DONE") ChunkStatus.DONE else ChunkStatus.PENDING,
                    retryCount = p.getOrNull(5)?.toIntOrNull() ?: 0
                )
            }
            DownloadSession(url, outputPath, totalBytes, chunks, lastSaveTime)
        }.getOrNull()
    }
}

fun sanitizeUrl(raw: String): String {
    val trimmed = raw.trim()
    return runCatching { URL(trimmed).toURI().toString() }.getOrElse {
        runCatching {
            val u = URL(trimmed)
            URI(u.protocol, u.userInfo, u.host, u.port, u.path, u.query, u.ref).toASCIIString()
        }.getOrDefault(trimmed)
    }
}

@Singleton
class SmartDownloader @Inject constructor() {

    private val parentScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, e ->
            
            android.util.Log.e("HAD", "Unhandled exception in SmartDownloader scope: ${e.message}", e)
        }
    )

    @Volatile
    private var activeJobs = mapOf<Long, Job>()
    private val jobLock = Any()

    private val _progress = MutableSharedFlow<Pair<Long, DownloadProgress>>(extraBufferCapacity = 512)
    val progress: SharedFlow<Pair<Long, DownloadProgress>> = _progress.asSharedFlow()

    private val _result = MutableSharedFlow<Pair<Long, DownloadResult>>(extraBufferCapacity = 64)
    val result: SharedFlow<Pair<Long, DownloadResult>> = _result.asSharedFlow()

    var sessionDir: File? = null

    private fun sessionFileFor(id: Long): File? {
        val dir = sessionDir ?: return null
        dir.mkdirs()
        return File(dir, "dl_${id}${SESSION_EXT}")
    }

    fun startDownload(id: Long, config: DownloadConfig) {
        cancelDownload(id)

        val job = parentScope.launch {
            val result = runCatching {
                downloadFile(id, config)
            }.getOrElse { e ->
                if (e is CancellationException) DownloadResult.Cancelled
                else DownloadResult.Failed(e.message ?: "Unknown error")
            }
            _result.emit(id to result)
            synchronized(jobLock) { activeJobs = activeJobs - id }
        }

        synchronized(jobLock) { activeJobs = activeJobs + (id to job) }
    }

    fun cancelDownload(id: Long) {
        val job = synchronized(jobLock) {
            val j = activeJobs[id]
            activeJobs = activeJobs - id
            j
        }
        job?.cancel(CancellationException("Stopped by user"))
    }

    fun stopAll() {
        val jobs = synchronized(jobLock) {
            val j = activeJobs.toMap()
            activeJobs = emptyMap()
            j
        }
        jobs.values.forEach { it.cancel() }
    }

    fun deleteSession(id: Long) {
        sessionFileFor(id)?.delete()
    }

    fun hasSession(id: Long): Boolean = sessionFileFor(id)?.exists() == true

    fun getSessionProgress(id: Long): Pair<Long, Float>? {
        val f = sessionFileFor(id) ?: return null
        if (!f.exists()) return null
        val session = DownloadSession.deserialize(f.readText()) ?: return null
        if (session.totalBytes <= 0L) return null
        val downloaded = session.chunks.sumOf { c ->
            if (c.status == ChunkStatus.DONE) c.end - c.start + 1 else c.downloaded
        }
        val pct = downloaded.toFloat() / session.totalBytes
        return downloaded to pct
    }

    private suspend fun downloadFile(id: Long, config: DownloadConfig): DownloadResult {
        emit(id, 0L, 0L, 0L, 0, 0f, emptyList(), 0, "CONNECTING")

        val safeUrl = sanitizeUrl(config.url)
        val allUrls = listOf(safeUrl) + config.mirrors.map { sanitizeUrl(it) }
        val mirrors = probeMirrors(allUrls, config)
        var bestUrl = mirrors.filter { it.available }.minByOrNull { it.latencyMs }?.url ?: safeUrl

        val meta = fetchMeta(bestUrl, config)
        val outputFile = resolveOutputFile(config.outputPath, meta.fileName)
        val tempFile = File(outputFile.parent, ".${outputFile.name}.tmp")
        val sessionFile = sessionFileFor(id)

        val tokenBucket = if (config.maxSpeedBps > 0L) TokenBucket(config.maxSpeedBps) else null

        val existingFileSize = if (tempFile.exists()) tempFile.length() else 0L

        val session = if (config.useResume && sessionFile != null && sessionFile.exists()) {
            DownloadSession.deserialize(sessionFile.readText())
                ?.takeIf { it.url == bestUrl && it.totalBytes == meta.contentLength }
        } else null

        val totalBytes = meta.contentLength

        if (totalBytes <= 0L || !meta.acceptsRanges) {
            val resumeFrom = if (config.useResume && existingFileSize > 0 && session == null) {
                existingFileSize
            } else {
                0L
            }
            return downloadSingleStreamWithSmartRetry(
                id = id,
                url = bestUrl,
                config = config,
                outputFile = tempFile,
                totalBytes = totalBytes,
                mirrors = mirrors,
                resumeFrom = resumeFrom,
                tokenBucket = tokenBucket
            )
        }

        val dynamicThreads = chooseDynamicThreads(totalBytes, config.threads)

        val chunks: List<ChunkInfo> = when {
            session != null -> {
                session.chunks.map { c ->
                    c.copy(
                        status = if (c.status == ChunkStatus.DONE) ChunkStatus.DONE else ChunkStatus.PENDING,
                        speedBps = 0L,
                        retryCount = c.retryCount
                    )
                }
            }
            config.useResume && existingFileSize > 0 && existingFileSize < totalBytes -> {
                recoverChunksFromFile(existingFileSize, totalBytes, dynamicThreads)
            }
            else -> splitChunks(totalBytes, dynamicThreads)
        }

        if (!tempFile.exists() || tempFile.length() != totalBytes) {
            if (tempFile.exists() && tempFile.length() == existingFileSize && existingFileSize > 0 && session == null) {
            } else {
                RandomAccessFile(tempFile, "rw").use { it.setLength(totalBytes) }
            }
        }

        if (session != null || (config.useResume && existingFileSize > 0)) {
            val alreadyDone = chunks.sumOf { c ->
                if (c.status == ChunkStatus.DONE) c.end - c.start + 1 else c.downloaded
            }
            val pct = if (totalBytes > 0) alreadyDone.toFloat() / totalBytes else 0f
            emit(id, totalBytes, alreadyDone, 0L, -1, pct, chunks, 0, "DOWNLOADING", mirrors)
        }

        var lastSessionSave = System.currentTimeMillis()
        if (sessionFile != null && session == null) {
            saveSession(sessionFile, bestUrl, tempFile.absolutePath, totalBytes, chunks)
        }

        val speedMap = ConcurrentHashMap<Int, Long>()
        var currentThreads = dynamicThreads
        var globalRetryAttempts = 0
        var currentMirrorIndex = 0

        val pendingChunks = chunks.filter { it.status != ChunkStatus.DONE }.toMutableList()

        while (pendingChunks.isNotEmpty()) {
            val remainingChunks = pendingChunks.filter { it.status != ChunkStatus.DONE }

            if (remainingChunks.isEmpty()) break

            var chunkErrors = 0
            val chunkMutex = Mutex()

            val downloadJobs = remainingChunks.map { chunk ->
                parentScope.async {
                    downloadChunkWithSmartRetry(
                        id = id,
                        chunk = chunk,
                        url = bestUrl,
                        config = config,
                        outputFile = tempFile,
                        tokenBucket = tokenBucket,
                        totalBytes = totalBytes,
                        chunks = chunks,
                        mirrors = mirrors,
                        onSpeed = { bps ->
                            speedMap[chunk.index] = bps
                        },
                        onProgress = {
                            val done = chunks.sumOf { c ->
                                if (c.status == ChunkStatus.DONE) c.end - c.start + 1 else c.downloaded
                            }
                            val speed = speedMap.values.sum()
                            val eta = if (speed > 0 && totalBytes > done) ((totalBytes - done) / speed).toInt() else -1
                            val pct = if (totalBytes > 0) done.toFloat() / totalBytes else 0f
                            val totalRetries = chunks.sumOf { it.retryCount }
                            val activeThreads = chunks.count { it.status == ChunkStatus.DOWNLOADING || it.status == ChunkStatus.RETRYING }

                            parentScope.launch {
                                emit(id, totalBytes, done, speed, eta, pct, chunks, activeThreads,
                                    if (totalRetries > 0) "RETRYING:$totalRetries" else "DOWNLOADING", mirrors, totalRetries)
                            }

                            val now = System.currentTimeMillis()
                            if (sessionFile != null && now - lastSessionSave > SESSION_SAVE_INTERVAL_MS) {
                                saveSession(sessionFile, bestUrl, tempFile.absolutePath, totalBytes, chunks)
                                lastSessionSave = now
                            }
                        }
                    )
                }
            }

            val results = downloadJobs.map { it.await() }

            val failedChunks = chunks.filter {
                it.status == ChunkStatus.FAILED &&
                        it.retryCount < MAX_SMART_RETRIES
            }

            if (failedChunks.isNotEmpty()) {
                globalRetryAttempts++

                failedChunks.forEach { chunk ->
                    chunk.status = ChunkStatus.RETRYING
                    chunk.retryCount++
                }

                currentMirrorIndex = (currentMirrorIndex + 1) % mirrors.size
                if (currentMirrorIndex < mirrors.size && mirrors[currentMirrorIndex].available) {
                    bestUrl = mirrors[currentMirrorIndex].url
                    Log.d(TAG, "Switching to mirror: $bestUrl")
                }

                emit(id, totalBytes,
                    chunks.sumOf { if (it.status == ChunkStatus.DONE) it.end - it.start + 1 else it.downloaded },
                    0L, -1,
                    chunks.sumOf { if (it.status == ChunkStatus.DONE) it.end - it.start + 1 else it.downloaded }.toFloat() / totalBytes,
                    chunks, currentThreads,
                    "RETRYING:${failedChunks.size} chunks, attempt $globalRetryAttempts",
                    mirrors, globalRetryAttempts
                )

                delay(RETRY_INTERVAL_MS)

                if (currentCoroutineContext().isActive.not()) {
                    break
                }

                continue
            } else {
                break
            }
        }

        val allDone = chunks.all { it.status == ChunkStatus.DONE }
        if (!allDone) {
            val stillFailed = chunks.filter { it.status == ChunkStatus.FAILED }
            if (stillFailed.isNotEmpty()) {
                val fallbackSuccess = downloadFullFileFallback(
                    id = id,
                    url = bestUrl,
                    config = config,
                    outputFile = tempFile,
                    totalBytes = totalBytes,
                    mirrors = mirrors,
                    tokenBucket = tokenBucket
                )

                if (fallbackSuccess) {
                    chunks.forEach { it.status = ChunkStatus.DONE }
                    emit(id, totalBytes, totalBytes, 0L, 0, 1f, chunks, 1, "COMPLETED", mirrors, globalRetryAttempts)
                } else {
                    emit(id, totalBytes,
                        chunks.sumOf { it.downloaded },
                        0L, -1,
                        chunks.sumOf { it.downloaded }.toFloat() / totalBytes,
                        chunks, 0,
                        "FAILED: Fallback download failed",
                        mirrors, globalRetryAttempts
                    )
                    return DownloadResult.Failed("Fallback download failed", chunks.sumOf { it.downloaded })
                }
            }
        }

        if (allDone) {
            if (totalBytes > 0 && tempFile.length() != totalBytes) {
                return DownloadResult.Failed("File size mismatch: expected $totalBytes, got ${tempFile.length()}", tempFile.length())
            }

            if (config.checksumAlgo != null && config.checksumExpected != null) {
                emit(id, totalBytes, totalBytes, 0L, 0, 0.95f, chunks, 0, "VERIFYING", mirrors, globalRetryAttempts)

                val hash = calculateChecksum(tempFile, config.checksumAlgo)
                if (hash == null || !hash.equals(config.checksumExpected, ignoreCase = true)) {
                    return DownloadResult.Failed("Checksum mismatch: expected ${config.checksumExpected}, got $hash", tempFile.length())
                }
            }

            tempFile.renameTo(outputFile)
            emit(id, totalBytes, totalBytes, 0L, 0, 1f, chunks, 0, "COMPLETED", mirrors, globalRetryAttempts)

            if (sessionFile != null) saveSession(sessionFile, bestUrl, outputFile.absolutePath, totalBytes, chunks)

            return DownloadResult.Success
        } else {
            return DownloadResult.Failed("Download incomplete", chunks.sumOf { it.downloaded })
        }
    }

    private suspend fun downloadSingleStreamWithSmartRetry(
        id: Long,
        url: String,
        config: DownloadConfig,
        outputFile: File,
        totalBytes: Long,
        mirrors: List<MirrorInfo>,
        resumeFrom: Long = 0L,
        tokenBucket: TokenBucket? = null
    ): DownloadResult {
        var downloaded = resumeFrom
        var retryCount = 0
        var lastError: String? = null
        var currentMirrorIndex = 0

        while (retryCount < MAX_SMART_RETRIES && (totalBytes <= 0 || downloaded < totalBytes)) {
            currentCoroutineContext().ensureActive()

            try {
                val currentUrl = if (currentMirrorIndex < mirrors.size && mirrors[currentMirrorIndex].available) {
                    mirrors[currentMirrorIndex].url
                } else url

                val conn = openConnection(currentUrl, config, rangeStart = if (downloaded > 0) downloaded else -1L)
                val responseCode = conn.responseCode

                if (downloaded > 0 && responseCode != 206) {
                    Log.w(TAG, "Server doesn't support resume, restarting from 0")
                    downloaded = 0L
                    outputFile.delete()
                    conn.disconnect()
                    continue
                }

                var lastTime = System.currentTimeMillis()
                var lastBytes = downloaded
                var bufferSize = INITIAL_BUFFER
                var buf = ByteArray(bufferSize)

                val outputStream = if (downloaded > 0 && outputFile.exists()) {
                    FileOutputStream(outputFile, true)
                } else {
                    FileOutputStream(outputFile)
                }

                conn.inputStream.buffered(bufferSize).use { input ->
                    outputStream.use { output ->
                        var n: Int
                        var bytesReadInThisAttempt = 0L
                        var stallCount = 0

                        while (input.read(buf).also { n = it } != -1) {
                            currentCoroutineContext().ensureActive()
                            tokenBucket?.consume(n)
                            output.write(buf, 0, n)
                            downloaded += n
                            bytesReadInThisAttempt += n

                            val now = System.currentTimeMillis()
                            val delta = now - lastTime
                            if (delta >= SPEED_SAMPLE_MS) {
                                val speed = if (bytesReadInThisAttempt > 0)
                                    (downloaded - lastBytes) * 1000L / delta
                                else 0L

                                if (speed == 0L && bytesReadInThisAttempt > 0) {
                                    stallCount++
                                } else {
                                    stallCount = 0
                                }

                                if (stallCount >= 5) {
                                    throw IOException("Download stalled")
                                }

                                lastBytes = downloaded
                                lastTime = now

                                bufferSize = adaptBuffer(bufferSize, speed)
                                if (buf.size != bufferSize) {
                                    buf = ByteArray(bufferSize)
                                }

                                val effectiveTotal = if (totalBytes > 0) totalBytes else maxOf(downloaded, 1L)
                                val pct = if (effectiveTotal > 0) downloaded.toFloat() / effectiveTotal else 0f
                                val eta = if (speed > 0 && totalBytes > downloaded)
                                    ((totalBytes - downloaded) / speed).toInt()
                                else -1

                                emit(
                                    id, effectiveTotal, downloaded, speed, eta,
                                    pct,
                                    listOf(ChunkInfo(0, 0L, effectiveTotal - 1, downloaded, retryCount = retryCount)),
                                    1,
                                    if (retryCount > 0) "RETRYING:$retryCount" else "DOWNLOADING",
                                    mirrors, retryCount
                                )
                            }
                        }

                        conn.disconnect()
                        if (totalBytes > 0 && downloaded >= totalBytes) {
                            return DownloadResult.Success
                        }
                        return DownloadResult.Success
                    }
                }

            } catch (e: Exception) {
                lastError = e.message
                retryCount++

                if (totalBytes > 0 && downloaded >= totalBytes) {
                    return DownloadResult.Success
                }

                if (retryCount >= MAX_SMART_RETRIES) {
                    try {
                        outputFile.delete()
                        val freshConn = openConnection(url, config, rangeStart = -1L)
                        var downloadedFresh = 0L
                        var lastTime = System.currentTimeMillis()
                        var lastBytes = 0L
                        var bufferSize = INITIAL_BUFFER
                        var buf = ByteArray(bufferSize)

                        FileOutputStream(outputFile).use { output ->
                            freshConn.inputStream.use { input ->
                                var n: Int
                                while (input.read(buf).also { n = it } != -1) {
                                    currentCoroutineContext().ensureActive()
                                    output.write(buf, 0, n)
                                    downloadedFresh += n

                                    val now = System.currentTimeMillis()
                                    val delta = now - lastTime
                                    if (delta >= SPEED_SAMPLE_MS) {
                                        val speed = (downloadedFresh - lastBytes) * 1000L / delta
                                        lastBytes = downloadedFresh
                                        lastTime = now

                                        bufferSize = adaptBuffer(bufferSize, speed)
                                        if (buf.size != bufferSize) {
                                            buf = ByteArray(bufferSize)
                                        }

                                        val effectiveTotal = if (totalBytes > 0) totalBytes else maxOf(downloadedFresh, 1L)
                                        val pct = if (effectiveTotal > 0) downloadedFresh.toFloat() / effectiveTotal else 0f
                                        emit(
                                            id, effectiveTotal, downloadedFresh, speed, -1, pct,
                                            listOf(ChunkInfo(0, 0L, effectiveTotal - 1, downloadedFresh, retryCount = retryCount)),
                                            1, "FALLBACK:${downloadedFresh}/${effectiveTotal}", mirrors, retryCount
                                        )
                                    }
                                }
                            }
                        }
                        freshConn.disconnect()

                        if (totalBytes > 0 && downloadedFresh >= totalBytes) {
                            return DownloadResult.Success
                        }
                        return DownloadResult.Success
                    } catch (fallbackError: Exception) {
                        return DownloadResult.Failed(fallbackError.message ?: "Fallback failed", downloaded)
                    }
                }

                delay(RETRY_INTERVAL_MS)

                if (e.message?.contains("416") == true || e.message?.contains("Requested Range") == true) {
                    downloaded = 0L
                    outputFile.delete()
                }

                currentMirrorIndex = (currentMirrorIndex + 1) % (mirrors.size + 1)
            }
        }

        if (totalBytes > 0 && downloaded < totalBytes) {
            return DownloadResult.Failed("Download failed after $retryCount attempts", downloaded)
        }
        return DownloadResult.Success
    }

    private suspend fun downloadFullFileFallback(
        id: Long,
        url: String,
        config: DownloadConfig,
        outputFile: File,
        totalBytes: Long,
        mirrors: List<MirrorInfo>,
        tokenBucket: TokenBucket?
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                outputFile.delete()
                val conn = openConnection(url, config, rangeStart = -1L)
                var downloaded = 0L
                var lastTime = System.currentTimeMillis()
                var lastBytes = 0L
                var bufferSize = INITIAL_BUFFER
                var buf = ByteArray(bufferSize)

                FileOutputStream(outputFile).use { output ->
                    conn.inputStream.use { input ->
                        var n: Int
                        while (input.read(buf).also { n = it } != -1) {
                            if (currentCoroutineContext().isActive.not()) {
                                return@withContext false
                            }
                            tokenBucket?.consume(n)
                            output.write(buf, 0, n)
                            downloaded += n

                            val now = System.currentTimeMillis()
                            val delta = now - lastTime
                            if (delta >= SPEED_SAMPLE_MS) {
                                val speed = (downloaded - lastBytes) * 1000L / delta
                                lastBytes = downloaded
                                lastTime = now

                                bufferSize = adaptBuffer(bufferSize, speed)
                                if (buf.size != bufferSize) {
                                    buf = ByteArray(bufferSize)
                                }

                                val effectiveTotal = if (totalBytes > 0) totalBytes else maxOf(downloaded, 1L)
                                val pct = if (effectiveTotal > 0) downloaded.toFloat() / effectiveTotal else 0f
                                emit(
                                    id, effectiveTotal, downloaded, speed, -1, pct,
                                    listOf(ChunkInfo(0, 0L, effectiveTotal - 1, downloaded)),
                                    1, "FALLBACK:${downloaded}/${effectiveTotal}", mirrors, 0
                                )
                            }
                        }
                    }
                }
                conn.disconnect()
                return@withContext true
            } catch (e: Exception) {
                return@withContext false
            }
        }
    }

    private suspend fun downloadChunkWithSmartRetry(
        id: Long,
        chunk: ChunkInfo,
        url: String,
        config: DownloadConfig,
        outputFile: File,
        tokenBucket: TokenBucket?,
        totalBytes: Long,
        chunks: List<ChunkInfo>,
        mirrors: List<MirrorInfo>,
        onSpeed: (Long) -> Unit,
        onProgress: () -> Unit
    ) = withContext(Dispatchers.IO) {
        var retries = chunk.retryCount
        val maxRetries = MAX_SMART_RETRIES
        val retryDelay = RETRY_INTERVAL_MS

        while (retries <= maxRetries) {
            currentCoroutineContext().ensureActive()

            try {
                chunk.status = if (retries > 0) ChunkStatus.RETRYING else ChunkStatus.DOWNLOADING
                chunk.lastError = null

                val rangeStart = chunk.start + chunk.downloaded
                val conn = openConnection(url, config, rangeStart, chunk.end)
                val responseCode = conn.responseCode

                if (chunk.downloaded > 0 && responseCode != 206) {
                    Log.w(TAG, "Chunk ${chunk.index}: Server doesn't support resume, resetting")
                    chunk.downloaded = 0L
                    conn.disconnect()
                    continue
                }

                var bufferSize = INITIAL_BUFFER
                var buf = ByteArray(bufferSize)
                var lastTime = System.currentTimeMillis()
                var lastBytes = chunk.downloaded
                var bytesReadInThisAttempt = 0L
                var stallCount = 0

                RandomAccessFile(outputFile, "rw").use { raf ->
                    raf.seek(rangeStart)
                    conn.inputStream.use { input ->
                        var n: Int
                        while (input.read(buf).also { n = it } != -1) {
                            currentCoroutineContext().ensureActive()
                            tokenBucket?.consume(n)
                            raf.write(buf, 0, n)

                            val now = System.currentTimeMillis()
                            val delta = now - lastTime

                            synchronized(chunk) {
                                chunk.downloaded += n
                                bytesReadInThisAttempt += n
                            }

                            if (delta >= SPEED_SAMPLE_MS) {
                                val speed = if (bytesReadInThisAttempt > 0)
                                    (chunk.downloaded - lastBytes) * 1000L / delta
                                else 0L

                                if (speed == 0L && bytesReadInThisAttempt > 0) {
                                    stallCount++
                                } else {
                                    stallCount = 0
                                }

                                if (stallCount >= 5) {
                                    throw IOException("Chunk stalled")
                                }

                                synchronized(chunk) {
                                    chunk.speedBps = speed
                                }
                                lastBytes = chunk.downloaded
                                lastTime = now

                                bufferSize = adaptBuffer(bufferSize, speed)
                                if (buf.size != bufferSize) {
                                    buf = ByteArray(bufferSize)
                                }

                                onSpeed(speed)
                                onProgress()
                            }
                        }
                    }
                }
                conn.disconnect()

                synchronized(chunk) {
                    chunk.status = ChunkStatus.DONE
                }
                onProgress()
                return@withContext

            } catch (e: Exception) {
                if (e is CancellationException) throw e

                synchronized(chunk) {
                    chunk.lastError = e.message
                    retries++
                    chunk.retryCount = retries
                    chunk.status = ChunkStatus.FAILED

                    if (e.message?.contains("416") == true || e.message?.contains("Requested Range") == true) {
                        chunk.downloaded = 0L
                    }
                }

                if (retries > maxRetries) {
                    synchronized(chunk) {
                        chunk.status = ChunkStatus.FAILED
                    }
                    throw IOException("Chunk ${chunk.index} failed after $maxRetries attempts: ${e.message}")
                }

                delay(retryDelay)
            }
        }
    }

    private fun recoverChunksFromFile(existingSize: Long, totalBytes: Long, threads: Int): List<ChunkInfo> {
        val chunkSize = totalBytes / threads
        return (0 until threads).map { i ->
            val start = i * chunkSize
            val end = if (i == threads - 1) totalBytes - 1 else start + chunkSize - 1
            when {
                end < existingSize -> ChunkInfo(i, start, end, end - start + 1, ChunkStatus.DONE)
                start < existingSize -> ChunkInfo(i, start, end, existingSize - start, ChunkStatus.PENDING)
                else -> ChunkInfo(i, start, end, 0L, ChunkStatus.PENDING)
            }
        }
    }

    private fun openConnection(
        url: String, config: DownloadConfig,
        rangeStart: Long = -1L, rangeEnd: Long = -1L
    ): HttpURLConnection {
        val conn = buildProxy(config.proxy).let { proxy ->
            if (proxy != null) URL(url).openConnection(proxy) as HttpURLConnection
            else URL(url).openConnection() as HttpURLConnection
        }
        conn.connectTimeout = config.timeoutMs
        conn.readTimeout = config.timeoutMs
        conn.setRequestProperty("User-Agent", config.userAgent)
        conn.setRequestProperty("Accept", "*/*")
        conn.setRequestProperty("Accept-Encoding", "identity")
        if (!config.cookies.isNullOrBlank()) conn.setRequestProperty("Cookie", config.cookies)
        config.headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        if (rangeStart >= 0) {
            conn.setRequestProperty(
                "Range",
                "bytes=$rangeStart-${if (rangeEnd > 0) rangeEnd else ""}"
            )
        }
        conn.requestMethod = config.method
        conn.instanceFollowRedirects = true
        conn.connect()
        return conn
    }

    private fun fetchMeta(url: String, config: DownloadConfig): MetaInfo {
        var currentUrl = url
        repeat(10) {
            val conn = openConnection(currentUrl, config)
            val code = conn.responseCode
            if (code in 300..399) {
                currentUrl = conn.getHeaderField("Location") ?: return@repeat
                conn.disconnect()
                return@repeat
            }

            if (code == 405 || code == 403) {
                conn.disconnect()
                val rangeConn = openConnection(currentUrl, config, rangeStart = 0L, rangeEnd = 0L)
                if (rangeConn.responseCode == 206) {
                    val contentRange = rangeConn.getHeaderField("Content-Range") ?: ""
                    val size = contentRange.substringAfterLast('/').toLongOrNull() ?: -1L
                    val acceptRanges = true
                    val ct = rangeConn.contentType ?: ""
                    val disp = rangeConn.getHeaderField("Content-Disposition") ?: ""
                    val fileName = extractFileName(disp, currentUrl)
                    rangeConn.disconnect()
                    return MetaInfo(fileName, size, acceptRanges, ct, null, currentUrl)
                }
                rangeConn.disconnect()
            }

            val length = conn.contentLengthLong
            val acceptRanges = conn.getHeaderField("Accept-Ranges")?.lowercase() == "bytes"
            val contentType = conn.contentType ?: ""
            val disposition = conn.getHeaderField("Content-Disposition") ?: ""
            val fileName = extractFileName(disposition, currentUrl)
            conn.disconnect()
            return MetaInfo(fileName, length, acceptRanges, contentType, null, currentUrl)
        }
        return MetaInfo(url.substringAfterLast('/'), -1L, false, "", null, url)
    }

    private suspend fun probeMirrors(urls: List<String>, config: DownloadConfig): List<MirrorInfo> =
        withContext(Dispatchers.IO) {
            urls.map { url ->
                async {
                    val t0 = System.currentTimeMillis()
                    val ok = runCatching {
                        openConnection(url, config.copy(timeoutMs = 5000)).let {
                            val ok = it.responseCode in 200..299
                            it.disconnect()
                            ok
                        }
                    }.getOrDefault(false)
                    MirrorInfo(url, System.currentTimeMillis() - t0, ok)
                }
            }.awaitAll()
        }

    private fun splitChunks(totalBytes: Long, threads: Int): List<ChunkInfo> {
        val chunkSize = totalBytes / threads
        return (0 until threads).map { i ->
            val start = i * chunkSize
            val end = if (i == threads - 1) totalBytes - 1 else start + chunkSize - 1
            ChunkInfo(i, start, end)
        }
    }

    private fun chooseDynamicThreads(totalBytes: Long, requested: Int): Int = when {
        totalBytes < 1_048_576 -> 1
        totalBytes < 10_485_760 -> min(2, requested)
        totalBytes < 104_857_600 -> min(4, requested)
        totalBytes < 1_073_741_824 -> min(8, requested)
        else -> min(16, requested)
    }

    private fun adaptBuffer(current: Int, speedBps: Long): Int {
        val target = (speedBps / 8).toInt().coerceIn(MIN_BUFFER, MAX_BUFFER)
        return ((current + target) / 2).coerceIn(MIN_BUFFER, MAX_BUFFER)
    }

    private fun buildProxy(proxyStr: String?): Proxy? {
        if (proxyStr.isNullOrBlank()) return null
        return runCatching {
            val clean = proxyStr.removePrefix("socks5://").removePrefix("http://")
            val (host, port) = clean.split(":")
            val type = if (proxyStr.startsWith("socks")) Proxy.Type.SOCKS else Proxy.Type.HTTP
            Proxy(type, InetSocketAddress(host, port.toInt()))
        }.getOrNull()
    }

    private fun resolveOutputFile(outputPath: String, serverFileName: String): File {
        val f = File(outputPath)
        return if (f.extension.isNotBlank()) {
            f.parentFile?.mkdirs()
            f
        } else {
            f.mkdirs()
            File(f, serverFileName.ifBlank { "download" })
        }
    }

    private fun extractFileName(disposition: String, url: String): String {
        val fromDisp =
            Regex("filename\\*?=[\"']?(?:UTF-\\d'[^']*')?([^\"';\\n]+)[\"']?", RegexOption.IGNORE_CASE)
                .find(disposition)?.groupValues?.getOrNull(1)?.trim()
        return fromDisp ?: url.substringAfterLast('/').substringBefore('?').ifBlank { "download" }
    }

    private fun calculateChecksum(file: File, algo: String?): String? {
        if (algo == null) return null
        return runCatching {
            val digest = MessageDigest.getInstance(algo.uppercase())
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }.getOrNull()
    }

    private fun saveSession(
        file: File, url: String, outputPath: String,
        totalBytes: Long, chunks: List<ChunkInfo>
    ) = runCatching {
        file.writeText(DownloadSession(url, outputPath, totalBytes, chunks).serialize())
    }

    private suspend fun emitStatus(id: Long, status: String) =
        _progress.emit(id to DownloadProgress(0L, 0L, 0L, 0, 0f, emptyList(), 0, status))

    private suspend fun emit(
        id: Long, total: Long, done: Long, speed: Long, eta: Int,
        pct: Float, chunks: List<ChunkInfo>, threads: Int, status: String,
        mirrors: List<MirrorInfo> = emptyList(),
        retryAttempts: Int = 0
    ) = _progress.emit(
        id to DownloadProgress(total, done, speed, eta, pct, chunks, threads, status, mirrors, retryAttempts)
    )
}