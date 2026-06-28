package com.had.downloader.service

import android.util.Log
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ScraperEngine"

enum class LinkType {
    VIDEO, AUDIO, IMAGE, ARCHIVE, DOCUMENT, EBOOK, FONT,
    CODE, DATA, EXECUTABLE, HLS, DASH, TORRENT, MAGNET,
    SUBTITLE, DISK_IMAGE, BACKUP, SPREADSHEET, PRESENTATION,
    DIRECT, OTHER
}

data class ScrapedLink(
    val url: String,
    val text: String,
    val type: LinkType,
    val size: Long = -1L
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

@Singleton
class ScraperEngine @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val cookieJar = CookieJar()

    private val scraperUserAgents = listOf(
        DEFAULT_USER_AGENT,
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/17.4 Safari/605.1.15",
        "curl/8.7.1"
    )

    val extMap: Map<LinkType, Set<String>> = mapOf(
        LinkType.VIDEO to setOf(
            "mp4", "mkv", "avi", "webm", "mov", "flv", "wmv", "ts", "m2ts", "mts",
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

    fun classifyUrl(url: String): LinkType {
        if (url.startsWith("magnet:")) return LinkType.MAGNET
        val ext = url.substringAfterLast('.').substringBefore('?').substringBefore('#').lowercase().trim()
        extMap.forEach { (type, exts) -> if (ext in exts) return type }
        return if (url.contains("download", ignoreCase = true) ||
            url.contains("file=", ignoreCase = true) ||
            url.contains("attach=", ignoreCase = true)) LinkType.DIRECT
        else LinkType.OTHER
    }

    fun typeLabelFor(type: LinkType): String = when (type) {
        LinkType.VIDEO        -> "VIDEO"
        LinkType.AUDIO        -> "AUDIO"
        LinkType.IMAGE        -> "IMAGE"
        LinkType.ARCHIVE      -> "ARCHIVE"
        LinkType.DOCUMENT     -> "DOCUMENT"
        LinkType.EBOOK        -> "EBOOK"
        LinkType.FONT         -> "FONT"
        LinkType.CODE         -> "CODE"
        LinkType.DATA         -> "DATA"
        LinkType.EXECUTABLE   -> "EXECUTABLE"
        LinkType.HLS          -> "HLS"
        LinkType.DASH         -> "DASH"
        LinkType.TORRENT      -> "TORRENT"
        LinkType.MAGNET       -> "MAGNET"
        LinkType.SUBTITLE     -> "SUBTITLE"
        LinkType.DISK_IMAGE   -> "DISK IMAGE"
        LinkType.BACKUP       -> "BACKUP"
        LinkType.SPREADSHEET  -> "SPREADSHEET"
        LinkType.PRESENTATION -> "PRESENTATION"
        LinkType.DIRECT       -> "DIRECT"
        LinkType.OTHER        -> "OTHER"
    }

    suspend fun scrape(
        pageUrl: String,
        filterExts: Set<String> = emptySet(),
        config: DownloadConfig? = null,
        depth: Int = 0,
        maxDepth: Int = 1
    ): List<ScrapedLink> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Scraping: $pageUrl (depth=$depth)")

        val html = fetchPage(pageUrl, config)
        val base = resolveBase(html, pageUrl)
        val links = extractLinks(html, base)

        val results = links.filter { link ->
            filterExts.isEmpty() || filterExts.any { link.url.endsWith(".$it", ignoreCase = true) }
        }

        val subPages = if (depth < maxDepth) {
            links.filter { it.type == LinkType.OTHER && !it.url.contains('.') }
                .take(5)
                .flatMap { scrape(it.url, filterExts, config, depth + 1, maxDepth) }
        } else emptyList()

        (results + subPages).distinctBy { it.url }
    }

    suspend fun fetchCookies(url: String, config: DownloadConfig? = null): String =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = openConn(url, config)
                cookieJar.parseSetCookie(conn.headerFields ?: emptyMap())
                conn.disconnect()
                cookieJar.asHeader()
            }.getOrDefault("")
        }

    suspend fun fetchHeaders(url: String, config: DownloadConfig? = null): Map<String, String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = openConn(url, config)
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
                    return@withContext fetchPage(url, config.copy(userAgent = ua))
                }
            }
            throw Exception("All user-agents failed for: $url")
        }

    private fun extractLinks(html: String, baseUrl: String): List<ScrapedLink> {
        val links = mutableListOf<ScrapedLink>()

        Regex("""<a\s[^>]*href=["']([^"']+)["'][^>]*>([^<]*)</a>""", RegexOption.IGNORE_CASE)
            .findAll(html).forEach { match ->
                val href = match.groupValues[1].trim()
                val text = match.groupValues[2].trim()
                resolveUrl(href, baseUrl)?.let { url ->
                    links.add(ScrapedLink(url, text, classifyUrl(url)))
                }
            }

        Regex("""<source\s[^>]*src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(html).forEach { match ->
                val src = match.groupValues[1].trim()
                resolveUrl(src, baseUrl)?.let { url ->
                    links.add(ScrapedLink(url, "", classifyUrl(url)))
                }
            }

        Regex("""magnet:\?[^\s"'<>]+""").findAll(html).forEach { match ->
            links.add(ScrapedLink(match.value, "Magnet", LinkType.MAGNET))
        }

        Regex("""data-(?:src|url|href|file|download)=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(html).forEach { match ->
                val src = match.groupValues[1].trim()
                resolveUrl(src, baseUrl)?.let { url ->
                    links.add(ScrapedLink(url, "", classifyUrl(url)))
                }
            }

        val allExts = extMap.values.flatten().joinToString("|")
        Regex(""""(https?://[^"]+\\.(?:$allExts)[^"]*)"""")
            .findAll(html).forEach { match ->
                val url = match.groupValues[1]
                links.add(ScrapedLink(url, "", classifyUrl(url)))
            }

        Regex("""<(?:video|audio)[^>]*src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(html).forEach { match ->
                val src = match.groupValues[1].trim()
                resolveUrl(src, baseUrl)?.let { url ->
                    links.add(ScrapedLink(url, "", classifyUrl(url)))
                }
            }

        Regex("""(?:file|download|attachment|href)\s*[:=]\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(html).forEach { match ->
                val src = match.groupValues[1].trim()
                resolveUrl(src, baseUrl)?.let { url ->
                    val type = classifyUrl(url)
                    if (type != LinkType.OTHER) links.add(ScrapedLink(url, "", type))
                }
            }

        return links.distinctBy { it.url }
    }

    private fun resolveUrl(href: String, baseUrl: String): String? {
        if (href.isBlank() || href.startsWith("javascript:") ||
            href.startsWith("mailto:") || href.startsWith("data:")) return null

        return when {
            href.startsWith("http://") || href.startsWith("https://") || href.startsWith("magnet:") -> href
            href.startsWith("//") -> "https:$href"
            href.startsWith("/") -> {
                val base = URL(baseUrl)
                "${base.protocol}://${base.host}$href"
            }
            else -> "${baseUrl.substringBeforeLast('/')}/$href"
        }
    }

    private fun resolveBase(html: String, pageUrl: String): String {
        val baseTag = Regex("""<base\s[^>]*href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)
        return baseTag ?: pageUrl
    }

    private fun fetchPage(url: String, config: DownloadConfig?): String {
        val conn = openConn(url, config)
        return conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
    }

    private fun openConn(url: String, config: DownloadConfig?): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = config?.timeoutMs ?: 15_000
        conn.readTimeout = config?.timeoutMs ?: 15_000
        conn.setRequestProperty("User-Agent", config?.userAgent ?: DEFAULT_USER_AGENT)
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*")
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
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
}