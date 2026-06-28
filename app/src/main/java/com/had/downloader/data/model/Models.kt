package com.had.downloader.data.model

import androidx.room.*
import com.had.downloader.service.ChunkInfo
import com.had.downloader.service.LinkType
import com.had.downloader.ui.components.ThreadViewMode

enum class DownloadStatus {
    QUEUED, CONNECTING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED,
    VERIFYING,
    MERGING
}

enum class DownloadMode {
    HTTP, MULTI, HLS, QUEUE, PATTERN
}

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val filename: String,
    val outputDir: String,
    val mode: DownloadMode = DownloadMode.HTTP,
    val threads: Int = 4,
    val proxy: String? = null,
    val userAgent: String = "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
    val customHeaders: String = "",
    val cookies: String = "",
    val httpMethod: String = "GET",
    val useResume: Boolean = true,
    val verbose: Boolean = false,
    val maxRetries: Int = 5,
    val timeoutSec: Int = 30,
    val maxSpeedBps: Long = 0L,
    val mirrors: String = "",
    val checksumAlgo: String = "",
    val checksumExpected: String = "",
    val scheduleFrom: String = "",
    val scheduleTo: String = "",
    val hlsSegmentCount: Int = 0,
    val hlsSegmentsDone: Int = 0,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val speedBps: Long = 0,
    val etaSeconds: Int = -1,
    val activeThreads: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null,
    val queuePriority: Int = 0
)

data class LiveChunkState(
    val id: Long,
    val chunks: List<ChunkInfo> = emptyList(),
    val threadViewMode: ThreadViewMode = ThreadViewMode.SEGMENT_BAR
)

data class AppSettings(
    val defaultThreads: Int = 4,
    val defaultOutputDir: String = "",
    val maxConcurrent: Int = 2,
    val defaultProxy: String = "",
    val defaultMaxSpeedBps: Long = 0L,
    val defaultRetries: Int = 5,
    val defaultTimeoutSec: Int = 30,
    val defaultScheduleFrom: String = "",
    val defaultScheduleTo: String = "",
    val enableGzip: Boolean = false,
    val saveSession: Boolean = true,
    val showNotifications: Boolean = true,
    val defaultUserAgent: String = "",
    val defaultThreadViewMode: ThreadViewMode = ThreadViewMode.SEGMENT_BAR
)

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String
)

data class DownloadStats(
    val total: Int = 0,
    val completed: Int = 0,
    val failed: Int = 0,
    val active: Int = 0,
    val totalDownloaded: Long = 0
)

data class ScraperResult(
    val pageUrl: String,
    val links: List<ScrapedLinkUi> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

data class ScrapedLinkUi(
    val url: String,
    val text: String,
    val typeLabel: String,
    val linkType: LinkType = LinkType.OTHER,
    val selected: Boolean = false
)

data class VideoDetectionState(
    val pageUrl: String = "",
    val streams: List<com.had.downloader.service.VideoStream> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

fun Long.toHumanSize(): String = when {
    this < 1_024         -> "$this B"
    this < 1_048_576     -> "%.1f KB".format(this / 1_024f)
    this < 1_073_741_824 -> "%.1f MB".format(this / 1_048_576f)
    else                 -> "%.2f GB".format(this / 1_073_741_824f)
}

fun Long.toSpeedString(): String = "${toHumanSize()}/s"

fun Int.toEtaString(): String = when {
    this < 0    -> "--"
    this < 60   -> "${this}s"
    this < 3600 -> "${this / 60}m ${this % 60}s"
    else        -> "${this / 3600}h ${(this % 3600) / 60}m"
}

fun DownloadItem.displayName(): String =
    filename.ifBlank { url.substringAfterLast('/').ifBlank { url } }

fun DownloadItem.statusColor(
    cyan: androidx.compose.ui.graphics.Color,
    green: androidx.compose.ui.graphics.Color,
    red: androidx.compose.ui.graphics.Color,
    orange: androidx.compose.ui.graphics.Color,
    muted: androidx.compose.ui.graphics.Color
) = when (status) {
    DownloadStatus.DOWNLOADING -> cyan
    DownloadStatus.COMPLETED   -> green
    DownloadStatus.FAILED      -> red
    DownloadStatus.PAUSED      -> orange
    DownloadStatus.CANCELLED   -> red
    else                       -> muted
}

fun DownloadItem.modeLabel(): String = when (mode) {
    DownloadMode.HTTP    -> "HTTP"
    DownloadMode.MULTI   -> "MULTI"
    DownloadMode.HLS     -> "HLS"
    DownloadMode.QUEUE   -> "QUEUE"
    DownloadMode.PATTERN -> "PATTERN"
}