package com.had.downloader.service

import android.util.Log
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VideoDetectionEngine"

data class VideoStream(
    val url: String,
    val quality: VideoQuality,
    val format: VideoFormat,
    val sizeMb: Float = -1f,
    val bandwidth: Long = 0L,
    val codecs: String = "",
    val resolution: String = "",
    val audioOnly: Boolean = false,
    val linkType: LinkType = LinkType.VIDEO
)

enum class VideoFormat {
    MP4, MKV, WEBM, TS, HLS_M3U8, DASH_MPD, AVI, MOV, FLV,
    AUDIO_MP3, AUDIO_FLAC, AUDIO_AAC, AUDIO_OGG, AUDIO_WAV, AUDIO_OPUS, AUDIO_M4A, AUDIO_WMA, AUDIO_OTHER,
    ARCHIVE_ZIP, ARCHIVE_RAR, ARCHIVE_7Z, ARCHIVE_TAR, ARCHIVE_GZ, ARCHIVE_OTHER,
    DOC_PDF, DOC_WORD, DOC_TEXT, DOC_OTHER,
    SPREADSHEET_XLS, SPREADSHEET_CSV, SPREADSHEET_OTHER,
    PRESENTATION_PPT, PRESENTATION_OTHER,
    EBOOK_EPUB, EBOOK_MOBI, EBOOK_OTHER,
    IMAGE_JPG, IMAGE_PNG, IMAGE_GIF, IMAGE_WEBP, IMAGE_OTHER,
    SUBTITLE_SRT, SUBTITLE_VTT, SUBTITLE_OTHER,
    EXECUTABLE, DISK_IMAGE, TORRENT, FONT, CODE, DATA, UNKNOWN
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

data class VideoDetectionResult(
    val pageUrl: String,
    val streams: List<VideoStream>,
    val loading: Boolean = false,
    val error: String? = null
)

@Singleton
class VideoDetectionEngine @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val scraperEngine = ScraperEngine()

    suspend fun detect(
        pageUrlOrDirectUrl: String,
        config: DownloadConfig? = null
    ): List<VideoStream> = withContext(Dispatchers.IO) {
        val streams = mutableListOf<VideoStream>()

        when {
            isHls(pageUrlOrDirectUrl)  -> streams.addAll(parseHlsMaster(pageUrlOrDirectUrl, config))
            isDash(pageUrlOrDirectUrl) -> streams.addAll(parseDashMpd(pageUrlOrDirectUrl, config))
            isDirectDownloadable(pageUrlOrDirectUrl) -> streams.add(buildDirectStream(pageUrlOrDirectUrl, config))
            else -> streams.addAll(scanPage(pageUrlOrDirectUrl, config))
        }

        streams.distinctBy { it.url }.sortedWith(
            compareByDescending<VideoStream> { it.quality.maxHeight }
                .thenByDescending { it.bandwidth }
        )
    }

    private fun isHls(url: String) =
        url.contains(".m3u8", ignoreCase = true)

    private fun isDash(url: String) =
        url.contains(".mpd", ignoreCase = true)

    private fun isDirectDownloadable(url: String): Boolean {
        val ext = url.substringAfterLast('.').substringBefore('?').lowercase()
        return scraperEngine.extMap.any { (type, exts) ->
            type != LinkType.OTHER && type != LinkType.CODE && ext in exts
        }
    }

    private fun buildDirectStream(url: String, config: DownloadConfig?): VideoStream {
        val ext = url.substringAfterLast('.').substringBefore('?').lowercase()
        val linkType = scraperEngine.classifyUrl(url)
        val format = extToFormat(ext)
        val sizeMb = runCatching {
            val conn = openConn(url, config)
            val len = conn.contentLengthLong
            conn.disconnect()
            if (len > 0) len / 1_048_576f else -1f
        }.getOrDefault(-1f)
        val audioOnly = linkType == LinkType.AUDIO
        return VideoStream(
            url       = url,
            quality   = inferQualityFromUrl(url, audioOnly, linkType),
            format    = format,
            sizeMb    = sizeMb,
            audioOnly = audioOnly,
            linkType  = linkType
        )
    }

    suspend fun parseHlsMaster(url: String, config: DownloadConfig?): List<VideoStream> =
        withContext(Dispatchers.IO) {
            runCatching {
                val content = fetchText(url, config)
                val lines   = content.lines()
                val streams = mutableListOf<VideoStream>()
                val baseUrl = url.substringBeforeLast('/')

                if (!lines.any { it.contains("EXT-X-STREAM-INF") }) {
                    return@withContext listOf(
                        VideoStream(url = url, quality = VideoQuality.UNKNOWN, format = VideoFormat.HLS_M3U8, linkType = LinkType.HLS)
                    )
                }

                var bandwidth = 0L
                var resolution = ""
                var codecs = ""

                lines.forEach { line ->
                    when {
                        line.startsWith("#EXT-X-STREAM-INF:") -> {
                            bandwidth  = Regex("BANDWIDTH=(\\d+)").find(line)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                            resolution = Regex("RESOLUTION=(\\d+x\\d+)").find(line)?.groupValues?.get(1) ?: ""
                            codecs     = Regex("CODECS=\"([^\"]+)\"").find(line)?.groupValues?.get(1) ?: ""
                        }
                        line.startsWith("#EXT-X-MEDIA:") && line.contains("TYPE=AUDIO") -> {
                            val audioUri = Regex("URI=\"([^\"]+)\"").find(line)?.groupValues?.get(1)
                            if (audioUri != null) {
                                val audioUrl = if (audioUri.startsWith("http")) audioUri else "$baseUrl/$audioUri"
                                streams.add(VideoStream(
                                    url = audioUrl, quality = VideoQuality.AUDIO_ONLY,
                                    format = VideoFormat.HLS_M3U8, bandwidth = bandwidth,
                                    codecs = codecs, audioOnly = true, linkType = LinkType.HLS
                                ))
                            }
                        }
                        !line.startsWith('#') && line.isNotBlank() -> {
                            val streamUrl = if (line.startsWith("http")) line else "$baseUrl/$line"
                            val height = resolution.substringAfter('x').toIntOrNull() ?: 0
                            streams.add(VideoStream(
                                url = streamUrl, quality = heightToQuality(height),
                                format = VideoFormat.HLS_M3U8, bandwidth = bandwidth,
                                codecs = codecs, resolution = resolution, linkType = LinkType.HLS
                            ))
                            bandwidth = 0L; resolution = ""; codecs = ""
                        }
                    }
                }
                streams
            }.getOrDefault(emptyList())
        }

    suspend fun parseDashMpd(url: String, config: DownloadConfig?): List<VideoStream> =
        withContext(Dispatchers.IO) {
            runCatching {
                val content = fetchText(url, config)
                val streams = mutableListOf<VideoStream>()
                val baseUrl = url.substringBeforeLast('/')

                val reprRegex = Regex(
                    """<Representation[^>]*bandwidth="(\d+)"[^>]*(?:height="(\d+)")?[^>]*(?:codecs="([^"]*)")?[^>]*>.*?(?:<BaseURL>([^<]+)</BaseURL>)?""",
                    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
                )

                reprRegex.findAll(content).forEach { match ->
                    val bw     = match.groupValues[1].toLongOrNull() ?: 0L
                    val height = match.groupValues[2].toIntOrNull() ?: 0
                    val codec  = match.groupValues[3]
                    val segUrl = match.groupValues[4].trim().let {
                        if (it.startsWith("http")) it else "$baseUrl/$it"
                    }
                    if (segUrl.isNotBlank()) {
                        val audioOnly = codec.contains("mp4a", ignoreCase = true) || height == 0
                        streams.add(VideoStream(
                            url = segUrl, quality = heightToQuality(height),
                            format = VideoFormat.DASH_MPD, bandwidth = bw, codecs = codec,
                            audioOnly = audioOnly, linkType = LinkType.DASH
                        ))
                    }
                }
                if (streams.isEmpty()) {
                    streams.add(VideoStream(url = url, quality = VideoQuality.UNKNOWN, format = VideoFormat.DASH_MPD, linkType = LinkType.DASH))
                }
                streams
            }.getOrDefault(emptyList())
        }

    private suspend fun scanPage(pageUrl: String, config: DownloadConfig?): List<VideoStream> =
        withContext(Dispatchers.IO) {
            runCatching {
                val html = fetchText(pageUrl, config)
                val streams = mutableListOf<VideoStream>()
                val baseUrl = pageUrl.substringBeforeLast('/')

                val allExtPattern = scraperEngine.extMap
                    .filterKeys { it != LinkType.CODE && it != LinkType.OTHER }
                    .values.flatten()
                    .joinToString("|") { Regex.escape(it) }

                val patterns = listOf(
                    Regex("""(?:src|href|data-src|data-url|file|source)=["']([^"']+\.(?:$allExtPattern)[^"']*)["']""", RegexOption.IGNORE_CASE),
                    Regex(""""(https?://[^"]+\.(?:$allExtPattern)[^"]*)""""),
                    Regex("""'(https?://[^']+\.(?:$allExtPattern)[^']*)'"""),
                    Regex("""<source[^>]+src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE),
                    Regex("""<video[^>]+src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE),
                    Regex("""<audio[^>]+src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE),
                    Regex("""(?:videoUrl|streamUrl|playUrl|hlsUrl|manifestUrl|fileUrl|downloadUrl)\s*[:=]\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
                    Regex(""""url"\s*:\s*"(https?://[^"]+\.(?:mp4|m3u8|mpd|mkv|webm|mp3|flac|zip|rar|pdf|epub)[^"]*)"""""  )
                )

                patterns.forEach { pattern ->
                    pattern.findAll(html).forEach { match ->
                        val rawUrl = match.groupValues[1].trim()
                        val resolved = when {
                            rawUrl.startsWith("http") -> rawUrl
                            rawUrl.startsWith("//")   -> "https:$rawUrl"
                            rawUrl.startsWith("/")    -> {
                                val base = try { URL(pageUrl) } catch (e: Exception) { return@forEach }
                                "${base.protocol}://${base.host}$rawUrl"
                            }
                            else -> "$baseUrl/$rawUrl"
                        }
                        when {
                            isHls(resolved)  -> streams.addAll(parseHlsMaster(resolved, config))
                            isDash(resolved) -> streams.addAll(parseDashMpd(resolved, config))
                            isDirectDownloadable(resolved) -> streams.add(buildDirectStream(resolved, config))
                        }
                    }
                }

                val jsonSrcRegex = Regex(""""sources"\s*:\s*\[(.*?)\]""", setOf(RegexOption.DOT_MATCHES_ALL))
                jsonSrcRegex.findAll(html).forEach { match ->
                    val srcBlock = match.groupValues[1]
                    Regex(""""src"\s*:\s*"([^"]+)"""").findAll(srcBlock).forEach { m ->
                        val src = m.groupValues[1]
                        if (isDirectDownloadable(src) || isHls(src) || isDash(src)) {
                            val qualLabel = Regex(""""label"\s*:\s*"([^"]+)"""")
                                .find(srcBlock.substring(0, m.range.first + 50))?.groupValues?.get(1) ?: ""
                            streams.add(VideoStream(
                                url      = src,
                                quality  = labelToQuality(qualLabel),
                                format   = if (isHls(src)) VideoFormat.HLS_M3U8 else extToFormat(src.substringAfterLast('.').substringBefore('?').lowercase()),
                                linkType = scraperEngine.classifyUrl(src)
                            ))
                        }
                    }
                }

                streams.distinctBy { it.url }
            }.getOrDefault(emptyList())
        }

    fun heightToQuality(height: Int): VideoQuality = when {
        height <= 0    -> VideoQuality.UNKNOWN
        height >= 2160 -> VideoQuality.UHD_4K
        height >= 1440 -> VideoQuality.QHD_2K
        height >= 1080 -> VideoQuality.FHD_1080
        height >= 720  -> VideoQuality.HD_720
        height >= 480  -> VideoQuality.SD_480
        height >= 360  -> VideoQuality.SD_360
        else           -> VideoQuality.LOW_240
    }

    private fun inferQualityFromUrl(url: String, audioOnly: Boolean, linkType: LinkType): VideoQuality {
        if (audioOnly || linkType == LinkType.AUDIO) return VideoQuality.AUDIO_ONLY
        if (linkType != LinkType.VIDEO && linkType != LinkType.HLS && linkType != LinkType.DASH)
            return VideoQuality.UNKNOWN
        val lower = url.lowercase()
        return when {
            "4k" in lower || "2160" in lower  -> VideoQuality.UHD_4K
            "2k" in lower || "1440" in lower  -> VideoQuality.QHD_2K
            "1080" in lower || "fhd" in lower -> VideoQuality.FHD_1080
            "720" in lower  || "hd" in lower  -> VideoQuality.HD_720
            "480" in lower  || "sd" in lower  -> VideoQuality.SD_480
            "360" in lower                    -> VideoQuality.SD_360
            "240" in lower                    -> VideoQuality.LOW_240
            else -> VideoQuality.UNKNOWN
        }
    }

    private fun labelToQuality(label: String): VideoQuality {
        val lower = label.lowercase()
        return when {
            "4k" in lower || "2160" in lower -> VideoQuality.UHD_4K
            "2k" in lower || "1440" in lower -> VideoQuality.QHD_2K
            "1080" in lower                  -> VideoQuality.FHD_1080
            "720" in lower                   -> VideoQuality.HD_720
            "480" in lower                   -> VideoQuality.SD_480
            "360" in lower                   -> VideoQuality.SD_360
            "audio" in lower                 -> VideoQuality.AUDIO_ONLY
            else -> VideoQuality.UNKNOWN
        }
    }

    fun extToFormat(ext: String): VideoFormat = when (ext) {
        "mp4", "m4v"       -> VideoFormat.MP4
        "mkv"              -> VideoFormat.MKV
        "webm"             -> VideoFormat.WEBM
        "ts", "m2ts", "mts"-> VideoFormat.TS
        "avi"              -> VideoFormat.AVI
        "mov"              -> VideoFormat.MOV
        "flv"              -> VideoFormat.FLV
        "m3u8"             -> VideoFormat.HLS_M3U8
        "mpd"              -> VideoFormat.DASH_MPD
        "mp3"              -> VideoFormat.AUDIO_MP3
        "flac"             -> VideoFormat.AUDIO_FLAC
        "aac"              -> VideoFormat.AUDIO_AAC
        "ogg", "opus", "spx" -> VideoFormat.AUDIO_OGG
        "wav", "aiff", "aif" -> VideoFormat.AUDIO_WAV
        "m4a"              -> VideoFormat.AUDIO_M4A
        "wma"              -> VideoFormat.AUDIO_WMA
        "mid", "midi", "ac3", "dts", "ape", "wv", "tta" -> VideoFormat.AUDIO_OTHER
        "zip"              -> VideoFormat.ARCHIVE_ZIP
        "rar"              -> VideoFormat.ARCHIVE_RAR
        "7z"               -> VideoFormat.ARCHIVE_7Z
        "tar", "tgz", "tbz2", "txz" -> VideoFormat.ARCHIVE_TAR
        "gz", "bz2", "xz", "zst"    -> VideoFormat.ARCHIVE_GZ
        "cab", "arj", "lzh", "ace", "jar", "war", "apk", "ipa" -> VideoFormat.ARCHIVE_OTHER
        "pdf"              -> VideoFormat.DOC_PDF
        "doc", "docx", "odt", "rtf", "pages" -> VideoFormat.DOC_WORD
        "txt", "md", "rst" -> VideoFormat.DOC_TEXT
        "tex", "wpd", "wps" -> VideoFormat.DOC_OTHER
        "xls", "xlsx", "ods" -> VideoFormat.SPREADSHEET_XLS
        "csv", "tsv"       -> VideoFormat.SPREADSHEET_CSV
        "numbers", "gnumeric" -> VideoFormat.SPREADSHEET_OTHER
        "ppt", "pptx", "odp" -> VideoFormat.PRESENTATION_PPT
        "key"              -> VideoFormat.PRESENTATION_OTHER
        "epub"             -> VideoFormat.EBOOK_EPUB
        "mobi", "azw", "azw3" -> VideoFormat.EBOOK_MOBI
        "fb2", "djvu", "lit", "cbz", "cbr" -> VideoFormat.EBOOK_OTHER
        "jpg", "jpeg"      -> VideoFormat.IMAGE_JPG
        "png"              -> VideoFormat.IMAGE_PNG
        "gif"              -> VideoFormat.IMAGE_GIF
        "webp"             -> VideoFormat.IMAGE_WEBP
        "bmp", "tiff", "tif", "ico", "heic", "heif", "avif", "psd" -> VideoFormat.IMAGE_OTHER
        "srt"              -> VideoFormat.SUBTITLE_SRT
        "vtt"              -> VideoFormat.SUBTITLE_VTT
        "ass", "ssa", "sub", "sbv" -> VideoFormat.SUBTITLE_OTHER
        "exe", "msi", "dmg", "run", "sh", "appimage", "deb", "rpm" -> VideoFormat.EXECUTABLE
        "iso", "img", "nrg", "mdf", "bin" -> VideoFormat.DISK_IMAGE
        "torrent"          -> VideoFormat.TORRENT
        "ttf", "otf", "woff", "woff2" -> VideoFormat.FONT
        "js", "ts", "py", "java", "kt", "cpp", "c", "cs", "go", "rs", "php", "rb", "swift",
        "html", "css", "xml", "json", "yaml", "yml", "sql" -> VideoFormat.CODE
        "db", "sqlite", "sqlite3", "dat" -> VideoFormat.DATA
        else               -> VideoFormat.UNKNOWN
    }

    private fun fetchText(url: String, config: DownloadConfig?): String {
        val conn = openConn(url, config)
        return conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
    }

    private fun openConn(url: String, config: DownloadConfig?): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = config?.timeoutMs ?: 15_000
        conn.readTimeout    = config?.timeoutMs ?: 15_000
        conn.setRequestProperty("User-Agent", config?.userAgent ?: DEFAULT_USER_AGENT)
        conn.setRequestProperty("Accept", "*/*")
        conn.instanceFollowRedirects = true
        config?.headers?.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        if (!config?.cookies.isNullOrBlank()) conn.setRequestProperty("Cookie", config!!.cookies)
        conn.connect()
        return conn
    }
}