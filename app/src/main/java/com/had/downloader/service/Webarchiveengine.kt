package com.had.downloader.service

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

private const val TAG = "WebArchiveEngine"
private const val MAX_PATH_LEN = 240
private const val MAX_SEG_LEN = 120
private const val STATE_VERSION = 2
private const val MAX_CONCURRENT_ASSETS = 20
private const val ASSET_QUEUE_SIZE = 1000

enum class ArchiveCrawlMode { SINGLE_PAGE, FULL_SITE }

data class ArchiveConfig(
    val targetUrl: String,
    val outputDir: String,
    val mode: ArchiveCrawlMode = ArchiveCrawlMode.SINGLE_PAGE,
    val maxPages: Int = 100,
    val concurrency: Int = 5,
    val downloadExternal: Boolean = false,
    val externalDomains: List<String> = emptyList(),
    val cookies: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36",
    val timeoutMs: Int = 30_000,
    val retries: Int = 3,
    val minifyOutput: Boolean = false,
    val resume: Boolean = false,
    val rateLimit: Double = 10.0,
    val maxAssetSizeBytes: Long = 100L * 1024 * 1024,
    val crawlIframes: Boolean = true,
    val crawlHashRoutes: Boolean = true,
    val followMetaRefresh: Boolean = true
)

data class ArchiveProgress(
    val pages: Long = 0,
    val assets: Long = 0,
    val bytes: Long = 0,
    val errors: Long = 0,
    val status: String = "IDLE",
    val currentUrl: String = "",
    val totalPages: Long = 0,
    val totalAssets: Long = 0,
    val queuedAssets: Long = 0,
    val completedAssets: Long = 0
)

data class ArchiveSession(
    val id: Long = System.currentTimeMillis(),
    val config: ArchiveConfig,
    var progress: ArchiveProgress = ArchiveProgress(),
    var startedAt: Long = System.currentTimeMillis(),
    var completedAt: Long? = null
)

private val SKIP_SCHEMES = setOf("mailto", "tel", "sms", "javascript", "data", "geo", "blob", "about")

@Singleton
class WebArchiveEngine @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _progress = MutableSharedFlow<ArchiveProgress>(extraBufferCapacity = 256)
    val progress: SharedFlow<ArchiveProgress> = _progress.asSharedFlow()

    private val activeSessions = ConcurrentHashMap<Long, Job>()

    fun startArchive(session: ArchiveSession) {
        val job = scope.launch {
            try {
                runCrawl(session)
            } catch (e: CancellationException) {
                emit(session.progress.copy(status = "CANCELLED"))
            } catch (e: Exception) {
                emit(session.progress.copy(status = "FAILED: ${e.message}"))
            }
        }
        activeSessions[session.id] = job
    }

    fun stopArchive(sessionId: Long) {
        activeSessions[sessionId]?.cancel()
        activeSessions.remove(sessionId)
    }

    fun stopAll() {
        activeSessions.values.forEach { it.cancel() }
        activeSessions.clear()
    }

    private suspend fun emit(p: ArchiveProgress) = _progress.emit(p)

    private suspend fun runCrawl(session: ArchiveSession) {
        val cfg = session.config
        val baseUrl = parseBaseUrl(cfg.targetUrl)
        if (baseUrl == null) {
            emit(session.progress.copy(status = "FAILED: Invalid URL"))
            return
        }

        val outputRoot = File(cfg.outputDir, sanitizeFilename(baseUrl.host))
        outputRoot.mkdirs()

        val pages = AtomicLong(0)
        val assets = AtomicLong(0)
        val bytes = AtomicLong(0)
        val errors = AtomicLong(0)
        val queuedAssets = AtomicLong(0)
        val completedAssets = AtomicLong(0)

        val visitedPages = ConcurrentHashMap<String, Boolean>()
        val queuedAssetsSet = ConcurrentHashMap<String, Boolean>()

        val pageSem = Semaphore(if (cfg.mode == ArchiveCrawlMode.SINGLE_PAGE) 1 else cfg.concurrency)
        val assetSem = Semaphore(MAX_CONCURRENT_ASSETS)

        val assetChannel = Channel<Pair<String, String>>(ASSET_QUEUE_SIZE)

        var totalPagesEstimate = 0L
        var totalAssetsEstimate = 0L

        fun makeProgress(status: String, currentUrl: String = ""): ArchiveProgress {
            val totalPages = if (cfg.mode == ArchiveCrawlMode.SINGLE_PAGE) 1L else totalPagesEstimate
            val totalAssets = totalAssetsEstimate
            return ArchiveProgress(
                pages = pages.get(),
                assets = assets.get(),
                bytes = bytes.get(),
                errors = errors.get(),
                status = status,
                currentUrl = currentUrl,
                totalPages = totalPages,
                totalAssets = totalAssets,
                queuedAssets = queuedAssets.get(),
                completedAssets = completedAssets.get()
            )
        }

        emit(makeProgress("RUNNING"))

        suspend fun fetchUrl(url: String): Pair<ByteArray, String>? {
            var lastError: String? = null
            repeat(cfg.retries + 1) { attempt ->
                if (attempt > 0) {
                    val delayTime = (attempt.toLong() * attempt * 1000L).coerceAtMost(10000L)
                    delay(delayTime)
                }
                try {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.connectTimeout = cfg.timeoutMs
                    conn.readTimeout = cfg.timeoutMs
                    conn.setRequestProperty("User-Agent", cfg.userAgent)
                    conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
                    conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                    conn.setRequestProperty("Accept-Encoding", "gzip, deflate")
                    conn.setRequestProperty("Connection", "keep-alive")
                    cfg.headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
                    if (cfg.cookies.isNotEmpty()) {
                        conn.setRequestProperty("Cookie", cfg.cookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
                    }
                    conn.instanceFollowRedirects = true
                    conn.connect()
                    val code = conn.responseCode
                    if (code in 400..499 && code != 429) {
                        conn.disconnect()
                        return null
                    }
                    if (code !in 200..399) {
                        conn.disconnect()
                        throw IOException("HTTP $code")
                    }
                    val ct = conn.contentType ?: ""
                    val encoding = conn.getHeaderField("Content-Encoding") ?: ""
                    val stream = if (encoding.equals("gzip", ignoreCase = true))
                        GZIPInputStream(conn.inputStream) else conn.inputStream
                    val body = stream.use { it.readBytes() }
                    conn.disconnect()
                    bytes.addAndGet(body.size.toLong())
                    return body to ct
                } catch (e: Exception) {
                    lastError = e.message
                    if (attempt == cfg.retries) errors.incrementAndGet()
                }
            }
            return null
        }

        suspend fun fetchAsset(url: String): ByteArray? {
            var lastError: String? = null
            repeat(cfg.retries + 1) { attempt ->
                if (attempt > 0) {
                    val delayTime = (attempt.toLong() * 500L).coerceAtMost(5000L)
                    delay(delayTime)
                }
                try {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.connectTimeout = min(cfg.timeoutMs, 15000)
                    conn.readTimeout = min(cfg.timeoutMs, 15000)
                    conn.setRequestProperty("User-Agent", cfg.userAgent)
                    conn.setRequestProperty("Accept", "*/*")
                    conn.setRequestProperty("Accept-Encoding", "gzip, deflate")
                    cfg.headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
                    if (cfg.cookies.isNotEmpty()) {
                        conn.setRequestProperty("Cookie", cfg.cookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
                    }
                    conn.instanceFollowRedirects = true
                    conn.connect()
                    val code = conn.responseCode
                    if (code !in 200..399) {
                        conn.disconnect()
                        throw IOException("HTTP $code")
                    }

                    val ct = conn.contentType ?: ""
                    if (!isAssetContentType(ct) && !hasAssetExtension(url)) {
                        conn.disconnect()
                        return null
                    }

                    val encoding = conn.getHeaderField("Content-Encoding") ?: ""
                    val stream = if (encoding.equals("gzip", ignoreCase = true))
                        GZIPInputStream(conn.inputStream) else conn.inputStream
                    val body = stream.use { it.readBytes() }
                    conn.disconnect()
                    bytes.addAndGet(body.size.toLong())
                    return body
                } catch (e: Exception) {
                    lastError = e.message
                    if (attempt == cfg.retries) errors.incrementAndGet()
                }
            }
            return null
        }

        fun urlToLocalPath(url: String, isPage: Boolean): File {
            val parsed = runCatching { URL(url) }.getOrNull() ?: return File(outputRoot, "_unknown")
            var rel = parsed.path.trimStart('/')
            val query = parsed.query

            if (isPage) {
                rel = when {
                    rel.isEmpty() -> "index.html"
                    rel.endsWith("/") -> "${rel}index.html"
                    !rel.contains('.') -> "$rel/index.html"
                    else -> rel
                }
            } else {
                if (rel.isEmpty()) rel = "_root"
            }

            if (query != null) {
                val hash = sha256Short(query)
                val ext = rel.substringAfterLast('.', "")
                val stem = if (ext.isNotEmpty()) rel.dropLast(ext.length + 1) else rel
                rel = if (ext.isNotEmpty()) "$stem-$hash.$ext" else "$stem-$hash"
            }

            val parts = rel.split("/").map { seg ->
                var s = seg.trim().trimEnd('.')
                s = s.replace(Regex("[<>:\"|?*\\\\]"), "_")
                if (s.isEmpty() || s == "." || s == "..") "_" else s.take(MAX_SEG_LEN)
            }

            var path = File(outputRoot, parts.joinToString(File.separator))
            if (path.absolutePath.length > MAX_PATH_LEN) {
                val hash = sha256Short(url)
                val ext = path.extension.let { if (it.isNotEmpty()) ".$it" else "" }
                val name = path.nameWithoutExtension.take(40) + "-$hash$ext"
                path = File(path.parent ?: outputRoot.absolutePath, name)
            }
            return path
        }

        fun isSameDomain(url: String): Boolean {
            return runCatching { URL(url).host == baseUrl.host }.getOrDefault(false)
        }

        fun resolveUrl(raw: String, base: String): String? {
            if (raw.isBlank() || raw.startsWith("#") || raw.startsWith("data:")) return null
            val scheme = raw.substringBefore(":")
            if (scheme.lowercase() in SKIP_SCHEMES) return null
            return runCatching {
                val baseU = URL(base)
                val resolved = URL(baseU, raw)
                resolved.toURI().let {
                    "${it.scheme}://${it.host}${if (it.port != -1) ":${it.port}" else ""}${it.path}${it.query?.let { q -> "?$q" } ?: ""}"
                }
            }.getOrNull()
        }

        fun extractLinksWithJsoup(html: String, basePageUrl: String): Pair<List<String>, List<String>> {
            val doc = Jsoup.parse(html, basePageUrl)
            val pageLinks = mutableListOf<String>()
            val assetLinks = mutableListOf<String>()

            fun addUrl(url: String, isPage: Boolean) {
                val abs = resolveUrl(url.trim(), basePageUrl) ?: return
                val isExt = !isSameDomain(abs)
                if (isExt && !cfg.downloadExternal) return
                if (isExt && cfg.externalDomains.isNotEmpty()) {
                    val host = runCatching { URL(abs).host }.getOrDefault("")
                    if (cfg.externalDomains.none { host.endsWith(it) }) return
                }
                if (isPage) {
                    pageLinks.add(abs)
                } else {
                    val isAsset = isAssetContentTypeFromUrl(abs) || hasAssetExtension(abs)
                    if (isAsset) assetLinks.add(abs)
                }
            }

            doc.select("a[href]").forEach { element ->
                val href = element.attr("href")
                if (href.isNotBlank() && !href.startsWith("#") && !href.startsWith("javascript:")) {
                    addUrl(href, true)
                }
            }

            if (cfg.crawlIframes) {
                doc.select("iframe[src]").forEach { element ->
                    addUrl(element.attr("src"), true)
                }
            }

            doc.select("link[href]").forEach { element ->
                val rel = element.attr("rel").lowercase()
                if (rel == "stylesheet" || rel == "icon" || rel == "preload" || rel == "prefetch") {
                    addUrl(element.attr("href"), false)
                }
            }

            doc.select("script[src]").forEach { element ->
                addUrl(element.attr("src"), false)
            }

            doc.select("img[src]").forEach { element ->
                addUrl(element.attr("src"), false)
            }

            doc.select("source[src]").forEach { element ->
                addUrl(element.attr("src"), false)
            }

            doc.select("video[src]").forEach { element ->
                addUrl(element.attr("src"), false)
            }

            doc.select("audio[src]").forEach { element ->
                addUrl(element.attr("src"), false)
            }

            doc.select("track[src]").forEach { element ->
                addUrl(element.attr("src"), false)
            }

            doc.select("[srcset]").forEach { element ->
                val srcset = element.attr("srcset")
                srcset.split(",").forEach { part ->
                    val url = part.trim().split("\\s+".toRegex()).firstOrNull()
                    if (!url.isNullOrBlank()) addUrl(url, false)
                }
            }

            doc.select("[data-src]").forEach { element ->
                addUrl(element.attr("data-src"), false)
            }

            doc.select("[poster]").forEach { element ->
                addUrl(element.attr("poster"), false)
            }

            doc.select("meta[http-equiv=refresh]").forEach { element ->
                if (cfg.followMetaRefresh) {
                    val content = element.attr("content")
                    val urlMatch = Regex("""url=([^"'\s>]+)""", RegexOption.IGNORE_CASE).find(content)
                    urlMatch?.let { addUrl(it.groupValues[1], true) }
                }
            }

            val styleTags = doc.select("style")
            styleTags.forEach { styleTag ->
                val css = styleTag.html()
                val urlPattern = Regex("""url\(['"]?([^'"()\s]+)['"]?\)""", RegexOption.IGNORE_CASE)
                urlPattern.findAll(css).forEach { match ->
                    addUrl(match.groupValues[1], false)
                }
            }

            return pageLinks to assetLinks
        }

        fun rewriteHtmlWithJsoup(html: String, pageUrl: String, pagePath: File): String {
            val doc = Jsoup.parse(html, pageUrl)
            val parentPath = pagePath.parentFile ?: outputRoot

            fun rewriteUrl(original: String): String {
                val abs = resolveUrl(original.trim(), pageUrl) ?: return original
                val localPath = urlToLocalPath(abs, false)
                val rel = localPath.relativeTo(parentPath)
                return rel.path.replace(File.separatorChar, '/')
            }

            fun rewriteAttribute(element: org.jsoup.nodes.Element, attr: String) {
                val value = element.attr(attr)
                if (value.isNotBlank() && !value.startsWith("data:") && !value.startsWith("#") && !value.startsWith("javascript:")) {
                    element.attr(attr, rewriteUrl(value))
                }
            }

            doc.select("a[href]").forEach { element ->
                rewriteAttribute(element, "href")
            }

            doc.select("link[href]").forEach { element ->
                rewriteAttribute(element, "href")
            }

            doc.select("script[src]").forEach { element ->
                rewriteAttribute(element, "src")
            }

            doc.select("img[src]").forEach { element ->
                rewriteAttribute(element, "src")
            }

            doc.select("source[src]").forEach { element ->
                rewriteAttribute(element, "src")
            }

            doc.select("video[src]").forEach { element ->
                rewriteAttribute(element, "src")
            }

            doc.select("audio[src]").forEach { element ->
                rewriteAttribute(element, "src")
            }

            doc.select("iframe[src]").forEach { element ->
                rewriteAttribute(element, "src")
            }

            doc.select("[srcset]").forEach { element ->
                val srcset = element.attr("srcset")
                val newSrcset = srcset.split(",").joinToString(",") { part ->
                    val parts = part.trim().split("\\s+".toRegex())
                    val url = parts.firstOrNull()
                    if (!url.isNullOrBlank()) {
                        val rewritten = rewriteUrl(url)
                        if (parts.size > 1) "$rewritten ${parts.drop(1).joinToString(" ")}" else rewritten
                    } else part
                }
                element.attr("srcset", newSrcset)
            }

            doc.select("[data-src]").forEach { element ->
                rewriteAttribute(element, "data-src")
            }

            doc.select("[poster]").forEach { element ->
                rewriteAttribute(element, "poster")
            }

            doc.select("style").forEach { styleTag ->
                var css = styleTag.html()
                val urlPattern = Regex("""url\(['"]?([^'"()\s]+)['"]?\)""", RegexOption.IGNORE_CASE)
                css = urlPattern.replace(css) { matchResult ->
                    val url = matchResult.groupValues[1]
                    val rewritten = rewriteUrl(url)
                    "url('$rewritten')"
                }
                styleTag.html(css)
            }

            return doc.html()
        }

        suspend fun crawlPage(pageUrl: String) {
            pageSem.withPermit {
                val url = if (cfg.crawlHashRoutes) pageUrl.substringBefore('#') else pageUrl
                emit(makeProgress("RUNNING", url))

                val result = fetchUrl(url)
                if (result == null) {
                    errors.incrementAndGet()
                    return
                }
                val (body, ct) = result

                val isHtml = ct.contains("html", ignoreCase = true) || ct.isEmpty()
                if (!isHtml) {
                    val localPath = urlToLocalPath(url, false)
                    localPath.parentFile?.mkdirs()
                    localPath.writeBytes(body)
                    assets.incrementAndGet()
                    return
                }

                val html = body.toString(Charsets.UTF_8)
                    .let { if (it.startsWith("\uFEFF")) it.drop(1) else it }

                val localPath = urlToLocalPath(url, true)
                localPath.parentFile?.mkdirs()

                val (pageLinks, assetLinks) = extractLinksWithJsoup(html, url)

                var rewritten = rewriteHtmlWithJsoup(html, url, localPath)
                if (cfg.minifyOutput) rewritten = minifyHtml(rewritten)
                localPath.writeText(rewritten, Charsets.UTF_8)

                pages.incrementAndGet()
                emit(makeProgress("RUNNING", url))

                assetLinks.forEach { assetUrl ->
                    if (queuedAssetsSet.putIfAbsent(assetUrl, true) == null) {
                        queuedAssets.incrementAndGet()
                        assetChannel.trySend(assetUrl to url)
                    }
                }

                if (cfg.mode == ArchiveCrawlMode.FULL_SITE) {
                    pageLinks.forEach { linkUrl ->
                        val clean = if (cfg.crawlHashRoutes) linkUrl.substringBefore('#') else linkUrl
                        if (isSameDomain(clean) && visitedPages.putIfAbsent(clean, true) == null) {
                            if (pages.get() < cfg.maxPages) {
                                totalPagesEstimate++
                                scope.launch { crawlPage(clean) }
                            }
                        }
                    }
                }
            }
        }

        suspend fun assetDownloader() {
            for ((assetUrl, referer) in assetChannel) {
                if (assetChannel.isEmpty) break
                assetSem.withPermit {
                    try {
                        val assetLocalPath = urlToLocalPath(assetUrl, false)
                        if (assetLocalPath.exists()) {
                            completedAssets.incrementAndGet()
                            emit(makeProgress("RUNNING", assetUrl))
                            return@withPermit
                        }

                        val assetBody = fetchAsset(assetUrl)
                        if (assetBody == null) {
                            return@withPermit
                        }
                        if (cfg.maxAssetSizeBytes > 0 && assetBody.size > cfg.maxAssetSizeBytes) {
                            return@withPermit
                        }
                        assetLocalPath.parentFile?.mkdirs()
                        assetLocalPath.writeBytes(assetBody)
                        assets.incrementAndGet()
                        completedAssets.incrementAndGet()
                        emit(makeProgress("RUNNING", assetUrl))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        errors.incrementAndGet()
                    } finally {
                        queuedAssets.decrementAndGet()
                    }
                }
            }
        }

        val startUrl = if (cfg.crawlHashRoutes) cfg.targetUrl.substringBefore('#') else cfg.targetUrl
        visitedPages[startUrl] = true
        totalPagesEstimate = 1

        val pageJob = scope.async { crawlPage(startUrl) }

        val assetJobs = (0 until MAX_CONCURRENT_ASSETS).map {
            scope.async { assetDownloader() }
        }

        pageJob.await()

        assetChannel.close()

        assetJobs.forEach { it.await() }

        val finalProg = makeProgress("COMPLETED")
        session.progress = finalProg
        session.completedAt = System.currentTimeMillis()
        emit(finalProg)
        activeSessions.remove(session.id)
        Log.d(TAG, "Archive completed: pages=${pages.get()} assets=${assets.get()} bytes=${bytes.get()}")
    }

    private fun isAssetContentType(ct: String): Boolean {
        val lower = ct.lowercase()
        return lower.contains("text/css") ||
                lower.contains("application/javascript") ||
                lower.contains("application/json") ||
                lower.contains("image/") ||
                lower.contains("font/") ||
                lower.contains("application/font") ||
                lower.contains("video/") ||
                lower.contains("audio/") ||
                lower.contains("application/pdf") ||
                lower.contains("application/zip") ||
                lower.contains("application/gzip")
    }

    private fun isAssetContentTypeFromUrl(url: String): Boolean {
        val ext = url.substringAfterLast('.').substringBefore('?').lowercase()
        return ext in setOf(
            "css", "js", "mjs", "map", "json", "wasm", "webmanifest",
            "png", "jpg", "jpeg", "gif", "webp", "avif", "svg", "ico",
            "woff", "woff2", "ttf", "eot",
            "mp4", "webm", "mp3", "pdf", "ts", "tsx", "jsx", "scss", "less",
            "xml", "txt", "csv", "html", "htm", "xhtml"
        )
    }

    private fun hasAssetExtension(url: String): Boolean {
        val ext = url.substringAfterLast('.').substringBefore('?').lowercase()
        return ext in setOf(
            "css", "js", "mjs", "map", "json", "wasm", "webmanifest",
            "png", "jpg", "jpeg", "gif", "webp", "avif", "svg", "ico",
            "woff", "woff2", "ttf", "eot",
            "mp4", "webm", "mp3", "pdf", "ts", "tsx", "jsx", "scss", "less",
            "xml", "txt", "csv"
        )
    }

    private fun minifyHtml(html: String): String {
        var result = html
        result = Regex(""">\s+<""").replace(result, "><")
        result = Regex("""[ \t]{2,}""").replace(result, " ")
        result = Regex("""\n{2,}""").replace(result, "\n")
        return result
    }

    private fun sha256Short(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.take(4).joinToString("") { "%02x".format(it) }
    }

    private data class ParsedBaseUrl(val scheme: String, val host: String, val port: Int)

    private fun parseBaseUrl(url: String): ParsedBaseUrl? {
        return runCatching {
            val u = URL(url)
            ParsedBaseUrl(u.protocol, u.host, u.port)
        }.getOrNull()
    }

    private fun sanitizeFilename(name: String): String =
        name.replace(Regex("[.:/\\\\]"), "_")
}