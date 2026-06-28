package com.had.downloader.service

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
    val currentUrl: String = ""
)

data class ArchiveSession(
    val id: Long = System.currentTimeMillis(),
    val config: ArchiveConfig,
    var progress: ArchiveProgress = ArchiveProgress(),
    var startedAt: Long = System.currentTimeMillis(),
    var completedAt: Long? = null
)

private val ASSET_EXTS = setOf(
    "css","js","mjs","map","json","wasm","webmanifest",
    "png","jpg","jpeg","gif","webp","avif","svg","ico",
    "woff","woff2","ttf","eot",
    "mp4","webm","mp3","pdf","ts","tsx","jsx","scss","less",
    "xml","txt","csv"
)

private val SKIP_SCHEMES = setOf("mailto","tel","sms","javascript","data","geo","blob","about")

@Singleton
class WebArchiveEngine @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _progress = MutableSharedFlow<ArchiveProgress>(extraBufferCapacity = 256)
    val progress: SharedFlow<ArchiveProgress> = _progress.asSharedFlow()

    private val activeSessions = ConcurrentHashMap<Long, Job>()

    fun startArchive(session: ArchiveSession) {
        val job = scope.launch {
            runCatching { runCrawl(session) }.onFailure { e ->
                if (e !is CancellationException) {
                    emit(session.progress.copy(status = "FAILED: ${e.message}"))
                }
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
        val baseUrl = parseBaseUrl(cfg.targetUrl) ?: run {
            emit(session.progress.copy(status = "FAILED: Invalid URL"))
            return
        }

        val outputRoot = File(cfg.outputDir, sanitizeFilename(baseUrl.host))
        outputRoot.mkdirs()

        val pages = AtomicLong(0)
        val assets = AtomicLong(0)
        val bytes = AtomicLong(0)
        val errors = AtomicLong(0)

        val visitedPages = ConcurrentHashMap<String, Boolean>()
        val queuedAssets = ConcurrentHashMap<String, Boolean>()

        val pageSem = Semaphore(if (cfg.mode == ArchiveCrawlMode.SINGLE_PAGE) 1 else cfg.concurrency)
        val assetSem = Semaphore(cfg.concurrency * 4)

        fun makeProgress(status: String, currentUrl: String = "") = ArchiveProgress(
            pages = pages.get(), assets = assets.get(),
            bytes = bytes.get(), errors = errors.get(),
            status = status, currentUrl = currentUrl
        )

        emit(makeProgress("RUNNING"))

        suspend fun fetchUrl(url: String): Pair<ByteArray, String>? {
            repeat(cfg.retries + 1) { attempt ->
                if (attempt > 0) delay(attempt.toLong() * attempt * 1000L)
                runCatching {
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
                    if (code in 400..499 && code != 429) return null
                    if (code !in 200..399) throw IOException("HTTP $code")
                    val ct = conn.contentType ?: ""
                    val encoding = conn.getHeaderField("Content-Encoding") ?: ""
                    val stream = if (encoding.equals("gzip", ignoreCase = true))
                        GZIPInputStream(conn.inputStream) else conn.inputStream
                    val body = stream.use { it.readBytes() }
                    conn.disconnect()
                    bytes.addAndGet(body.size.toLong())
                    return body to ct
                }.onFailure { e ->
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
                resolved.toURI().let { "${it.scheme}://${it.host}${if (it.port != -1) ":${it.port}" else ""}${it.path}${it.query?.let { q -> "?$q" } ?: ""}" }
            }.getOrNull()
        }

        fun extractLinks(html: String, basePageUrl: String): Pair<List<String>, List<String>> {
            val pageLinks = mutableListOf<String>()
            val assetLinks = mutableListOf<String>()

            val srcPattern = Regex("""(?:src|href|data-src|poster)=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val urlPattern = Regex("""url\(\s*['"]?([^'"()\s]+)['"]?\s*\)""", RegexOption.IGNORE_CASE)
            val importPattern = Regex("""@import\s+['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
            val srcsetPattern = Regex("""srcset=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val metaRefreshPattern = Regex("""content=["']?\d+;\s*url=([^"'\s>]+)""", RegexOption.IGNORE_CASE)

            fun addUrl(raw: String, isPage: Boolean) {
                val abs = resolveUrl(raw.trim(), basePageUrl) ?: return
                val isExt = !isSameDomain(abs)
                if (isExt && !cfg.downloadExternal) return
                if (isExt && cfg.externalDomains.isNotEmpty()) {
                    val host = runCatching { URL(abs).host }.getOrDefault("")
                    if (cfg.externalDomains.none { host.endsWith(it) }) return
                }
                if (isPage) pageLinks.add(abs)
                else {
                    val ext = abs.substringAfterLast('.').substringBefore('?').lowercase()
                    if (ext.isEmpty() || ext in ASSET_EXTS) assetLinks.add(abs)
                }
            }

            srcPattern.findAll(html).forEach { m ->
                val raw = m.groupValues[1]
                val tag = html.lastIndexOf('<', m.range.first).let { idx ->
                    if (idx >= 0) html.substring(idx, min(idx + 10, html.length)).lowercase() else ""
                }
                val isPage = tag.contains("<a") || tag.contains("<iframe")
                addUrl(raw, isPage && cfg.mode == ArchiveCrawlMode.FULL_SITE)
            }

            urlPattern.findAll(html).forEach { m -> addUrl(m.groupValues[1], false) }
            importPattern.findAll(html).forEach { m -> addUrl(m.groupValues[1], false) }

            srcsetPattern.findAll(html).forEach { m ->
                m.groupValues[1].split(",").forEach { part ->
                    val url = part.trim().split("\\s+".toRegex()).firstOrNull() ?: return@forEach
                    addUrl(url, false)
                }
            }

            if (cfg.followMetaRefresh) {
                metaRefreshPattern.findAll(html).forEach { m -> addUrl(m.groupValues[1], true) }
            }

            return pageLinks to assetLinks
        }

        fun rewriteHtml(html: String, pageUrl: String, pagePath: File): String {
            var result = html

            fun rewrite(raw: String, isPage: Boolean): String {
                val abs = resolveUrl(raw.trim(), pageUrl) ?: return raw
                val localPath = urlToLocalPath(abs, isPage)
                val rel = localPath.relativeTo(pagePath.parentFile ?: outputRoot)
                return rel.path.replace(File.separatorChar, '/')
            }

            result = Regex("""(src|href|poster|data-src)=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .replace(result) { mr ->
                    val attr = mr.groupValues[1]
                    val raw = mr.groupValues[2]
                    if (raw.startsWith("data:") || raw.startsWith("#") || raw.startsWith("javascript:")) mr.value
                    else {
                        val isPage = attr.equals("href", ignoreCase = true)
                        val rewritten = rewrite(raw, isPage)
                        "$attr=\"$rewritten\""
                    }
                }

            result = Regex("""url\(\s*['"]?([^'"()\s]+)['"]?\s*\)""", RegexOption.IGNORE_CASE)
                .replace(result) { mr ->
                    val raw = mr.groupValues[1]
                    if (raw.startsWith("data:")) mr.value
                    else "url('${rewrite(raw, false)}')"
                }

            return result
        }

        fun minifyHtml(html: String): String {
            var result = html
            result = Regex(""">\s+<""").replace(result, "><")
            result = Regex("""[ \t]{2,}""").replace(result, " ")
            result = Regex("""\n{2,}""").replace(result, "\n")
            return result
        }

        suspend fun crawlPage(pageUrl: String) {
            pageSem.withPermit {
                val url = if (cfg.crawlHashRoutes) pageUrl.substringBefore('#') else pageUrl
                emit(makeProgress("RUNNING", url))

                val (body, ct) = fetchUrl(url) ?: run { errors.incrementAndGet(); return }

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

                val (pageLinks, assetLinks) = extractLinks(html, url)

                var rewritten = rewriteHtml(html, url, localPath)
                if (cfg.minifyOutput) rewritten = minifyHtml(rewritten)
                localPath.writeText(rewritten, Charsets.UTF_8)

                pages.incrementAndGet()
                emit(makeProgress("RUNNING", url))

                assetLinks.forEach { assetUrl ->
                    if (queuedAssets.putIfAbsent(assetUrl, true) == null) {
                        scope.launch {
                            assetSem.withPermit {
                                val assetLocalPath = urlToLocalPath(assetUrl, false)
                                if (!assetLocalPath.exists()) {
                                    val (assetBody, _) = fetchUrl(assetUrl) ?: return@withPermit
                                    if (cfg.maxAssetSizeBytes > 0 && assetBody.size > cfg.maxAssetSizeBytes) return@withPermit
                                    assetLocalPath.parentFile?.mkdirs()
                                    assetLocalPath.writeBytes(assetBody)
                                    assets.incrementAndGet()
                                    emit(makeProgress("RUNNING", assetUrl))
                                }
                            }
                        }
                    }
                }

                if (cfg.mode == ArchiveCrawlMode.FULL_SITE) {
                    pageLinks.forEach { linkUrl ->
                        val clean = if (cfg.crawlHashRoutes) linkUrl.substringBefore('#') else linkUrl
                        if (isSameDomain(clean) && visitedPages.putIfAbsent(clean, true) == null) {
                            if (pages.get() < cfg.maxPages) {
                                scope.launch { crawlPage(clean) }
                            }
                        }
                    }
                }
            }
        }

        val startUrl = if (cfg.crawlHashRoutes) cfg.targetUrl.substringBefore('#') else cfg.targetUrl
        visitedPages[startUrl] = true

        crawlPage(startUrl)

        delay(500L)

        val finalProg = makeProgress("COMPLETED")
        session.progress = finalProg
        session.completedAt = System.currentTimeMillis()
        emit(finalProg)
        activeSessions.remove(session.id)
        Log.d(TAG, "Archive completed: pages=${pages.get()} assets=${assets.get()} bytes=${bytes.get()}")
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