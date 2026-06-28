package com.had.downloader.service

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

data class ChunkInfo(
    val index: Int,
    val start: Long,
    val end: Long,
    var downloaded: Long = 0L,
    var status: ChunkStatus = ChunkStatus.PENDING,
    var speedBps: Long = 0L
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
    val mirrors: List<MirrorInfo> = emptyList()
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
const val DEFAULT_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

internal val USER_AGENTS = listOf(DEFAULT_USER_AGENT)

private const val MIN_BUFFER = 8 * 1024
private const val MAX_BUFFER = 4 * 1024 * 1024
private const val INITIAL_BUFFER = 64 * 1024
private const val SPEED_SAMPLE_MS = 500L
private const val SESSION_EXT = ".hadsess"

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
            val available = tokens.get()
            if (available < needed) {
                val deficit = needed - available
                val waitMs = (deficit * 1000L / ratePerSecond).coerceAtLeast(1L)
                tokens.set(0L)
                delay(waitMs)
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
    val chunks: List<ChunkInfo>
) {
    fun serialize(): String = buildString {
        appendLine(url)
        appendLine(outputPath)
        appendLine(totalBytes)
        chunks.forEach { c ->
            appendLine("${c.index},${c.start},${c.end},${c.downloaded},${c.status.name}")
        }
    }

    companion object {
        fun deserialize(data: String): DownloadSession? = runCatching {
            val lines = data.lines().filter { it.isNotBlank() }
            val chunks = lines.drop(3).map { line ->
                val p = line.split(",")
                ChunkInfo(
                    p[0].toInt(), p[1].toLong(), p[2].toLong(), p[3].toLong(),
                    if (p[4] == "DONE") ChunkStatus.DONE else ChunkStatus.PENDING
                )
            }
            DownloadSession(lines[0], lines[1], lines[2].toLong(), chunks)
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

    private val parentScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var activeJobs = mapOf<Long, Job>()
    private val jobLock = Any()

    private val _progress = MutableSharedFlow<Pair<Long, DownloadProgress>>(extraBufferCapacity = 512)
    val progress: SharedFlow<Pair<Long, DownloadProgress>> = _progress.asSharedFlow()

    var sessionDir: File? = null

    private fun sessionFileFor(id: Long): File? {
        val dir = sessionDir ?: return null
        dir.mkdirs()
        return File(dir, "dl_${id}${SESSION_EXT}")
    }

    fun startDownload(id: Long, config: DownloadConfig) {
        cancelDownload(id)

        val job = parentScope.launch {
            runCatching {
                downloadFile(id, config)
            }.onFailure { e ->
                if (e is CancellationException) {
                    Log.d(TAG, "Download $id cancelled")
                    emitStatus(id, "CANCELLED")
                } else {
                    Log.e(TAG, "Download $id failed: ${e.message}")
                    emitStatus(id, "FAILED: ${e.message}")
                }
            }
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

    private suspend fun downloadFile(id: Long, config: DownloadConfig) {
        emit(id, 0L, 0L, 0L, 0, 0f, emptyList(), 0, "CONNECTING")

        val safeUrl = sanitizeUrl(config.url)
        val allUrls = listOf(safeUrl) + config.mirrors.map { sanitizeUrl(it) }
        val mirrors = probeMirrors(allUrls, config)
        val bestUrl = mirrors.filter { it.available }.minByOrNull { it.latencyMs }?.url ?: safeUrl

        val meta = fetchMeta(bestUrl, config)
        val outputFile = resolveOutputFile(config.outputPath, meta.fileName)
        val sessionFile = sessionFileFor(id)

        val tokenBucket = if (config.maxSpeedBps > 0L) TokenBucket(config.maxSpeedBps) else null

        val existingFileSize = if (outputFile.exists()) outputFile.length() else 0L

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
            downloadSingleStream(id, bestUrl, config, outputFile, totalBytes, mirrors, resumeFrom, tokenBucket)
            verifyAndFinalize(id, outputFile, sessionFile, config, totalBytes,
                listOf(ChunkInfo(0, 0L, -1L)), mirrors)
            return
        }

        val dynamicThreads = chooseDynamicThreads(totalBytes, config.threads)

        val chunks: List<ChunkInfo> = when {
            session != null -> {
                session.chunks.map { c ->
                    c.copy(
                        status = if (c.status == ChunkStatus.DONE) ChunkStatus.DONE else ChunkStatus.PENDING,
                        speedBps = 0L
                    )
                }
            }
            config.useResume && existingFileSize > 0 && existingFileSize < totalBytes -> {
                recoverChunksFromFile(existingFileSize, totalBytes, dynamicThreads)
            }
            else -> splitChunks(totalBytes, dynamicThreads)
        }

        if (!outputFile.exists() || outputFile.length() != totalBytes) {
            if (outputFile.exists() && outputFile.length() == existingFileSize && existingFileSize > 0 && session == null) {
            } else {
                RandomAccessFile(outputFile, "rw").use { it.setLength(totalBytes) }
            }
        }

        if (session != null || (config.useResume && existingFileSize > 0)) {
            val alreadyDone = chunks.sumOf { c ->
                if (c.status == ChunkStatus.DONE) c.end - c.start + 1 else c.downloaded
            }
            val pct = if (totalBytes > 0) alreadyDone.toFloat() / totalBytes else 0f
            emit(id, totalBytes, alreadyDone, 0L, -1, pct, chunks, 0, "DOWNLOADING", mirrors)
        }

        if (sessionFile != null) saveSession(sessionFile, bestUrl, outputFile.absolutePath, totalBytes, chunks)

        val speedMap = mutableMapOf<Int, Long>()
        val lock = Mutex()
        var currentThreads = dynamicThreads

        val pendingChunks = chunks.filter { it.status != ChunkStatus.DONE }

        pendingChunks.map { chunk ->
            parentScope.async {
                downloadChunk(
                    id, chunk, bestUrl, config, outputFile, tokenBucket,
                    onSpeed = { bps -> parentScope.launch { lock.withLock { speedMap[chunk.index] = bps } } },
                    onProgress = {
                        val done = chunks.sumOf { c ->
                            if (c.status == ChunkStatus.DONE) c.end - c.start + 1 else c.downloaded
                        }
                        val speed = speedMap.values.sum()
                        val eta = if (speed > 0 && totalBytes > done) ((totalBytes - done) / speed).toInt() else -1
                        val pct = if (totalBytes > 0) done.toFloat() / totalBytes else 0f
                        parentScope.launch {
                            emit(id, totalBytes, done, speed, eta, pct, chunks, currentThreads, "DOWNLOADING", mirrors)
                        }
                        currentThreads = adaptThreads(currentThreads, speed, config.threads * 2)
                        if (sessionFile != null) {
                            saveSession(sessionFile, bestUrl, outputFile.absolutePath, totalBytes, chunks)
                        }
                    }
                )
            }
        }.awaitAll()

        if (sessionFile != null) saveSession(sessionFile, bestUrl, outputFile.absolutePath, totalBytes, chunks)
        verifyAndFinalize(id, outputFile, sessionFile, config, totalBytes, chunks, mirrors)
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

    private suspend fun downloadSingleStream(
        id: Long, url: String, config: DownloadConfig,
        outputFile: File, totalBytes: Long, mirrors: List<MirrorInfo>,
        resumeFrom: Long = 0L,
        tokenBucket: TokenBucket? = null
    ) = withContext(Dispatchers.IO) {
        val conn = openConnection(url, config, rangeStart = if (resumeFrom > 0) resumeFrom else -1L)
        var downloaded = resumeFrom
        var lastTime = System.currentTimeMillis()
        var lastBytes = downloaded
        var bufferSize = INITIAL_BUFFER

        val outputStream = if (resumeFrom > 0 && outputFile.exists()) {
            java.io.FileOutputStream(outputFile, true)
        } else {
            outputFile.outputStream()
        }

        conn.inputStream.buffered(bufferSize).use { input ->
            outputStream.use { output ->
                val buf = ByteArray(bufferSize)
                var n: Int
                while (input.read(buf).also { n = it } != -1) {
                    currentCoroutineContext().ensureActive()
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
                        val eta = if (speed > 0 && totalBytes > downloaded) ((totalBytes - downloaded) / speed).toInt() else -1
                        val pct = if (totalBytes > 0) downloaded.toFloat() / totalBytes else 0f
                        emit(
                            id, totalBytes, downloaded, speed, eta, pct,
                            listOf(ChunkInfo(0, 0L, totalBytes - 1, downloaded)), 1, "DOWNLOADING", mirrors
                        )
                    }
                }
            }
        }
        conn.disconnect()
    }

    private suspend fun downloadChunk(
        id: Long, chunk: ChunkInfo, url: String, config: DownloadConfig, file: File,
        tokenBucket: TokenBucket?,
        onSpeed: (Long) -> Unit, onProgress: () -> Unit
    ) = withContext(Dispatchers.IO) {
        var retries = 0
        val maxRetries = config.maxRetries
        val retryDelay = 5000L

        while (retries <= maxRetries) {
            currentCoroutineContext().ensureActive()
            runCatching {
                chunk.status = if (retries > 0) ChunkStatus.RETRYING else ChunkStatus.DOWNLOADING
                val rangeStart = chunk.start + chunk.downloaded
                val conn = openConnection(url, config, rangeStart, chunk.end)
                var bufferSize = INITIAL_BUFFER
                var lastTime = System.currentTimeMillis()
                var lastBytes = chunk.downloaded

                RandomAccessFile(file, "rw").use { raf ->
                    raf.seek(rangeStart)
                    conn.inputStream.use { input ->
                        val buf = ByteArray(bufferSize)
                        var n: Int
                        while (input.read(buf).also { n = it } != -1) {
                            currentCoroutineContext().ensureActive()
                            tokenBucket?.consume(n)
                            raf.write(buf, 0, n)
                            chunk.downloaded += n

                            val now = System.currentTimeMillis()
                            val delta = now - lastTime
                            if (delta >= SPEED_SAMPLE_MS) {
                                val speed = (chunk.downloaded - lastBytes) * 1000L / delta
                                chunk.speedBps = speed
                                lastBytes = chunk.downloaded
                                lastTime = now
                                bufferSize = adaptBuffer(bufferSize, speed)
                                onSpeed(speed)
                                onProgress()
                            }
                        }
                    }
                }
                conn.disconnect()
                chunk.status = ChunkStatus.DONE
                onProgress()
                return@withContext
            }.onFailure { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Chunk ${chunk.index} retry ${retries + 1}/$maxRetries: ${e.message}")
                retries++
                chunk.status = ChunkStatus.RETRYING
                if (retries <= maxRetries) {
                    val delayTime = minOf(retryDelay * retries, 60_000L)
                    delay(delayTime)
                }
            }
        }
        chunk.status = ChunkStatus.FAILED
        throw Exception("Chunk ${chunk.index} failed after $maxRetries retries")
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

    private fun adaptThreads(current: Int, totalSpeed: Long, maxThreads: Int): Int = when {
        totalSpeed < 500_000 && current > 1 -> current - 1
        totalSpeed > 5_000_000 && current < maxThreads -> current + 1
        else -> current
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

    private fun saveSession(
        file: File, url: String, outputPath: String,
        totalBytes: Long, chunks: List<ChunkInfo>
    ) = runCatching {
        file.writeText(DownloadSession(url, outputPath, totalBytes, chunks).serialize())
    }

    private suspend fun verifyAndFinalize(
        id: Long, outputFile: File, sessionFile: File?, config: DownloadConfig,
        totalBytes: Long, chunks: List<ChunkInfo>, mirrors: List<MirrorInfo> = emptyList()
    ) {
        if (!config.checksumAlgo.isNullOrBlank() && !config.checksumExpected.isNullOrBlank()) {
            emit(id, totalBytes, totalBytes, 0L, 0, 1f, chunks, 0, "VERIFYING")
            val computed = computeHash(outputFile, config.checksumAlgo)
            if (!computed.equals(config.checksumExpected, ignoreCase = true)) {
                emit(
                    id, totalBytes, totalBytes, 0L, 0, 1f, chunks, 0,
                    "CHECKSUM_FAIL: expected=${config.checksumExpected} got=$computed"
                )
                return
            }
        }
        sessionFile?.delete()
        emit(id, totalBytes, totalBytes, 0L, 0, 1f, chunks, 0, "COMPLETED", mirrors)
    }

    private fun computeHash(file: File, algo: String): String {
        val digest = MessageDigest.getInstance(algo.uppercase().replace("-", ""))
        file.inputStream().use { input ->
            val buf = ByteArray(65536)
            var n: Int
            while (input.read(buf).also { n = it } != -1) digest.update(buf, 0, n)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private suspend fun emitStatus(id: Long, status: String) =
        _progress.emit(id to DownloadProgress(0L, 0L, 0L, 0, 0f, emptyList(), 0, status))

    private suspend fun emit(
        id: Long, total: Long, done: Long, speed: Long, eta: Int,
        pct: Float, chunks: List<ChunkInfo>, threads: Int, status: String,
        mirrors: List<MirrorInfo> = emptyList()
    ) = _progress.emit(
        id to DownloadProgress(total, done, speed, eta, pct, chunks, threads, status, mirrors)
    )
}