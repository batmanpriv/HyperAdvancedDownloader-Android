package com.had.downloader.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FileSizeFetcher"

data class FileSizeResult(
    val url: String,
    val sizeBytes: Long,       
    val acceptsRanges: Boolean,
    val contentType: String,
    val filename: String,
    val loading: Boolean = false,
    val error: String? = null
)

@Singleton
class FileSizeFetcher @Inject constructor() {

    suspend fun fetch(
        url: String,
        headers: Map<String, String> = emptyMap(),
        cookies: String? = null,
        userAgent: String = DEFAULT_USER_AGENT
    ): FileSizeResult = withContext(Dispatchers.IO) {

        if (url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            return@withContext FileSizeResult(
                url = url, sizeBytes = -1, acceptsRanges = false,
                contentType = "", filename = "", error = "Invalid URL"
            )
        }

        runCatching {
            headRequest(url, headers, cookies, userAgent)
        }.getOrElse {
            Log.w(TAG, "HEAD failed for $url, trying GET range: ${it.message}")
            runCatching {
                getRangeRequest(url, headers, cookies, userAgent)
            }.getOrElse { e ->
                Log.e(TAG, "Both HEAD and GET failed for $url: ${e.message}")
                FileSizeResult(
                    url = url, sizeBytes = -1, acceptsRanges = false,
                    contentType = "", filename = "", error = e.message
                )
            }
        }
    }

    private fun headRequest(
        url: String,
        headers: Map<String, String>,
        cookies: String?,
        userAgent: String
    ): FileSizeResult {
        var finalUrl = url
        var redirectCount = 0

        while (redirectCount < 10) {
            val conn = (URL(finalUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 10_000
                readTimeout = 10_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "*/*")
                if (!cookies.isNullOrBlank()) setRequestProperty("Cookie", cookies)
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
                connect()
            }

            val code = conn.responseCode
            if (code in 301..308) {
                val loc = conn.getHeaderField("Location") ?: break
                finalUrl = if (loc.startsWith("http")) loc
                else URL(URL(finalUrl), loc).toString()
                conn.disconnect()
                redirectCount++
                continue
            }

            val size = conn.contentLengthLong
            val acceptRanges = conn.getHeaderField("Accept-Ranges")?.lowercase() == "bytes"
            val ct = conn.contentType ?: ""
            val disp = conn.getHeaderField("Content-Disposition") ?: ""
            val filename = extractFilename(disp, finalUrl)
            conn.disconnect()

            return FileSizeResult(
                url = finalUrl,
                sizeBytes = size,
                acceptsRanges = acceptRanges,
                contentType = ct,
                filename = filename
            )
        }

        throw Exception("Too many redirects")
    }

    private fun getRangeRequest(
        url: String,
        headers: Map<String, String>,
        cookies: String?,
        userAgent: String
    ): FileSizeResult {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "*/*")
            setRequestProperty("Range", "bytes=0-0")
            if (!cookies.isNullOrBlank()) setRequestProperty("Cookie", cookies)
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            connect()
        }

        val code = conn.responseCode
        
        val size = if (code == 206) {
            val contentRange = conn.getHeaderField("Content-Range") ?: ""
            
            contentRange.substringAfterLast('/').toLongOrNull() ?: -1L
        } else {
            conn.contentLengthLong
        }

        val acceptRanges = code == 206 ||
                conn.getHeaderField("Accept-Ranges")?.lowercase() == "bytes"
        val ct = conn.contentType ?: ""
        val disp = conn.getHeaderField("Content-Disposition") ?: ""
        val filename = extractFilename(disp, url)

        runCatching { conn.inputStream.read() }
        conn.disconnect()

        return FileSizeResult(
            url = url,
            sizeBytes = size,
            acceptsRanges = acceptRanges,
            contentType = ct,
            filename = filename
        )
    }

    private fun extractFilename(disposition: String, url: String): String {
        if (disposition.isNotBlank()) {
            val match = Regex(
                "filename\\*?=[\"']?(?:UTF-\\d'[^']*')?([^\"';\\n]+)[\"']?",
                RegexOption.IGNORE_CASE
            ).find(disposition)
            val name = match?.groupValues?.getOrNull(1)?.trim()
            if (!name.isNullOrBlank()) return name
        }
        return url.substringAfterLast('/').substringBefore('?').ifBlank { "" }
    }
}