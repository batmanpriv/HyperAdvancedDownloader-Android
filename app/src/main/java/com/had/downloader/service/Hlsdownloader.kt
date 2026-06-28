package com.had.downloader.service

import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HlsDownloader"
private const val MAX_PARALLEL_SEGMENTS = 4
private const val MAX_RETRY = 5
private const val CONNECT_TIMEOUT = 30_000
private const val READ_TIMEOUT = 120_000
private const val TS_PACKET_SIZE = 188
private const val BUFFER_SIZE = 64 * 1024
private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
private const val LIVE_POLL_INTERVAL_MS = 3_000L
private const val LIVE_MAX_STALL_COUNT = 10

data class HlsSegment(
    val index: Int,
    val url: String,
    val duration: Double,
    val iv: ByteArray?,
    val discontinuity: Boolean = false,
    val byteRangeLength: Long = -1L,
    val byteRangeOffset: Long = -1L
)

data class HlsMediaTrack(
    val type: String,
    val groupId: String,
    val language: String?,
    val name: String,
    val default: Boolean,
    val uri: String?,
    val autoSelect: Boolean
)

data class HlsVariant(
    val bandwidth: Long,
    val resolution: String?,
    val codecs: String?,
    val url: String,
    val audioGroupId: String?,
    val subtitleGroupId: String?,
    val frameRate: Double = 0.0,
    val videoRange: String = ""
) {
    fun heightLabel(): String {
        val h = resolution?.substringAfter('x')?.toIntOrNull()
        return if (h != null) "${h}p" else "${bandwidth / 1000}kbps"
    }

    fun height(): Int = resolution?.substringAfter('x')?.toIntOrNull() ?: 0
}

data class HlsPlaylist(
    val segments: List<HlsSegment>,
    val encryptionKey: ByteArray?,
    val encryptionMethod: String?,
    val defaultIv: ByteArray?,
    val totalDuration: Double = 0.0,
    val isLive: Boolean = false,
    val isEvent: Boolean = false,
    val mediaSequence: Int = 0,
    val targetDuration: Int = 0,
    val initSegmentUrl: String? = null,
    val initSegmentRange: Pair<Long, Long>? = null,
    val audioTracks: List<HlsMediaTrack> = emptyList(),
    val subtitleTracks: List<HlsMediaTrack> = emptyList()
)

data class HlsSegmentSummary(
    val totalSegments: Int,
    val completedSegments: Int,
    val failedSegments: Int,
    val downloadingSegments: Int,
    val downloadedBytes: Long
)

enum class HlsQualityPreference {
    HIGHEST,
    LOWEST,
    AUTO_BANDWIDTH
}

@Singleton
class HlsDownloader @Inject constructor() {

    private var currentJob: Job? = null

    @Volatile
    private var isCancelled = false

    private val segmentChunks = ConcurrentHashMap<Int, ChunkInfo>()

    fun buildSummaryChunks(summary: HlsSegmentSummary, totalBytes: Long): List<ChunkInfo> {
        if (summary.totalSegments == 0) return emptyList()
        return listOf(
            ChunkInfo(
                index = 0,
                start = 0L,
                end = if (totalBytes > 0) totalBytes - 1 else summary.downloadedBytes,
                downloaded = summary.downloadedBytes,
                status = when {
                    summary.failedSegments > 0 -> ChunkStatus.FAILED
                    summary.downloadingSegments > 0 -> ChunkStatus.DOWNLOADING
                    summary.completedSegments == summary.totalSegments -> ChunkStatus.DONE
                    else -> ChunkStatus.PENDING
                },
                speedBps = 0L
            )
        )
    }

    fun selectVariant(
        variants: List<HlsVariant>,
        preference: HlsQualityPreference = HlsQualityPreference.HIGHEST,
        maxBandwidthBps: Long = 0L
    ): HlsVariant {
        if (variants.isEmpty()) throw IllegalArgumentException("No variants available")

        val sorted = variants.sortedByDescending { it.bandwidth }

        return when (preference) {
            HlsQualityPreference.HIGHEST -> sorted.first()
            HlsQualityPreference.LOWEST -> sorted.last()
            HlsQualityPreference.AUTO_BANDWIDTH -> {
                if (maxBandwidthBps > 0L) {
                    sorted.firstOrNull { it.bandwidth <= maxBandwidthBps } ?: sorted.last()
                } else {
                    sorted.first()
                }
            }
        }
    }

    suspend fun probeVariantBandwidth(variant: HlsVariant, config: DownloadConfig): Long =
        withContext(Dispatchers.IO) {
            runCatching {
                val playlist = parsePlaylist(variant.url, config)
                val testSeg = playlist.segments.firstOrNull() ?: return@runCatching 0L
                val start = System.currentTimeMillis()
                val conn = openConnection(testSeg.url, config, variant.url)
                val buf = ByteArray(32 * 1024)
                var bytesRead = 0L
                conn.inputStream.use { inp ->
                    var n = inp.read(buf)
                    while (n != -1 && bytesRead < 128 * 1024) {
                        bytesRead += n
                        n = inp.read(buf)
                    }
                }
                conn.disconnect()
                val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
                (bytesRead * 8000L / elapsed)
            }.getOrDefault(0L)
        }

    suspend fun download(
        m3u8Url: String,
        outputPath: String,
        config: DownloadConfig,
        downloadAudio: Boolean = true,
        downloadSubtitles: Boolean = false,
        qualityPreference: HlsQualityPreference = HlsQualityPreference.HIGHEST,
        onVariantsAvailable: (suspend (List<HlsVariant>) -> HlsVariant?)? = null,
        onProgress: (segmentsDone: Int, totalSegments: Int, percent: Float, status: String, chunks: List<ChunkInfo>) -> Unit
    ) {
        isCancelled = false
        segmentChunks.clear()
        currentJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                downloadInternal(
                    m3u8Url, outputPath, config,
                    downloadAudio, downloadSubtitles,
                    qualityPreference, onVariantsAvailable, onProgress
                )
            } catch (e: CancellationException) {
                onProgress(0, 0, 0f, "CANCELLED", emptyList())
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${e.message}")
                onProgress(0, 0, 0f, "FAILED: ${e.message}", emptyList())
                throw e
            }
        }
        currentJob?.join()
    }

    private suspend fun downloadInternal(
        m3u8Url: String,
        outputPath: String,
        config: DownloadConfig,
        downloadAudio: Boolean,
        downloadSubtitles: Boolean,
        qualityPreference: HlsQualityPreference,
        onVariantsAvailable: (suspend (List<HlsVariant>) -> HlsVariant?)?,
        onProgress: (Int, Int, Float, String, List<ChunkInfo>) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            onProgress(0, 0, 0f, "PARSING", emptyList())

            val masterContent = try {
                fetchText(m3u8Url, config)
            } catch (e: Exception) {
                onProgress(0, 0, 0f, "FAILED: Cannot fetch playlist: ${e.message}", emptyList())
                return@withContext
            }

            val masterLines = masterContent.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val resolvedOutput = resolveOutputPath(outputPath)
            val outputFile = File(resolvedOutput).also { it.parentFile?.mkdirs() }
            val urlHash = m3u8Url.hashCode().let { if (it < 0) "n${-it}" else "p$it" }
            val tempDir = File(outputFile.parentFile, ".had_hls_$urlHash").also { it.mkdirs() }

            if (masterLines.any { it.startsWith("#EXT-X-STREAM-INF") }) {
                val variants = parseMasterVariants(masterLines, m3u8Url)
                val allAudioTracks = parseMasterMediaTracks(masterLines, m3u8Url, "AUDIO")
                val allSubtitleTracks = parseMasterMediaTracks(masterLines, m3u8Url, "SUBTITLES")

                val selectedVariant: HlsVariant = if (onVariantsAvailable != null && variants.size > 1) {
                    onVariantsAvailable(variants) ?: selectVariant(variants, qualityPreference)
                } else {
                    if (variants.size > 1 && qualityPreference == HlsQualityPreference.AUTO_BANDWIDTH) {
                        val probedVariant = withContext(Dispatchers.IO) {
                            val midVariant = variants.sortedByDescending { it.bandwidth }
                                .getOrElse(variants.size / 2) { variants.first() }
                            val measuredBw = probeVariantBandwidth(midVariant, config)
                            Log.d(TAG, "Measured bandwidth: ${measuredBw / 1000}kbps")
                            selectVariant(variants, HlsQualityPreference.AUTO_BANDWIDTH, measuredBw)
                        }
                        Log.d(TAG, "Auto-selected variant: ${probedVariant.heightLabel()} @ ${probedVariant.bandwidth / 1000}kbps")
                        probedVariant
                    } else {
                        selectVariant(variants, qualityPreference)
                    }
                }

                Log.d(TAG, "Selected HLS variant: ${selectedVariant.heightLabel()} @ ${selectedVariant.bandwidth / 1000}kbps")

                val playlist = try {
                    parsePlaylist(selectedVariant.url, config)
                } catch (e: Exception) {
                    onProgress(0, 0, 0f, "FAILED: Cannot parse variant playlist: ${e.message}", emptyList())
                    return@withContext
                }

                val audioTracks = if (downloadAudio && selectedVariant.audioGroupId != null) {
                    allAudioTracks.filter { it.groupId == selectedVariant.audioGroupId && it.uri != null }
                } else {
                    playlist.audioTracks.filter { it.uri != null }
                }

                val subtitleTracks = if (downloadSubtitles && selectedVariant.subtitleGroupId != null) {
                    allSubtitleTracks.filter { it.groupId == selectedVariant.subtitleGroupId && it.uri != null }
                } else {
                    emptyList()
                }

                if (playlist.isLive || playlist.isEvent) {
                    downloadLive(playlist, selectedVariant.url, tempDir, outputFile, config, onProgress)
                } else {
                    downloadVod(playlist, selectedVariant.url, tempDir, outputFile, config, audioTracks, subtitleTracks, onProgress)
                }
            } else {
                val playlist = try {
                    parsePlaylistFromContent(masterContent, m3u8Url, config)
                } catch (e: Exception) {
                    onProgress(0, 0, 0f, "FAILED: Cannot parse playlist: ${e.message}", emptyList())
                    return@withContext
                }

                if (playlist.isLive || playlist.isEvent) {
                    downloadLive(playlist, m3u8Url, tempDir, outputFile, config, onProgress)
                } else {
                    downloadVod(playlist, m3u8Url, tempDir, outputFile, config, playlist.audioTracks, playlist.subtitleTracks, onProgress)
                }
            }
        }
    }

    private suspend fun downloadVod(
        playlist: HlsPlaylist,
        playlistUrl: String,
        tempDir: File,
        outputFile: File,
        config: DownloadConfig,
        audioTracks: List<HlsMediaTrack>,
        subtitleTracks: List<HlsMediaTrack>,
        onProgress: (Int, Int, Float, String, List<ChunkInfo>) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val segments = playlist.segments
            val total = segments.size

            if (total == 0) {
                onProgress(0, 0, 0f, "FAILED: No segments found in playlist", emptyList())
                return@withContext
            }

            if (playlist.initSegmentUrl != null) {
                val initFile = File(tempDir, "init.mp4")
                if (!initFile.exists() || initFile.length() == 0L) {
                    downloadInitSegment(playlist.initSegmentUrl, playlist.initSegmentRange, initFile, config, playlistUrl)
                }
            }

            val doneCnt = AtomicInteger(0)
            val totalBytesDownloaded = AtomicLong(0L)
            val totalBytesEstimated = AtomicLong(0L)
            val existingDone = AtomicInteger(0)
            val segmentFiles = arrayOfNulls<File>(total)
            val segmentSizes = LongArray(total)
            val failedCount = AtomicInteger(0)
            val downloadingCount = AtomicInteger(0)

            for (i in 0 until total) {
                if (isCancelled) {
                    onProgress(0, 0, 0f, "CANCELLED", emptyList())
                    throw CancellationException("Download cancelled")
                }
                val seg = segments[i]
                val segFile = File(tempDir, "seg_%010d.ts".format(i))
                val expectedSize = if (seg.byteRangeLength > 0) seg.byteRangeLength else -1L

                if (segFile.exists() && segFile.length() > 0 &&
                    (expectedSize < 0 || segFile.length() >= expectedSize)
                ) {
                    existingDone.incrementAndGet()
                    segmentFiles[i] = segFile
                    val size = segFile.length()
                    segmentSizes[i] = size
                    totalBytesDownloaded.addAndGet(size)
                    totalBytesEstimated.addAndGet(size)
                    segmentChunks[i] = ChunkInfo(index = i, start = 0L, end = size, downloaded = size, status = ChunkStatus.DONE, speedBps = 0L)
                } else {
                    segmentChunks[i] = ChunkInfo(index = i, start = 0L, end = 0L, downloaded = 0L, status = ChunkStatus.PENDING, speedBps = 0L)
                }
            }

            for (i in 0 until total) {
                if (segmentFiles[i] == null) {
                    val seg = segments[i]
                    val size = if (seg.byteRangeLength > 0) seg.byteRangeLength
                    else try { getSegmentSize(seg.url, config) } catch (e: Exception) { -1L }
                    if (size > 0) {
                        segmentSizes[i] = size
                        totalBytesEstimated.addAndGet(size)
                        val existing = segmentChunks[i]
                        segmentChunks[i] = existing?.copy(end = size)
                            ?: ChunkInfo(index = i, start = 0L, end = size, downloaded = 0L, status = ChunkStatus.PENDING, speedBps = 0L)
                    }
                }
            }

            if (existingDone.get() > 0) {
                doneCnt.set(existingDone.get())
                val estimated = totalBytesEstimated.get()
                val summary = makeSummary(doneCnt.get(), total, 0, 0, totalBytesDownloaded.get())
                onProgress(doneCnt.get(), total, doneCnt.get().toFloat() / total, "RESUMING:$estimated", buildSummaryChunks(summary, estimated))
            }

            val semaphore = Semaphore(MAX_PARALLEL_SEGMENTS)

            try {
                val jobs = segments.mapIndexed { index, segment ->
                    async(Dispatchers.IO) {
                        if (isCancelled) return@async
                        if (segmentFiles[index] != null) return@async

                        semaphore.withPermit {
                            if (isCancelled) return@withPermit

                            downloadingCount.incrementAndGet()
                            val existingChunk = segmentChunks[index]
                            segmentChunks[index] = existingChunk?.copy(status = ChunkStatus.DOWNLOADING, start = 0L, end = segmentSizes[index])
                                ?: ChunkInfo(index = index, start = 0L, end = segmentSizes[index], downloaded = 0L, status = ChunkStatus.DOWNLOADING, speedBps = 0L)

                            val result = downloadSegmentWithRetry(
                                segment = segment,
                                index = index,
                                tempDir = tempDir,
                                config = config,
                                referer = playlistUrl,
                                encKey = playlist.encryptionKey,
                                encMethod = playlist.encryptionMethod,
                                encIv = segment.iv ?: playlist.defaultIv
                            )

                            downloadingCount.decrementAndGet()

                            if (result != null) {
                                segmentFiles[index] = result
                                val bytes = result.length()
                                segmentSizes[index] = bytes
                                totalBytesDownloaded.addAndGet(bytes)
                                if (totalBytesEstimated.get() <= 0) totalBytesEstimated.addAndGet(bytes)
                                val done = doneCnt.incrementAndGet()
                                val estimated = totalBytesEstimated.get()
                                val c = segmentChunks[index]
                                segmentChunks[index] = c?.copy(downloaded = bytes, end = bytes, status = ChunkStatus.DONE, speedBps = 0L)
                                    ?: ChunkInfo(index = index, start = 0L, end = bytes, downloaded = bytes, status = ChunkStatus.DONE, speedBps = 0L)
                                val summary = makeSummary(done, total, failedCount.get(), downloadingCount.get(), totalBytesDownloaded.get())
                                onProgress(done, total, done.toFloat() / total, "DOWNLOADING:$estimated", buildSummaryChunks(summary, estimated))
                            } else {
                                failedCount.incrementAndGet()
                                val c = segmentChunks[index]
                                segmentChunks[index] = c?.copy(status = ChunkStatus.FAILED)
                                    ?: ChunkInfo(index = index, start = 0L, end = 0L, downloaded = 0L, status = ChunkStatus.FAILED, speedBps = 0L)
                                throw IOException("Segment $index failed after $MAX_RETRY retries")
                            }
                        }
                    }
                }
                jobs.awaitAll()
            } catch (e: CancellationException) {
                isCancelled = true
                val estimated = totalBytesEstimated.get()
                val summary = makeSummary(doneCnt.get(), total, failedCount.get(), 0, totalBytesDownloaded.get())
                onProgress(doneCnt.get(), total, doneCnt.get().toFloat() / total, "CANCELLED", buildSummaryChunks(summary, estimated))
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "HLS download failed: ${e.message}")
                val estimated = totalBytesEstimated.get()
                val summary = makeSummary(doneCnt.get(), total, failedCount.get(), 0, totalBytesDownloaded.get())
                onProgress(doneCnt.get(), total, doneCnt.get().toFloat() / total, "FAILED: ${e.message}", buildSummaryChunks(summary, estimated))
                return@withContext
            }

            val validCount = segmentFiles.count { it != null }
            if (validCount < total * 0.95) {
                val estimated = totalBytesEstimated.get()
                val summary = makeSummary(doneCnt.get(), total, failedCount.get(), 0, totalBytesDownloaded.get())
                onProgress(doneCnt.get(), total, 0f, "FAILED: Too many missing segments ($validCount/$total)", buildSummaryChunks(summary, estimated))
                return@withContext
            }

            val audioFile: File? = if (audioTracks.isNotEmpty()) {
                val track = audioTracks.firstOrNull { it.default } ?: audioTracks.first()
                if (track.uri != null) {
                    val dir = File(tempDir, "audio").also { it.mkdirs() }
                    try { downloadExternalTrack(track.uri, dir, config, playlistUrl, "audio") }
                    catch (e: Exception) { Log.w(TAG, "Audio track failed: ${e.message}"); null }
                } else null
            } else null

            val subtitleFile: File? = if (subtitleTracks.isNotEmpty()) {
                val track = subtitleTracks.firstOrNull { it.default } ?: subtitleTracks.first()
                if (track.uri != null) {
                    val dir = File(tempDir, "subs").also { it.mkdirs() }
                    try { downloadExternalTrack(track.uri, dir, config, playlistUrl, "subtitle") }
                    catch (e: Exception) { Log.w(TAG, "Subtitle track failed: ${e.message}"); null }
                } else null
            } else null

            val finalTotalSize = segmentFiles.filterNotNull().sumOf { it.length() }
            val mergeSummary = makeSummary(total, total, 0, 0, totalBytesDownloaded.get())
            onProgress(total, total, 0.92f, "MERGING:$finalTotalSize", buildSummaryChunks(mergeSummary, finalTotalSize))

            val isFmp4 = playlist.initSegmentUrl != null
            val tempMergedFile = File(tempDir, if (isFmp4) "merged.mp4" else "merged.ts")

            val mergeOk = if (isFmp4) {
                mergeFmp4Segments(File(tempDir, "init.mp4"), segmentFiles, tempMergedFile, total)
            } else {
                mergeSegmentsToTs(segmentFiles, tempMergedFile, total)
            }

            if (!mergeOk || !tempMergedFile.exists() || tempMergedFile.length() < 1L) {
                Log.e(TAG, "Merge failed: exists=${tempMergedFile.exists()}, size=${tempMergedFile.length()}")
                onProgress(total, total, 0f, "FAILED: Merge failed (${tempMergedFile.length()} bytes)", buildSummaryChunks(mergeSummary, finalTotalSize))
                return@withContext
            }

            onProgress(total, total, 0.95f, "CONVERTING:$finalTotalSize", buildSummaryChunks(mergeSummary, finalTotalSize))

            val mp4Out = if (outputFile.extension.equals("mp4", ignoreCase = true)) outputFile
            else File(outputFile.parentFile, outputFile.nameWithoutExtension + ".mp4")

            var ok = runCatching { convertToMp4(tempMergedFile, mp4Out, audioFile, subtitleFile, isFmp4) }.getOrDefault(false)

            if (!ok || !mp4Out.exists() || mp4Out.length() < 1024) {
                val retryFile = File(outputFile.parentFile, outputFile.nameWithoutExtension + "_retry.mp4")
                ok = runCatching { convertToMp4Alt(tempMergedFile, retryFile) }.getOrDefault(false)
                if (ok && retryFile.exists() && retryFile.length() > 1024) {
                    retryFile.renameTo(mp4Out)
                } else {
                    retryFile.delete()
                    val ext = if (isFmp4) ".mp4" else ".ts"
                    val fallback = File(outputFile.parentFile, outputFile.nameWithoutExtension + ext)
                    tempMergedFile.copyTo(fallback, overwrite = true)
                    tempDir.deleteRecursively()
                    val sz = fallback.length()
                    onProgress(total, total, 1f, "COMPLETED:$sz", buildSummaryChunks(makeSummary(total, total, 0, 0, sz), sz))
                    return@withContext
                }
            }

            tempDir.deleteRecursively()
            val finalSize = mp4Out.length()
            onProgress(total, total, 1f, "COMPLETED:$finalSize", buildSummaryChunks(makeSummary(total, total, 0, 0, finalSize), finalSize))
        }
    }

    private suspend fun downloadLive(
        initialPlaylist: HlsPlaylist,
        playlistUrl: String,
        tempDir: File,
        outputFile: File,
        config: DownloadConfig,
        onProgress: (Int, Int, Float, String, List<ChunkInfo>) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val mp4Out = if (outputFile.extension.equals("mp4", ignoreCase = true)) outputFile
            else File(outputFile.parentFile, outputFile.nameWithoutExtension + ".mp4")

            val mergedTs = File(tempDir, "live_merged.ts")
            val mergeStream = FileOutputStream(mergedTs, true).buffered(8 * 1024 * 1024)

            val downloadedUrls = mutableSetOf<String>()
            var segmentIndex = 0
            var totalBytes = 0L
            var stallCount = 0

            if (initialPlaylist.initSegmentUrl != null) {
                val initFile = File(tempDir, "init.mp4")
                downloadInitSegment(initialPlaylist.initSegmentUrl, initialPlaylist.initSegmentRange, initFile, config, playlistUrl)
                if (initFile.exists()) mergeStream.write(initFile.readBytes())
            }

            try {
                var currentPlaylist = initialPlaylist
                while (!isCancelled) {
                    val newSegs = currentPlaylist.segments.filter { it.url !in downloadedUrls }

                    if (newSegs.isEmpty()) {
                        stallCount++
                        if (stallCount >= LIVE_MAX_STALL_COUNT && !currentPlaylist.isEvent) break
                        delay(LIVE_POLL_INTERVAL_MS)
                    } else {
                        stallCount = 0
                        for (seg in newSegs) {
                            if (isCancelled) break
                            val result = downloadSegmentWithRetry(
                                segment = seg, index = segmentIndex, tempDir = tempDir,
                                config = config, referer = playlistUrl,
                                encKey = currentPlaylist.encryptionKey,
                                encMethod = currentPlaylist.encryptionMethod,
                                encIv = seg.iv ?: currentPlaylist.defaultIv
                            )
                            if (result != null && result.exists()) {
                                mergeStream.write(result.readBytes())
                                totalBytes += result.length()
                                result.delete()
                            }
                            downloadedUrls.add(seg.url)
                            segmentIndex++
                            val summary = makeSummary(segmentIndex, segmentIndex, 0, 0, totalBytes)
                            onProgress(segmentIndex, segmentIndex, -1f, "LIVE:$totalBytes", buildSummaryChunks(summary, totalBytes))
                        }
                    }

                    if (!isCancelled) {
                        delay(LIVE_POLL_INTERVAL_MS)
                        try {
                            currentPlaylist = parsePlaylist(playlistUrl, config)
                            if (!currentPlaylist.isLive && !currentPlaylist.isEvent) {
                                for (seg in currentPlaylist.segments.filter { it.url !in downloadedUrls }) {
                                    if (isCancelled) break
                                    val result = downloadSegmentWithRetry(
                                        segment = seg, index = segmentIndex, tempDir = tempDir,
                                        config = config, referer = playlistUrl,
                                        encKey = currentPlaylist.encryptionKey,
                                        encMethod = currentPlaylist.encryptionMethod,
                                        encIv = seg.iv ?: currentPlaylist.defaultIv
                                    )
                                    if (result != null && result.exists()) {
                                        mergeStream.write(result.readBytes())
                                        totalBytes += result.length()
                                        result.delete()
                                    }
                                    downloadedUrls.add(seg.url)
                                    segmentIndex++
                                }
                                break
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to refresh live playlist: ${e.message}")
                        }
                    }
                }
            } finally {
                mergeStream.flush()
                mergeStream.close()
            }

            if (isCancelled) {
                val summary = makeSummary(segmentIndex, segmentIndex, 0, 0, totalBytes)
                onProgress(segmentIndex, segmentIndex, 0f, "CANCELLED", buildSummaryChunks(summary, totalBytes))
                return@withContext
            }

            onProgress(segmentIndex, segmentIndex, 0.95f, "CONVERTING:$totalBytes", emptyList())

            var ok = runCatching { convertToMp4(mergedTs, mp4Out, null, null, false) }.getOrDefault(false)
            if (!ok || !mp4Out.exists() || mp4Out.length() < 1024) {
                ok = runCatching { convertToMp4Alt(mergedTs, mp4Out) }.getOrDefault(false)
            }

            tempDir.deleteRecursively()
            val finalSize = if (ok && mp4Out.exists()) mp4Out.length() else totalBytes
            onProgress(segmentIndex, segmentIndex, 1f, "COMPLETED:$finalSize", emptyList())
        }
    }

    private suspend fun downloadExternalTrack(
        playlistUrl: String,
        tempDir: File,
        config: DownloadConfig,
        referer: String,
        trackType: String
    ): File? {
        return withContext(Dispatchers.IO) {
            try {
                val playlist = parsePlaylist(playlistUrl, config)
                if (playlist.segments.isEmpty()) return@withContext null

                val mergedFile = File(tempDir, "$trackType.ts")
                val outStream = FileOutputStream(mergedFile).buffered(BUFFER_SIZE)

                if (playlist.initSegmentUrl != null) {
                    val initFile = File(tempDir, "${trackType}_init.mp4")
                    downloadInitSegment(playlist.initSegmentUrl, playlist.initSegmentRange, initFile, config, referer)
                    if (initFile.exists()) outStream.write(initFile.readBytes())
                }

                try {
                    for ((i, seg) in playlist.segments.withIndex()) {
                        val result = downloadSegmentWithRetry(
                            segment = seg, index = i, tempDir = tempDir,
                            config = config, referer = referer,
                            encKey = playlist.encryptionKey,
                            encMethod = playlist.encryptionMethod,
                            encIv = seg.iv ?: playlist.defaultIv
                        )
                        if (result != null && result.exists()) {
                            outStream.write(result.readBytes())
                            result.delete()
                        }
                    }
                } finally {
                    outStream.flush()
                    outStream.close()
                }

                if (mergedFile.exists() && mergedFile.length() > 0) mergedFile else null
            } catch (e: Exception) {
                Log.e(TAG, "External track download failed: ${e.message}")
                null
            }
        }
    }

    private suspend fun downloadInitSegment(
        url: String,
        range: Pair<Long, Long>?,
        outFile: File,
        config: DownloadConfig,
        referer: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val conn = openConnection(url, config, referer)
                if (range != null) conn.setRequestProperty("Range", "bytes=${range.first}-${range.second}")
                conn.connect()
                val bytes = conn.inputStream.use { it.readBytes() }
                conn.disconnect()
                outFile.writeBytes(bytes)
                outFile.exists() && outFile.length() > 0
            } catch (e: Exception) {
                Log.e(TAG, "Init segment download failed: ${e.message}")
                false
            }
        }
    }

    private fun convertToMp4(
        input: File,
        output: File,
        audioFile: File?,
        subtitleFile: File?,
        isFmp4: Boolean
    ): Boolean {
        if (output.exists()) output.delete()
        val cmd = buildString {
            append("-i \"${input.absolutePath}\"")
            if (audioFile?.exists() == true) append(" -i \"${audioFile.absolutePath}\"")
            if (subtitleFile?.exists() == true) append(" -i \"${subtitleFile.absolutePath}\"")
            append(" -c copy")
            if (!isFmp4) append(" -bsf:a aac_adtstoasc")
            if (audioFile?.exists() == true) append(" -map 0:v -map 1:a")
            if (subtitleFile?.exists() == true) {
                val subIdx = if (audioFile?.exists() == true) 2 else 1
                append(" -map $subIdx:s")
            }
            append(" -movflags +faststart -y \"${output.absolutePath}\"")
        }
        val session = FFmpegKit.execute(cmd)
        if (!ReturnCode.isSuccess(session.returnCode)) {
            Log.e(TAG, "FFmpeg failed: ${session.failStackTrace}")
            return false
        }
        return output.exists() && output.length() > 1024
    }

    private fun convertToMp4Alt(input: File, output: File): Boolean {
        if (output.exists()) output.delete()
        val cmd = "-i \"${input.absolutePath}\" -c:v libx264 -c:a aac -b:a 128k -movflags +faststart -y \"${output.absolutePath}\""
        val session = FFmpegKit.execute(cmd)
        if (!ReturnCode.isSuccess(session.returnCode)) {
            Log.e(TAG, "FFmpeg alt failed: ${session.failStackTrace}")
            return false
        }
        return output.exists() && output.length() > 1024
    }

    private fun makeSummary(done: Int, total: Int, failed: Int, downloading: Int, bytes: Long) =
        HlsSegmentSummary(total, done, failed, downloading, bytes)

    private suspend fun getSegmentSize(url: String, config: DownloadConfig): Long {
        return withContext(Dispatchers.IO) {
            try {
                val conn = URL(url.trim()).openConnection() as HttpURLConnection
                conn.requestMethod = "HEAD"
                conn.connectTimeout = CONNECT_TIMEOUT
                conn.readTimeout = CONNECT_TIMEOUT
                conn.setRequestProperty("User-Agent", config.userAgent.ifBlank { USER_AGENT })
                if (!config.cookies.isNullOrBlank()) conn.setRequestProperty("Cookie", config.cookies)
                config.headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
                conn.connect()
                val size = conn.contentLengthLong
                conn.disconnect()
                size
            } catch (e: Exception) { -1L }
        }
    }

    private fun resolveOutputPath(outputPath: String): String {
        val lower = outputPath.lowercase()
        return when {
            lower.endsWith(".mp4")    -> outputPath
            lower.endsWith(".mkv")    -> outputPath.dropLast(4) + ".mp4"
            lower.endsWith(".ts")     -> outputPath.dropLast(3) + ".mp4"
            lower.endsWith(".m3u8")   -> outputPath.dropLast(5) + ".mp4"
            lower.endsWith(".m3u")    -> outputPath.dropLast(4) + ".mp4"
            !outputPath.contains('.') -> "$outputPath.mp4"
            else -> outputPath.substring(0, outputPath.lastIndexOf('.')) + ".mp4"
        }
    }

    fun resolveHlsFilename(filename: String): String = resolveOutputPath(filename)

    private suspend fun decryptAes128(input: File, output: File, key: ByteArray, iv: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key.copyOf(16), "AES"), IvParameterSpec(iv.copyOf(16)))
                output.writeBytes(cipher.doFinal(input.readBytes()))
                output.exists() && output.length() > 0
            } catch (e: Exception) {
                Log.e(TAG, "AES-128 decrypt failed: ${e.message}")
                false
            }
        }
    }

    private suspend fun decryptSampleAes(input: File, output: File, key: ByteArray, iv: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val tmp = File(input.parent, input.name + ".sadec.ts")
                val keyHex = key.joinToString("") { "%02x".format(it) }
                val cmd = "-decryption_key $keyHex -i \"${input.absolutePath}\" -c copy -y \"${tmp.absolutePath}\""
                val session = FFmpegKit.execute(cmd)
                if (ReturnCode.isSuccess(session.returnCode) && tmp.exists() && tmp.length() > 0) {
                    tmp.renameTo(output)
                    output.exists() && output.length() > 0
                } else {
                    tmp.delete()
                    input.copyTo(output, overwrite = true)
                    output.exists()
                }
            } catch (e: Exception) {
                Log.e(TAG, "SAMPLE-AES decrypt failed: ${e.message}")
                false
            }
        }
    }

    private fun mergeFmp4Segments(initFile: File, files: Array<File?>, output: File, total: Int): Boolean {
        return try {
            if (output.exists()) output.delete()
            FileOutputStream(output).buffered(8 * 1024 * 1024).use { out ->
                if (initFile.exists()) out.write(initFile.readBytes())
                for (i in 0 until total) {
                    val f = files[i]
                    if (f != null && f.exists() && f.length() > 0) out.write(f.readBytes())
                    else Log.w(TAG, "fMP4 segment $i missing")
                }
            }
            output.exists() && output.length() > 0
        } catch (e: Exception) {
            Log.e(TAG, "fMP4 merge error: ${e.message}")
            output.delete()
            false
        }
    }

    private fun mergeSegmentsToTs(files: Array<File?>, output: File, total: Int): Boolean {
        return try {
            if (output.exists()) output.delete()
            var written = 0L
            var missing = 0
            FileOutputStream(output).buffered(8 * 1024 * 1024).use { out ->
                for (i in 0 until total) {
                    val f = files[i]
                    if (f != null && f.exists() && f.length() > 0) {
                        f.inputStream().use { inp ->
                            val buf = ByteArray(BUFFER_SIZE)
                            var n = inp.read(buf)
                            while (n != -1) { out.write(buf, 0, n); written += n; n = inp.read(buf) }
                        }
                    } else {
                        missing++
                        Log.w(TAG, "Segment $i missing, skipping")
                    }
                }
            }
            if (missing > total * 0.05) {
                Log.e(TAG, "Too many missing segments: $missing/$total")
                output.delete()
                return false
            }
            output.exists() && written > 0
        } catch (e: Exception) {
            Log.e(TAG, "Merge error: ${e.message}")
            output.delete()
            false
        }
    }

    private suspend fun downloadSegmentWithRetry(
        segment: HlsSegment,
        index: Int,
        tempDir: File,
        config: DownloadConfig,
        referer: String,
        encKey: ByteArray?,
        encMethod: String?,
        encIv: ByteArray?
    ): File? {
        val outFile = File(tempDir, "seg_%010d.ts".format(index))
        val cleanUrl = segment.url.trim()

        for (attempt in 0 until MAX_RETRY) {
            if (isCancelled) return null
            currentCoroutineContext().ensureActive()

            val result = tryDownloadSegment(segment, cleanUrl, index, outFile, tempDir, config, referer, encKey, encMethod, encIv, attempt)
            when (result) {
                SegmentResult.SUCCESS       -> return outFile
                SegmentResult.CANCELLED     -> return null
                SegmentResult.PERMANENT_FAIL -> return null
                SegmentResult.RETRY         -> delay(2000L * (attempt + 1))
            }
        }
        return null
    }

    private enum class SegmentResult { SUCCESS, RETRY, CANCELLED, PERMANENT_FAIL }

    private suspend fun tryDownloadSegment(
        segment: HlsSegment,
        url: String,
        index: Int,
        outFile: File,
        tempDir: File,
        config: DownloadConfig,
        referer: String,
        encKey: ByteArray?,
        encMethod: String?,
        encIv: ByteArray?,
        attempt: Int
    ): SegmentResult {
        return withContext(Dispatchers.IO) {
            try {
                val tmpFile = File(tempDir, "seg_%010d.tmp".format(index))
                val partFile = File(tempDir, "seg_%010d.part".format(index))
                val alreadyDl = if (partFile.exists()) partFile.length() else 0L

                val conn = openConnection(url, config, referer)

                when {
                    segment.byteRangeLength > 0 -> {
                        val start = segment.byteRangeOffset + alreadyDl
                        val end = segment.byteRangeOffset + segment.byteRangeLength - 1
                        conn.setRequestProperty("Range", "bytes=$start-$end")
                    }
                    alreadyDl > 0 && attempt > 0 -> {
                        conn.setRequestProperty("Range", "bytes=$alreadyDl-")
                    }
                }

                conn.connect()
                val code = conn.responseCode

                if (code !in 200..299) {
                    conn.disconnect()
                    return@withContext if (code == 404 || code == 410) SegmentResult.PERMANENT_FAIL else SegmentResult.RETRY
                }

                val isResume = code == 206 && alreadyDl > 0
                val dlTarget = if (isResume) partFile else { partFile.delete(); tmpFile }

                if (!streamToFile(conn, dlTarget, isResume)) {
                    return@withContext SegmentResult.CANCELLED
                }

                val finalTmp = if (isResume && partFile.exists()) partFile else tmpFile

                if (finalTmp.length() < TS_PACKET_SIZE && segment.byteRangeLength < 0) {
                    finalTmp.renameTo(partFile)
                    return@withContext SegmentResult.RETRY
                }

                if (encKey != null) {
                    val iv = encIv ?: buildDefaultIv(index)
                    val ok = when (encMethod?.uppercase()) {
                        "SAMPLE-AES" -> decryptSampleAes(finalTmp, outFile, encKey, iv)
                        else         -> decryptAes128(finalTmp, outFile, encKey, iv)
                    }
                    finalTmp.delete()
                    partFile.delete()
                    return@withContext if (ok && outFile.exists() && outFile.length() > 0) SegmentResult.SUCCESS else SegmentResult.RETRY
                } else {
                    if (!finalTmp.renameTo(outFile)) {
                        finalTmp.copyTo(outFile, overwrite = true)
                        finalTmp.delete()
                    }
                    partFile.delete()
                    return@withContext SegmentResult.SUCCESS
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Seg[$index] attempt ${attempt + 1} error: ${e.message}")
                SegmentResult.RETRY
            }
        }
    }

    private fun streamToFile(conn: HttpURLConnection, dest: File, append: Boolean): Boolean {
        return try {
            conn.inputStream.use { inp ->
                FileOutputStream(dest, append).buffered(BUFFER_SIZE).use { out ->
                    val buf = ByteArray(BUFFER_SIZE)
                    var n = inp.read(buf)
                    while (n != -1) {
                        if (isCancelled) return false
                        out.write(buf, 0, n)
                        n = inp.read(buf)
                    }
                    out.flush()
                }
            }
            conn.disconnect()
            true
        } catch (e: Exception) {
            runCatching { conn.disconnect() }
            false
        }
    }

    private fun openConnection(url: String, config: DownloadConfig, referer: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.setRequestProperty("User-Agent", config.userAgent.ifBlank { USER_AGENT })
        conn.setRequestProperty("Accept", "*/*")
        conn.setRequestProperty("Accept-Encoding", "identity")
        conn.setRequestProperty("Connection", "keep-alive")
        if (referer.startsWith("http")) conn.setRequestProperty("Referer", referer)
        if (!config.cookies.isNullOrBlank()) conn.setRequestProperty("Cookie", config.cookies)
        config.headers.forEach { (k, v) ->
            if (k.lowercase() !in setOf("user-agent", "cookie", "accept-encoding"))
                conn.setRequestProperty(k, v)
        }
        conn.instanceFollowRedirects = true
        return conn
    }

    private suspend fun parsePlaylist(url: String, config: DownloadConfig): HlsPlaylist =
        withContext(Dispatchers.IO) { parsePlaylistFromContent(fetchText(url, config), url, config) }

    private fun parsePlaylistFromContent(content: String, url: String, config: DownloadConfig): HlsPlaylist {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.none { it.startsWith("#EXTM3U") } && lines.none { it.startsWith("#EXT") })
            throw IOException("Not a valid M3U8 playlist")

        val baseUrl = url.substringBeforeLast('/')
        var encKey: ByteArray? = null
        var encMethod: String? = null
        var defaultIv: ByteArray? = null
        var segIndex = 0
        val segments = mutableListOf<HlsSegment>()
        var duration = 0.0
        var totalDuration = 0.0
        var discontinuity = false
        var isLive = true
        var isEvent = false
        var mediaSequence = 0
        var targetDuration = 0
        var initSegmentUrl: String? = null
        var initSegmentRange: Pair<Long, Long>? = null
        var pendingByteRange: Pair<Long, Long>? = null
        var lastByteRangeEnd = 0L
        val audioTracks = mutableListOf<HlsMediaTrack>()
        val subtitleTracks = mutableListOf<HlsMediaTrack>()

        for (line in lines) {
            when {
                line.startsWith("#EXT-X-ENDLIST") -> isLive = false
                line.startsWith("#EXT-X-PLAYLIST-TYPE:EVENT") -> { isEvent = true; isLive = true }
                line.startsWith("#EXT-X-PLAYLIST-TYPE:VOD")   -> { isLive = false; isEvent = false }
                line.startsWith("#EXT-X-MEDIA-SEQUENCE:") ->
                    mediaSequence = line.substringAfter(':').trim().toIntOrNull() ?: 0
                line.startsWith("#EXT-X-TARGETDURATION:") ->
                    targetDuration = line.substringAfter(':').trim().toIntOrNull() ?: 0
                line.startsWith("#EXT-X-MAP:") -> {
                    val mapUri = extractAttr(line, "URI")?.trim('"')
                    if (!mapUri.isNullOrBlank()) {
                        initSegmentUrl = resolveUrl(mapUri, baseUrl)
                        val mapRange = extractAttr(line, "BYTERANGE")
                        if (mapRange != null) {
                            val parts = mapRange.split("@")
                            val len = parts[0].toLongOrNull() ?: 0L
                            val off = if (parts.size > 1) parts[1].toLongOrNull() ?: 0L else 0L
                            initSegmentRange = Pair(off, off + len - 1)
                        }
                    }
                }
                line.startsWith("#EXT-X-KEY:") -> {
                    val method = extractAttr(line, "METHOD") ?: ""
                    encMethod = method.uppercase()
                    if (method.equals("NONE", ignoreCase = true)) {
                        encKey = null; defaultIv = null; encMethod = null
                    } else if (method.equals("AES-128", ignoreCase = true) || method.equals("SAMPLE-AES", ignoreCase = true)) {
                        val keyUrl = extractAttr(line, "URI")?.trim('"')
                        val ivHex = extractAttr(line, "IV")
                        defaultIv = ivHex?.removePrefix("0x")?.removePrefix("0X")?.let { hexToBytes(it) }
                        if (!keyUrl.isNullOrBlank()) encKey = fetchBytes(resolveUrl(keyUrl, baseUrl), config)
                    }
                }
                line.startsWith("#EXT-X-BYTERANGE:") -> {
                    val parts = line.substringAfter(':').trim().split("@")
                    val len = parts[0].toLongOrNull() ?: 0L
                    val off = if (parts.size > 1) { val o = parts[1].toLongOrNull() ?: lastByteRangeEnd; lastByteRangeEnd = o; o } else lastByteRangeEnd
                    pendingByteRange = Pair(off, len)
                    lastByteRangeEnd = off + len
                }
                line.startsWith("#EXTINF:") -> {
                    duration = line.substringAfter(':').substringBefore(',').toDoubleOrNull() ?: 0.0
                    totalDuration += duration
                }
                line.startsWith("#EXT-X-DISCONTINUITY") -> discontinuity = true
                line.startsWith("#EXT-X-MEDIA:") -> {
                    val type      = extractAttr(line, "TYPE") ?: ""
                    val groupId   = extractAttr(line, "GROUP-ID")?.trim('"') ?: ""
                    val language  = extractAttr(line, "LANGUAGE")?.trim('"')
                    val name      = extractAttr(line, "NAME")?.trim('"') ?: ""
                    val default   = extractAttr(line, "DEFAULT")?.equals("YES", ignoreCase = true) ?: false
                    val uri       = extractAttr(line, "URI")?.trim('"')?.let { resolveUrl(it, baseUrl) }
                    val autoSel   = extractAttr(line, "AUTOSELECT")?.equals("YES", ignoreCase = true) ?: false
                    val track = HlsMediaTrack(type, groupId, language, name, default, uri, autoSel)
                    when (type.uppercase()) {
                        "AUDIO"     -> audioTracks.add(track)
                        "SUBTITLES" -> subtitleTracks.add(track)
                    }
                }
                !line.startsWith('#') && line.isNotBlank() -> {
                    val br = pendingByteRange
                    segments.add(HlsSegment(
                        index          = segIndex++,
                        url            = resolveUrl(line, baseUrl),
                        duration       = duration,
                        iv             = defaultIv ?: buildDefaultIv(segIndex - 1),
                        discontinuity  = discontinuity,
                        byteRangeLength = br?.second ?: -1L,
                        byteRangeOffset = br?.first  ?: -1L
                    ))
                    duration = 0.0; discontinuity = false; pendingByteRange = null
                }
            }
        }

        return HlsPlaylist(
            segments         = segments,
            encryptionKey    = encKey,
            encryptionMethod = encMethod,
            defaultIv        = defaultIv,
            totalDuration    = totalDuration,
            isLive           = isLive,
            isEvent          = isEvent,
            mediaSequence    = mediaSequence,
            targetDuration   = targetDuration,
            initSegmentUrl   = initSegmentUrl,
            initSegmentRange = initSegmentRange,
            audioTracks      = audioTracks,
            subtitleTracks   = subtitleTracks
        )
    }

    private fun parseMasterVariants(lines: List<String>, masterUrl: String): List<HlsVariant> {
        val base = masterUrl.substringBeforeLast('/')
        val variants = mutableListOf<HlsVariant>()
        var bw = 0L; var res: String? = null; var codecs: String? = null
        var audioGrp: String? = null; var subGrp: String? = null
        var frameRate = 0.0; var videoRange = ""

        for (line in lines) {
            when {
                line.startsWith("#EXT-X-STREAM-INF:") -> {
                    bw       = Regex("BANDWIDTH=(\\d+)").find(line)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                    res      = Regex("RESOLUTION=([\\dx]+)").find(line)?.groupValues?.get(1)
                    codecs   = extractAttr(line, "CODECS")?.trim('"')
                    audioGrp = extractAttr(line, "AUDIO")?.trim('"')
                    subGrp   = extractAttr(line, "SUBTITLES")?.trim('"')
                    frameRate = Regex("FRAME-RATE=([\\d.]+)").find(line)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                    videoRange = extractAttr(line, "VIDEO-RANGE")?.trim('"') ?: ""
                }
                !line.startsWith('#') && line.isNotBlank() && bw > 0 -> {
                    variants.add(HlsVariant(bw, res, codecs, resolveUrl(line, base), audioGrp, subGrp, frameRate, videoRange))
                    bw = 0L; res = null; codecs = null; audioGrp = null; subGrp = null; frameRate = 0.0; videoRange = ""
                }
            }
        }
        return variants
    }

    private fun parseMasterMediaTracks(lines: List<String>, masterUrl: String, filterType: String): List<HlsMediaTrack> {
        val base = masterUrl.substringBeforeLast('/')
        return lines
            .filter { it.startsWith("#EXT-X-MEDIA:") }
            .mapNotNull { line ->
                val type = extractAttr(line, "TYPE") ?: return@mapNotNull null
                if (!type.equals(filterType, ignoreCase = true)) return@mapNotNull null
                HlsMediaTrack(
                    type      = type,
                    groupId   = extractAttr(line, "GROUP-ID")?.trim('"') ?: "",
                    language  = extractAttr(line, "LANGUAGE")?.trim('"'),
                    name      = extractAttr(line, "NAME")?.trim('"') ?: "",
                    default   = extractAttr(line, "DEFAULT")?.equals("YES", ignoreCase = true) ?: false,
                    uri       = extractAttr(line, "URI")?.trim('"')?.let { resolveUrl(it, base) },
                    autoSelect = extractAttr(line, "AUTOSELECT")?.equals("YES", ignoreCase = true) ?: false
                )
            }
    }

    private fun resolveUrl(seg: String, base: String): String = when {
        seg.startsWith("http://") || seg.startsWith("https://") -> seg
        seg.startsWith("//") -> "https:$seg"
        seg.startsWith("/")  -> {
            val proto = base.substringBefore("://")
            val host  = base.substringAfter("://").substringBefore('/')
            "$proto://$host$seg"
        }
        else -> "${base.trimEnd('/')}/$seg"
    }

    private fun extractAttr(line: String, attr: String): String? =
        Regex("$attr=([^,\\s\"]+|\"[^\"]*\")").find(line)?.groupValues?.get(1)?.trim('"')

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.padStart(32, '0').take(32)
        return clean.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private fun fetchText(url: String, config: DownloadConfig): String {
        var lastEx: Exception? = null
        repeat(3) { attempt ->
            try {
                val conn = URL(url.trim()).openConnection() as HttpURLConnection
                conn.connectTimeout = CONNECT_TIMEOUT
                conn.readTimeout = CONNECT_TIMEOUT
                conn.setRequestProperty("User-Agent", config.userAgent.ifBlank { USER_AGENT })
                conn.setRequestProperty("Accept", "application/vnd.apple.mpegurl,application/x-mpegurl,*/*")
                conn.instanceFollowRedirects = true
                if (!config.cookies.isNullOrBlank()) conn.setRequestProperty("Cookie", config.cookies)
                config.headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
                conn.connect()
                if (conn.responseCode in 200..299) {
                    val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    conn.disconnect()
                    return text
                }
                conn.disconnect()
            } catch (e: Exception) {
                lastEx = e
                if (attempt < 2) Thread.sleep(2000L * (attempt + 1))
            }
        }
        throw IOException("Failed to fetch: $url", lastEx)
    }

    private fun fetchBytes(url: String, config: DownloadConfig): ByteArray? {
        return try {
            val conn = URL(url.trim()).openConnection() as HttpURLConnection
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.setRequestProperty("User-Agent", config.userAgent.ifBlank { USER_AGENT })
            conn.instanceFollowRedirects = true
            if (!config.cookies.isNullOrBlank()) conn.setRequestProperty("Cookie", config.cookies)
            conn.connect()
            val bytes = conn.inputStream.use { it.readBytes() }
            conn.disconnect()
            bytes
        } catch (e: Exception) {
            Log.e(TAG, "fetchBytes failed: ${e.message}")
            null
        }
    }

    private fun buildDefaultIv(index: Int): ByteArray {
        val iv = ByteArray(16)
        iv[12] = ((index shr 24) and 0xFF).toByte()
        iv[13] = ((index shr 16) and 0xFF).toByte()
        iv[14] = ((index shr 8)  and 0xFF).toByte()
        iv[15] = (index and 0xFF).toByte()
        return iv
    }

    fun isHlsUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".m3u8") || lower.contains(".m3u") ||
                lower.contains("hls")  || lower.contains("manifest")
    }

    fun getVariants(m3u8Url: String, config: DownloadConfig): List<HlsVariant> {
        return try {
            val content = fetchText(m3u8Url, config)
            val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.any { it.startsWith("#EXT-X-STREAM-INF") }) parseMasterVariants(lines, m3u8Url)
            else emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "getVariants failed: ${e.message}")
            emptyList()
        }
    }

    fun cancel() {
        isCancelled = true
        currentJob?.cancel()
    }
}