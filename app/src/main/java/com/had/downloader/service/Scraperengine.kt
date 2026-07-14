package com.had.downloader.service

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ScraperEngine"
private const val MIN_FILE_SIZE = 1024L
private const val MAX_CONCURRENT = 10
private const val TIMEOUT_MS = 10000
private const val MAX_RETRIES = 1
private const val CACHE_MAX_SIZE = 200
private const val CACHE_EXPIRY_MS = 30 * 60 * 1000L

enum class LinkType {
    VIDEO, AUDIO, IMAGE, ARCHIVE, DOCUMENT, EBOOK, FONT,
    CODE, DATA, EXECUTABLE, HLS, DASH, TORRENT, MAGNET,
    SUBTITLE, DISK_IMAGE, BACKUP, SPREADSHEET, PRESENTATION,
    DIRECT, API, STREAM, OTHER
}

data class ScrapedLink(
    val url: String,
    val text: String,
    val type: LinkType,
    val size: Long = -1L,
    val mimeType: String = "",
    val lastModified: String = "",
    val filename: String = "",
    val quality: VideoQualityType = VideoQualityType.UNKNOWN,
    val depth: Int = 0,
    val parentUrl: String = "",
    val score: Int = 0
)

data class CookieJar(
    val cookies: MutableMap<String, String> = mutableMapOf()
) {
    fun asHeader(): String = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
    fun parseCookieHeader(header: String) {
        header.split(";").forEach { part ->
            val kv = part.trim().split("=", limit = 2)
            if (kv.size == 2) cookies[kv[0].trim()] = kv[1].trim()
        }
    }
    fun parseSetCookie(headers: Map<String, List<String>>) {
        headers["Set-Cookie"]?.forEach { parseCookieHeader(it.substringBefore(';')) }
    }
}

enum class VideoQualityType(val label: String, val maxHeight: Int) {
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

data class ScrapeStats(
    val totalLinks: Int = 0,
    val uniqueLinks: Int = 0,
    val pagesScanned: Int = 0,
    val durationMs: Long = 0,
    val linksPerSecond: Double = 0.0,
    val errors: Int = 0,
    val qualityDistribution: Map<VideoQualityType, Int> = emptyMap()
)

data class ScrapeResult(
    val links: List<ScrapedLink>,
    val stats: ScrapeStats,
    val qualityOptions: List<VideoQualityType>
)

@Singleton
class ScraperEngine @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val cookieJar = CookieJar()
    private val sessionCache = ConcurrentHashMap<String, String>()
    private val visitedUrls = ConcurrentHashMap<String, Boolean>()
    private val linkCache = LinkCache()
    private var startTime = 0L

    private val scraperUserAgents = listOf(
        DEFAULT_USER_AGENT,
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    )

    private val qualityKeywords = mapOf(
        "4k" to VideoQualityType.UHD_4K,
        "2160" to VideoQualityType.UHD_4K,
        "2k" to VideoQualityType.QHD_2K,
        "1440" to VideoQualityType.QHD_2K,
        "1080" to VideoQualityType.FHD_1080,
        "fhd" to VideoQualityType.FHD_1080,
        "720" to VideoQualityType.HD_720,
        "hd" to VideoQualityType.HD_720,
        "480" to VideoQualityType.SD_480,
        "sd" to VideoQualityType.SD_480,
        "360" to VideoQualityType.SD_360,
        "240" to VideoQualityType.LOW_240,
        "audio" to VideoQualityType.AUDIO_ONLY,
        "music" to VideoQualityType.AUDIO_ONLY
    )

    val extMap: Map<LinkType, Set<String>> = mapOf(
        LinkType.VIDEO to setOf(
            "mp4", "mkv", "webm", "avi", "mov", "flv", "wmv", "ts", "m2ts", "mts",
            "mpeg", "mpg", "3gp", "3g2", "m4v", "f4v", "vob", "ogv", "divx", "xvid",
            "rm", "rmvb", "asf", "amv", "mxf", "roq", "nsv", "yuv"
        ),
        LinkType.AUDIO to setOf(
            "mp3", "flac", "ogg", "wav", "aac", "opus", "m4a", "wma", "aiff", "aif",
            "au", "ra", "mid", "midi", "ac3", "dts", "ape", "mka", "mpa", "amr",
            "spx", "wv", "tta", "m3u", "pls", "xspf"
        ),
        LinkType.IMAGE to setOf(
            "jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "tiff", "tif",
            "ico", "heic", "heif", "avif", "raw", "cr2", "nef", "orf", "arw",
            "dng", "psd", "ai", "eps", "xcf", "jxl", "pbm", "pgm", "ppm"
        ),
        LinkType.ARCHIVE to setOf(
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst", "lz4", "lzma",
            "tgz", "tbz2", "txz", "tar.gz", "tar.bz2", "tar.xz", "tar.zst",
            "cab", "arj", "lzh", "ace", "uue", "jar", "war", "ear", "apk",
            "ipa", "xapk", "aab", "whl", "egg", "gem", "deb", "rpm", "pkg"
        ),
        LinkType.DOCUMENT to setOf(
            "pdf", "doc", "docx", "odt", "rtf", "txt", "tex", "md", "rst",
            "pages", "wpd", "wps", "abw", "fodp", "fodt", "fods"
        ),
        LinkType.SPREADSHEET to setOf(
            "xls", "xlsx", "ods", "csv", "tsv", "numbers", "gnumeric", "fods"
        ),
        LinkType.PRESENTATION to setOf(
            "ppt", "pptx", "odp", "key", "fodp"
        ),
        LinkType.EBOOK to setOf(
            "epub", "mobi", "azw", "azw3", "fb2", "djvu", "lit", "lrf", "pdb",
            "prc", "cbz", "cbr", "cb7", "cbt"
        ),
        LinkType.SUBTITLE to setOf(
            "srt", "vtt", "ass", "ssa", "sub", "sbv", "ttml", "dfxp", "smi", "idx"
        ),
        LinkType.FONT to setOf(
            "ttf", "otf", "woff", "woff2", "eot", "fon", "fnt"
        ),
        LinkType.CODE to setOf(
            "js", "ts", "py", "java", "kt", "cpp", "c", "h", "cs", "go", "rs",
            "php", "rb", "swift", "sh", "bash", "ps1", "bat", "cmd", "lua",
            "r", "m", "pl", "sql", "html", "css", "xml", "json", "yaml", "yml",
            "toml", "ini", "cfg", "conf"
        ),
        LinkType.DATA to setOf(
            "db", "sqlite", "sqlite3", "mdb", "accdb", "dbf", "dat",
            "bin", "hex", "iso", "img", "vmdk", "vhd", "vhdx"
        ),
        LinkType.EXECUTABLE to setOf(
            "exe", "msi", "dmg", "app", "run", "sh", "appimage",
            "deb", "rpm", "flatpak", "snap"
        ),
        LinkType.HLS to setOf("m3u8"),
        LinkType.DASH to setOf("mpd"),
        LinkType.TORRENT to setOf("torrent"),
        LinkType.DISK_IMAGE to setOf(
            "iso", "img", "dmg", "nrg", "mdf", "bin", "cue", "vcd"
        ),
        LinkType.BACKUP to setOf(
            "bak", "backup", "old", "orig", "tmp", "temp"
        )
    )

    private val trackPatterns = listOf(
        Regex("""[?&](utm_|fb_|ref=|source=|campaign=|term=|content=|medium=)""", RegexOption.IGNORE_CASE),
        Regex("""[/\\]track(?:ing)?[/\\]""", RegexOption.IGNORE_CASE),
        Regex("""[/\\]click[/\\]""", RegexOption.IGNORE_CASE)
    )

    private val shortUrlPatterns = listOf(
        Regex("""bit\.ly"""),
        Regex("""tinyurl\.com"""),
        Regex("""goo\.gl"""),
        Regex("""ow\.ly"""),
        Regex("""is\.gd"""),
        Regex("""buff\.ly"""),
        Regex("""t\.co"""),
        Regex("""short\.link"""),
        Regex("""rb\.gy""")
    )

    suspend fun scrape(
        pageUrl: String,
        filterExts: Set<String> = emptySet(),
        config: DownloadConfig? = null,
        qualityFilter: VideoQualityType? = null,
        minFileSize: Long = MIN_FILE_SIZE
    ): ScrapeResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting scrape for: $pageUrl")
        startTime = System.currentTimeMillis()
        visitedUrls.clear()
        linkCache.clear()
        val resultLinks = mutableListOf<ScrapedLink>()
        var errorCount = 0

        try {
            val html = fetchPageFast(pageUrl, config)
            val base = resolveBase(html, pageUrl)
            val document = Jsoup.parse(html, base)
            val allLinks = extractLinksWithJsoup(document, base, pageUrl)

            val downloadableLinks = allLinks.filter { link ->
                link.type != LinkType.OTHER && link.type != LinkType.CODE && link.type != LinkType.IMAGE
            }

            val enhancedLinks = enhanceLinksParallel(downloadableLinks, config)

            resultLinks.addAll(enhancedLinks)

        } catch (e: Exception) {
            Log.e(TAG, "Scrape error: ${e.message}", e)
            errorCount++
        }

        val uniqueLinks = deduplicateLinks(resultLinks)
        val filteredLinks = filterByQuality(uniqueLinks, qualityFilter)
        val finalLinks = filterByMinSize(filteredLinks, minFileSize)

        val duration = System.currentTimeMillis() - startTime
        val stats = ScrapeStats(
            totalLinks = resultLinks.size,
            uniqueLinks = uniqueLinks.size,
            pagesScanned = 1,
            durationMs = duration,
            linksPerSecond = if (duration > 0) resultLinks.size.toDouble() / (duration / 1000.0) else 0.0,
            errors = errorCount,
            qualityDistribution = finalLinks.groupBy { it.quality }.mapValues { it.value.size }
        )

        ScrapeResult(
            links = finalLinks,
            stats = stats,
            qualityOptions = finalLinks.map { it.quality }.distinct().sortedByDescending { it.maxHeight }
        )
    }

    private suspend fun enhanceLinksParallel(
        links: List<ScrapedLink>,
        config: DownloadConfig?
    ): List<ScrapedLink> = withContext(Dispatchers.IO) {
        if (links.isEmpty()) return@withContext emptyList()

        val semaphore = Semaphore(MAX_CONCURRENT)

        val deferreds = links.map { link ->
            async {
                semaphore.withPermit {
                    enhanceLinkInfoFast(link, config)
                }
            }
        }

        deferreds.mapNotNull { it.await() }
    }

    private suspend fun enhanceLinkInfoFast(
        link: ScrapedLink,
        config: DownloadConfig?
    ): ScrapedLink? = withContext(Dispatchers.IO) {
        val cached = linkCache.get(link.url)
        if (cached != null) {
            return@withContext cached
        }

        try {
            val resolvedUrl = resolveShortUrl(link.url)
            val fileInfo = fetchFileInfoFast(resolvedUrl, config)
            val quality = detectQuality(link.url, fileInfo.filename, fileInfo.mimeType)

            val enhanced = link.copy(
                size = fileInfo.size,
                mimeType = fileInfo.mimeType,
                lastModified = fileInfo.lastModified,
                filename = fileInfo.filename.ifBlank { link.url.substringAfterLast('/').substringBefore('?') },
                quality = quality
            )

            linkCache.put(link.url, enhanced)
            enhanced
        } catch (e: Exception) {
            Log.w(TAG, "Failed to enhance link ${link.url}: ${e.message}")
            link
        }
    }

    private suspend fun fetchFileInfoFast(
        url: String,
        config: DownloadConfig?
    ): FileInfoResult = withContext(Dispatchers.IO) {
        try {
            val conn = openHeadConnectionFast(url, config)
            conn.connect()
            val code = conn.responseCode

            if (code in 200..299) {
                val result = FileInfoResult(
                    size = conn.contentLengthLong,
                    mimeType = conn.contentType ?: "",
                    lastModified = conn.getHeaderField("Last-Modified") ?: "",
                    filename = extractFilename(conn.getHeaderField("Content-Disposition") ?: "", url)
                )
                conn.disconnect()
                return@withContext result
            } else if (code == 403 || code == 405) {
                conn.disconnect()
                val rangeConn = openRangeConnectionFast(url, config)
                rangeConn.connect()
                if (rangeConn.responseCode == 206) {
                    val contentRange = rangeConn.getHeaderField("Content-Range") ?: ""
                    val size = contentRange.substringAfterLast('/').toLongOrNull() ?: -1L
                    val result = FileInfoResult(
                        size = size,
                        mimeType = rangeConn.contentType ?: "",
                        lastModified = rangeConn.getHeaderField("Last-Modified") ?: "",
                        filename = extractFilename(rangeConn.getHeaderField("Content-Disposition") ?: "", url)
                    )
                    rangeConn.disconnect()
                    return@withContext result
                } else {
                    val result = FileInfoResult(
                        size = rangeConn.contentLengthLong,
                        mimeType = rangeConn.contentType ?: "",
                        lastModified = rangeConn.getHeaderField("Last-Modified") ?: "",
                        filename = extractFilename(rangeConn.getHeaderField("Content-Disposition") ?: "", url)
                    )
                    rangeConn.disconnect()
                    return@withContext result
                }
            }
            conn.disconnect()
            FileInfoResult(-1L, "", "", "")
        } catch (e: Exception) {
            Log.w(TAG, "fetchFileInfoFast error for $url: ${e.message}")
            FileInfoResult(-1L, "", "", "")
        }
    }

    private fun openHeadConnectionFast(url: String, config: DownloadConfig?): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS / 2
        conn.readTimeout = TIMEOUT_MS / 2
        conn.requestMethod = "HEAD"
        conn.setRequestProperty("User-Agent", config?.userAgent ?: DEFAULT_USER_AGENT)
        config?.headers?.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        if (cookieJar.asHeader().isNotBlank()) {
            conn.setRequestProperty("Cookie", cookieJar.asHeader())
        }
        conn.instanceFollowRedirects = true
        return conn
    }

    private fun openRangeConnectionFast(url: String, config: DownloadConfig?): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS / 2
        conn.readTimeout = TIMEOUT_MS / 2
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", config?.userAgent ?: DEFAULT_USER_AGENT)
        conn.setRequestProperty("Range", "bytes=0-0")
        config?.headers?.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        if (cookieJar.asHeader().isNotBlank()) {
            conn.setRequestProperty("Cookie", cookieJar.asHeader())
        }
        conn.instanceFollowRedirects = true
        return conn
    }

    private suspend fun fetchPageFast(url: String, config: DownloadConfig?): String =
        withContext(Dispatchers.IO) {
            var lastException: Exception? = null
            for (ua in scraperUserAgents) {
                try {
                    return@withContext fetchPageWithAgentFast(url, config?.copy(userAgent = ua))
                } catch (e: Exception) {
                    lastException = e
                }
            }
            throw lastException ?: Exception("All user-agents failed for: $url")
        }

    private fun fetchPageWithAgentFast(url: String, config: DownloadConfig?): String {
        val conn = openConnFast(url, config)
        return conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
    }

    private fun openConnFast(url: String, config: DownloadConfig?): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = config?.timeoutMs?.let { it / 2 } ?: (TIMEOUT_MS / 2)
        conn.readTimeout = config?.timeoutMs?.let { it / 2 } ?: (TIMEOUT_MS / 2)
        conn.setRequestProperty("User-Agent", config?.userAgent ?: DEFAULT_USER_AGENT)
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*")
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        conn.setRequestProperty("Connection", "close")
        val cookies = buildString {
            if (cookieJar.asHeader().isNotBlank()) append(cookieJar.asHeader())
            if (!config?.cookies.isNullOrBlank()) {
                if (isNotBlank()) append("; ")
                append(config!!.cookies)
            }
        }
        if (cookies.isNotBlank()) conn.setRequestProperty("Cookie", cookies)
        config?.headers?.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        conn.instanceFollowRedirects = true
        conn.connect()
        cookieJar.parseSetCookie(conn.headerFields ?: emptyMap())
        return conn
    }

    private fun extractLinksWithJsoup(
        document: Document,
        baseUrl: String,
        pageUrl: String
    ): List<ScrapedLink> {
        val links = mutableListOf<ScrapedLink>()
        val base = URL(baseUrl)

        fun resolveWithUrl(href: String): String? {
            if (href.isBlank() || href.startsWith("javascript:") ||
                href.startsWith("mailto:") || href.startsWith("data:")) return null
            return try {
                val resolved = URL(base, href)
                val proto = resolved.protocol
                if (proto != "http" && proto != "https") return null
                resolved.toString()
            } catch (e: Exception) {
                null
            }
        }

        document.select("a[href]").forEach { element ->
            val href = element.attr("href")
            val text = element.text().trim()
            resolveWithUrl(href)?.let { url ->
                if (!isTrackingLink(url)) {
                    links.add(ScrapedLink(url, text, classifyUrl(url), parentUrl = pageUrl))
                }
            }
        }

        document.select("source[src]").forEach { element ->
            val src = element.attr("src")
            resolveWithUrl(src)?.let { url ->
                links.add(ScrapedLink(url, "", classifyUrl(url), parentUrl = pageUrl))
            }
        }

        document.select("video[src], audio[src]").forEach { element ->
            val src = element.attr("src")
            resolveWithUrl(src)?.let { url ->
                links.add(ScrapedLink(url, "", classifyUrl(url), parentUrl = pageUrl))
            }
        }

        document.select("link[href]").forEach { element ->
            val rel = element.attr("rel").lowercase()
            if (rel == "stylesheet" || rel == "icon" || rel == "preload" || rel == "prefetch") {
                val href = element.attr("href")
                resolveWithUrl(href)?.let { url ->
                    links.add(ScrapedLink(url, "", classifyUrl(url), parentUrl = pageUrl))
                }
            }
        }

        document.select("script[src]").forEach { element ->
            val src = element.attr("src")
            resolveWithUrl(src)?.let { url ->
                links.add(ScrapedLink(url, "", classifyUrl(url), parentUrl = pageUrl))
            }
        }

        document.select("img[src]").forEach { element ->
            val src = element.attr("src")
            resolveWithUrl(src)?.let { url ->
                links.add(ScrapedLink(url, "", classifyUrl(url), parentUrl = pageUrl))
            }
        }

        document.select("[poster]").forEach { element ->
            val src = element.attr("poster")
            resolveWithUrl(src)?.let { url ->
                links.add(ScrapedLink(url, "", classifyUrl(url), parentUrl = pageUrl))
            }
        }

        document.select("iframe[src]").forEach { element ->
            val src = element.attr("src")
            resolveWithUrl(src)?.let { url ->
                links.add(ScrapedLink(url, "", classifyUrl(url), parentUrl = pageUrl))
            }
        }

        document.select("meta[http-equiv=refresh]").forEach { element ->
            val content = element.attr("content")
            val urlMatch = Regex("""url=([^"'\s>]+)""", RegexOption.IGNORE_CASE).find(content)
            urlMatch?.let { match ->
                resolveWithUrl(match.groupValues[1])?.let { url ->
                    links.add(ScrapedLink(url, "", classifyUrl(url), parentUrl = pageUrl))
                }
            }
        }

        document.select("[srcset]").forEach { element ->
            val srcset = element.attr("srcset")
            srcset.split(",").forEach { part ->
                val src = part.trim().split("\\s+".toRegex()).firstOrNull()
                if (!src.isNullOrBlank()) {
                    resolveWithUrl(src)?.let { url ->
                        if (!isTrackingLink(url)) {
                            links.add(ScrapedLink(url, "", classifyUrl(url), parentUrl = pageUrl))
                        }
                    }
                }
            }
        }

        document.select("[data-src]").forEach { element ->
            val src = element.attr("data-src")
            resolveWithUrl(src)?.let { url ->
                links.add(ScrapedLink(url, "", classifyUrl(url), parentUrl = pageUrl))
            }
        }

        document.select("style").forEach { styleTag ->
            val css = styleTag.html()
            val urlPattern = Regex("""url\(['"]?([^'"()\s]+)['"]?\)""", RegexOption.IGNORE_CASE)
            urlPattern.findAll(css).forEach { match ->
                resolveWithUrl(match.groupValues[1])?.let { url ->
                    links.add(ScrapedLink(url, "", classifyUrl(url), parentUrl = pageUrl))
                }
            }
        }

        document.select("a").forEach { element ->
            val onclick = element.attr("onclick")
            val downloadMatch = Regex("""(?:location\.href|window\.open|download)\s*=\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
            downloadMatch.find(onclick)?.let { match ->
                resolveWithUrl(match.groupValues[1])?.let { url ->
                    links.add(ScrapedLink(url, "", classifyUrl(url), parentUrl = pageUrl))
                }
            }
        }

        val magnetMatches = Regex("""magnet:\?[^\s"'<>]+""").findAll(document.html())
        magnetMatches.forEach { match ->
            links.add(ScrapedLink(match.value, "Magnet", LinkType.MAGNET, parentUrl = pageUrl))
        }

        val allExts = extMap.values.flatten().joinToString("|")
        val quotedUrlRegex = Regex("""["'](https?://[^"']+\.(?:$allExts)[^"']*)["']""", RegexOption.IGNORE_CASE)
        quotedUrlRegex.findAll(document.html()).forEach { match ->
            val url = match.groupValues[1]
            links.add(ScrapedLink(url, "", classifyUrl(url), parentUrl = pageUrl))
        }

        return links.distinctBy { it.url }
    }

    private fun deduplicateLinks(links: List<ScrapedLink>): List<ScrapedLink> {
        val seen = mutableSetOf<String>()
        return links.filter { link ->
            val normalized = normalizeUrl(link.url)
            if (seen.contains(normalized)) false
            else {
                seen.add(normalized)
                !isTrackingLink(link.url)
            }
        }
    }

    private fun normalizeUrl(url: String): String {
        return url.replace(Regex("""https?://"""), "")
            .replace(Regex("""/#.*$"""), "")
            .replace(Regex("""\?.*$"""), "")
            .lowercase()
            .trim('/')
    }

    private fun isTrackingLink(url: String): Boolean {
        return trackPatterns.any { it.containsMatchIn(url) }
    }

    private suspend fun resolveShortUrl(url: String): String = withContext(Dispatchers.IO) {
        if (shortUrlPatterns.none { it.containsMatchIn(url) }) return@withContext url
        runCatching {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.instanceFollowRedirects = false
            conn.requestMethod = "HEAD"
            conn.connect()
            val location = conn.getHeaderField("Location")
            conn.disconnect()
            location ?: url
        }.getOrDefault(url)
    }

    private data class FileInfoResult(
        val size: Long,
        val mimeType: String,
        val lastModified: String,
        val filename: String
    )

    private fun extractFilename(disposition: String, url: String): String {
        if (disposition.isNotBlank()) {
            val pattern = Regex("""filename\*?=["']?(?:UTF-8'[^']*'|UTF-\d'[^']*')?([^"';\\n]+)["']?""", RegexOption.IGNORE_CASE)
            val match = pattern.find(disposition)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return url.substringAfterLast('/').substringBefore('?')
    }

    private fun detectQuality(
        url: String,
        filename: String,
        mimeType: String
    ): VideoQualityType {
        if (mimeType.startsWith("audio/")) return VideoQualityType.AUDIO_ONLY
        val combined = "$url $filename".lowercase()
        qualityKeywords.forEach { (keyword, quality) ->
            if (keyword in combined) return quality
        }
        val heightMatch = Regex("""(\d{3,4})[pP]""").find(combined)
        if (heightMatch != null) {
            val height = heightMatch.groupValues[1].toIntOrNull() ?: 0
            return when {
                height >= 2160 -> VideoQualityType.UHD_4K
                height >= 1440 -> VideoQualityType.QHD_2K
                height >= 1080 -> VideoQualityType.FHD_1080
                height >= 720 -> VideoQualityType.HD_720
                height >= 480 -> VideoQualityType.SD_480
                height >= 360 -> VideoQualityType.SD_360
                else -> VideoQualityType.LOW_240
            }
        }
        return VideoQualityType.UNKNOWN
    }

    private fun filterByQuality(
        links: List<ScrapedLink>,
        qualityFilter: VideoQualityType?
    ): List<ScrapedLink> {
        if (qualityFilter == null || qualityFilter == VideoQualityType.UNKNOWN) return links
        return links.filter { it.quality == qualityFilter || it.quality == VideoQualityType.UNKNOWN }
    }

    private fun filterByMinSize(
        links: List<ScrapedLink>,
        minSize: Long
    ): List<ScrapedLink> {
        if (minSize <= 0) return links
        return links.filter { it.size < 0 || it.size >= minSize }
    }

    fun classifyUrl(url: String): LinkType {
        if (url.startsWith("magnet:")) return LinkType.MAGNET
        if (url.contains(".m3u8", ignoreCase = true)) return LinkType.HLS
        if (url.contains(".mpd", ignoreCase = true)) return LinkType.DASH

        val ext = url.substringAfterLast('.').substringBefore('?').substringBefore('#').lowercase().trim()
        extMap.forEach { (type, exts) ->
            if (ext in exts) {
                return type
            }
        }

        val lower = url.lowercase()
        when {
            lower.contains("download") || lower.contains("file=") || lower.contains("attach=") -> return LinkType.DIRECT
            lower.contains("/api/") -> return LinkType.API
            lower.contains("/stream/") -> return LinkType.STREAM
        }

        return LinkType.OTHER
    }

    fun typeLabelFor(type: LinkType): String = when (type) {
        LinkType.VIDEO -> "VIDEO"
        LinkType.AUDIO -> "AUDIO"
        LinkType.IMAGE -> "IMAGE"
        LinkType.ARCHIVE -> "ARCHIVE"
        LinkType.DOCUMENT -> "DOCUMENT"
        LinkType.EBOOK -> "EBOOK"
        LinkType.FONT -> "FONT"
        LinkType.CODE -> "CODE"
        LinkType.DATA -> "DATA"
        LinkType.EXECUTABLE -> "EXECUTABLE"
        LinkType.HLS -> "HLS"
        LinkType.DASH -> "DASH"
        LinkType.TORRENT -> "TORRENT"
        LinkType.MAGNET -> "MAGNET"
        LinkType.SUBTITLE -> "SUBTITLE"
        LinkType.DISK_IMAGE -> "DISK IMAGE"
        LinkType.BACKUP -> "BACKUP"
        LinkType.SPREADSHEET -> "SPREADSHEET"
        LinkType.PRESENTATION -> "PRESENTATION"
        LinkType.DIRECT -> "DIRECT"
        LinkType.API -> "API"
        LinkType.STREAM -> "STREAM"
        LinkType.OTHER -> "OTHER"
    }

    private fun resolveBase(html: String, pageUrl: String): String {
        val baseTag = Regex("""<base\s[^>]*href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)
        return baseTag ?: pageUrl
    }

    suspend fun fetchCookies(url: String, config: DownloadConfig? = null): String =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = openConnFast(url, config)
                cookieJar.parseSetCookie(conn.headerFields ?: emptyMap())
                conn.disconnect()
                cookieJar.asHeader()
            }.getOrDefault("")
        }

    suspend fun fetchHeaders(url: String, config: DownloadConfig? = null): Map<String, String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = openConnFast(url, config)
                val headers = conn.headerFields
                    ?.filterKeys { it != null }
                    ?.mapKeys { it.key!! }
                    ?.mapValues { it.value.firstOrNull() ?: "" }
                    ?: emptyMap()
                conn.disconnect()
                headers
            }.getOrDefault(emptyMap())
        }

    suspend fun fetchWithFallback(url: String, config: DownloadConfig): String =
        withContext(Dispatchers.IO) {
            scraperUserAgents.forEach { ua ->
                runCatching {
                    return@withContext fetchPageFast(url, config.copy(userAgent = ua))
                }
            }
            throw Exception("All user-agents failed for: $url")
        }

    fun saveSession(url: String, sessionData: String) {
        sessionCache[url] = sessionData
    }

    fun getSession(url: String): String? {
        return sessionCache[url]
    }

    fun clearCache() {
        visitedUrls.clear()
        linkCache.clear()
        sessionCache.clear()
        cookieJar.cookies.clear()
    }

    private class LinkCache {
        private data class Entry(
            val value: ScrapedLink,
            val timestamp: Long = System.currentTimeMillis()
        )

        private val cache = object : LinkedHashMap<String, Entry>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>): Boolean {
                return size > CACHE_MAX_SIZE
            }
        }

        @Synchronized
        fun get(key: String): ScrapedLink? {
            val entry = cache[key] ?: return null
            if (System.currentTimeMillis() - entry.timestamp > CACHE_EXPIRY_MS) {
                cache.remove(key)
                return null
            }
            return entry.value
        }

        @Synchronized
        fun put(key: String, value: ScrapedLink) {
            cache[key] = Entry(value)
        }

        @Synchronized
        fun clear() {
            cache.clear()
        }

        @Synchronized
        fun size(): Int = cache.size
    }
}