package com.had.downloader.service

import android.util.Log
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VideoDetection"

data class VideoStream(
    val url: String,
    val variantUrl: String = "",
    val quality: VideoQuality,
    val format: VideoFormat,
    val sizeMb: Float = -1f,
    val bandwidth: Long = 0L,
    val codecs: String = "",
    val resolution: String = "",
    val audioOnly: Boolean = false,
    val linkType: LinkType = LinkType.VIDEO,
    val durationSeconds: Int = 0,
    val hasVideo: Boolean = true,
    val hasAudio: Boolean = true,
    val bitrate: Long = 0L,
    val fps: Float = 0f
)

enum class VideoFormat {
    MP4, MKV, WEBM, TS, HLS_M3U8, DASH_MPD, AVI, MOV, FLV,
    AUDIO_MP3, AUDIO_FLAC, AUDIO_AAC, AUDIO_OGG, AUDIO_WAV, AUDIO_OPUS, AUDIO_M4A,
    UNKNOWN
}

enum class VideoQuality(val label: String, val maxHeight: Int) {
    UHD_4K("4K", 2160),
    QHD_2K("2K", 1440),
    FHD_1080("1080p", 1080),
    HD_720("720p", 720),
    SD_480("480p", 480),
    SD_360("360p", 360),
    LOW_240("240p", 240),
    AUDIO_ONLY("Audio Only", 0),
    UNKNOWN("Unknown", -1)
}

data class VideoDetectionState(
    val pageUrl: String = "",
    val streams: List<VideoStream> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val detected: Boolean = false
)

@Singleton
class VideoDetectionEngine @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val scraperEngine = ScraperEngine()

    private val videoExtensions = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "flv", "wmv", "m4v", "3gp",
        "m3u8", "mpd", "ts", "m2ts", "mts"
    )

    private val audioExtensions = setOf(
        "mp3", "flac", "aac", "ogg", "opus", "m4a", "wav", "wma"
    )

    private val mediaExtensions = videoExtensions + audioExtensions

    suspend fun detectFromWebView(
        pageUrl: String,
        html: String,
        config: DownloadConfig? = null
    ): List<VideoStream> = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔍 Detecting: $html")
        val streams = mutableListOf<VideoStream>()

        val cleanUrl = html.substringBefore('?').substringBefore('#')
        val ext = cleanUrl.substringAfterLast('.').lowercase()

        if (ext !in mediaExtensions && !cleanUrl.contains(".m3u8") && !cleanUrl.contains(".mpd")) {
            Log.d(TAG, "⛔ Not media: $html")
            return@withContext emptyList()
        }

        Log.d(TAG, "✅ Media found: $html")

        val stream = when {
            cleanUrl.contains(".m3u8") -> {
                VideoStream(
                    url = html,
                    quality = VideoQuality.UNKNOWN,
                    format = VideoFormat.HLS_M3U8,
                    linkType = LinkType.HLS,
                    hasVideo = true,
                    hasAudio = true
                )
            }
            cleanUrl.contains(".mpd") -> {
                VideoStream(
                    url = html,
                    quality = VideoQuality.UNKNOWN,
                    format = VideoFormat.DASH_MPD,
                    linkType = LinkType.DASH,
                    hasVideo = true,
                    hasAudio = true
                )
            }
            ext in videoExtensions -> {
                val quality = detectQualityFromUrl(html)
                val format = extToFormat(ext)
                val sizeMb = runCatching {
                    val conn = openConn(html, config)
                    val len = conn.contentLengthLong
                    conn.disconnect()
                    if (len > 0) len / 1_048_576f else -1f
                }.getOrDefault(-1f)

                VideoStream(
                    url = html,
                    quality = quality,
                    format = format,
                    sizeMb = sizeMb,
                    linkType = LinkType.VIDEO,
                    hasVideo = true,
                    hasAudio = true
                )
            }
            ext in audioExtensions -> {
                VideoStream(
                    url = html,
                    quality = VideoQuality.AUDIO_ONLY,
                    format = extToFormat(ext),
                    linkType = LinkType.AUDIO,
                    audioOnly = true,
                    hasVideo = false,
                    hasAudio = true
                )
            }
            else -> null
        }

        stream?.let { streams.add(it) }
        streams
    }

    private fun detectQualityFromUrl(url: String): VideoQuality {
        val lower = url.lowercase()
        return when {
            "4k" in lower || "2160" in lower -> VideoQuality.UHD_4K
            "2k" in lower || "1440" in lower -> VideoQuality.QHD_2K
            "1080" in lower || "fhd" in lower -> VideoQuality.FHD_1080
            "720" in lower || "hd" in lower -> VideoQuality.HD_720
            "480" in lower || "sd" in lower -> VideoQuality.SD_480
            "360" in lower -> VideoQuality.SD_360
            "240" in lower -> VideoQuality.LOW_240
            else -> VideoQuality.UNKNOWN
        }
    }

    fun extToFormat(ext: String): VideoFormat = when (ext) {
        "mp4", "m4v" -> VideoFormat.MP4
        "mkv" -> VideoFormat.MKV
        "webm" -> VideoFormat.WEBM
        "ts", "m2ts", "mts" -> VideoFormat.TS
        "m3u8" -> VideoFormat.HLS_M3U8
        "mpd" -> VideoFormat.DASH_MPD
        "mp3" -> VideoFormat.AUDIO_MP3
        "flac" -> VideoFormat.AUDIO_FLAC
        "aac" -> VideoFormat.AUDIO_AAC
        "ogg", "opus", "spx" -> VideoFormat.AUDIO_OGG
        "wav", "aiff", "aif" -> VideoFormat.AUDIO_WAV
        "m4a" -> VideoFormat.AUDIO_M4A
        else -> VideoFormat.UNKNOWN
    }

    private fun openConn(url: String, config: DownloadConfig?): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = config?.timeoutMs ?: 15_000
        conn.readTimeout = config?.timeoutMs ?: 15_000
        conn.setRequestProperty("User-Agent", config?.userAgent ?: DEFAULT_USER_AGENT)
        conn.setRequestProperty("Accept", "*/*")
        conn.instanceFollowRedirects = true
        conn.connect()
        return conn
    }

    fun heightToQuality(height: Int): VideoQuality = when {
        height <= 0 -> VideoQuality.UNKNOWN
        height >= 2160 -> VideoQuality.UHD_4K
        height >= 1440 -> VideoQuality.QHD_2K
        height >= 1080 -> VideoQuality.FHD_1080
        height >= 720 -> VideoQuality.HD_720
        height >= 480 -> VideoQuality.SD_480
        height >= 360 -> VideoQuality.SD_360
        else -> VideoQuality.LOW_240
    }
}