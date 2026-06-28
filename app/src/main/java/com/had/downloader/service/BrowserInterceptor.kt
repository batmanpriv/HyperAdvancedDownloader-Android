package com.had.downloader.service

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import javax.inject.Inject
import javax.inject.Singleton

data class InterceptedRequest(
    val url: String,
    val headers: Map<String, String>,
    val method: String,
    val referer: String?,
    val cookies: String?,
    val userAgent: String?,
    val contentType: String?,
    val contentLength: Long,
    val linkType: LinkType,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class BrowserInterceptor @Inject constructor(
    private val scraperEngine: ScraperEngine
) {
    private val interceptedRequests = mutableListOf<InterceptedRequest>()
    private val listeners = mutableListOf<(InterceptedRequest) -> Unit>()

    fun addListener(listener: (InterceptedRequest) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (InterceptedRequest) -> Unit) {
        listeners.remove(listener)
    }

    fun intercept(request: WebResourceRequest, cookieHeader: String?): WebResourceResponse? {
        val url = request.url.toString()
        val headers = request.requestHeaders ?: emptyMap()
        val method = request.method ?: "GET"
        val referer = headers["Referer"]
        val userAgent = headers["User-Agent"]

        val linkType = scraperEngine.classifyUrl(url)
        val isInteresting = linkType != LinkType.OTHER &&
                linkType != LinkType.CODE &&
                linkType != LinkType.IMAGE

        if (!isInteresting && !isMediaUrl(url)) return null

        val intercepted = InterceptedRequest(
            url = url,
            headers = headers,
            method = method,
            referer = referer,
            cookies = cookieHeader,
            userAgent = userAgent,
            contentType = null,
            contentLength = -1L,
            linkType = linkType
        )

        synchronized(interceptedRequests) {
            val existing = interceptedRequests.any { it.url == url }
            if (!existing) {
                interceptedRequests.add(intercepted)
                listeners.forEach { it(intercepted) }
            }
        }

        return null
    }

    fun interceptResponse(
        url: String,
        contentType: String?,
        contentLength: Long,
        headers: Map<String, String>,
        cookies: String?
    ) {
        val linkType = when {
            contentType?.startsWith("video/") == true -> LinkType.VIDEO
            contentType?.startsWith("audio/") == true -> LinkType.AUDIO
            contentType == "application/x-mpegURL" || contentType == "application/vnd.apple.mpegurl" -> LinkType.HLS
            contentType == "application/dash+xml" -> LinkType.DASH
            contentType?.contains("octet-stream") == true -> scraperEngine.classifyUrl(url)
            else -> scraperEngine.classifyUrl(url)
        }

        val isDownloadable = linkType != LinkType.OTHER && linkType != LinkType.CODE
        if (!isDownloadable && contentLength < 1_000_000L) return

        val intercepted = InterceptedRequest(
            url = url,
            headers = headers,
            method = "GET",
            referer = headers["Referer"],
            cookies = cookies,
            userAgent = headers["User-Agent"],
            contentType = contentType,
            contentLength = contentLength,
            linkType = linkType
        )

        synchronized(interceptedRequests) {
            val idx = interceptedRequests.indexOfFirst { it.url == url }
            if (idx >= 0) {
                interceptedRequests[idx] = intercepted
            } else {
                interceptedRequests.add(intercepted)
                listeners.forEach { it(intercepted) }
            }
        }
    }

    fun getIntercepted(): List<InterceptedRequest> =
        synchronized(interceptedRequests) { interceptedRequests.toList() }

    fun clearIntercepted() =
        synchronized(interceptedRequests) { interceptedRequests.clear() }

    fun removeIntercepted(url: String) =
        synchronized(interceptedRequests) { interceptedRequests.removeAll { it.url == url } }

    private fun isMediaUrl(url: String): Boolean {
        val ext = url.substringAfterLast('.').substringBefore('?').lowercase()
        return ext in setOf("mp4", "mkv", "webm", "avi", "mov", "m3u8", "mpd", "ts",
            "mp3", "flac", "aac", "ogg", "m4a", "zip", "rar", "7z", "apk", "pdf")
    }

    fun toDownloadConfig(request: InterceptedRequest, outputPath: String, defaultUserAgent: String): DownloadConfig {
        val headers = request.headers.toMutableMap()
        request.referer?.let { headers["Referer"] = it }
        return DownloadConfig(
            url        = request.url,
            outputPath = outputPath,
            headers    = headers,
            cookies    = request.cookies,
            userAgent  = request.userAgent ?: defaultUserAgent,
            method     = request.method
        )
    }
}