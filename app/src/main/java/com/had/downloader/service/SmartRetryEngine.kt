package com.had.downloader.service

import android.util.Log
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SmartRetryEngine"

data class RetryContext(
    val downloadId: Long,
    val url: String,
    val attempt: Int,
    val lastStatusCode: Int,
    val lastError: String?,
    val currentUserAgent: String,
    val currentCookies: String?,
    val currentHeaders: Map<String, String>
)

data class RetryDecision(
    val shouldRetry: Boolean,
    val delayMs: Long,
    val newUserAgent: String?,
    val newCookies: String?,
    val additionalHeaders: Map<String, String>,
    val reason: String
)

@Singleton
class SmartRetryEngine @Inject constructor() {

    private val userAgentPool = listOf(
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15",
        "Mozilla/5.0 (X11; Linux x86_64; rv:124.0) Gecko/20100101 Firefox/124.0",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
        "curl/8.7.1",
        "Wget/1.21.4",
        "python-requests/2.31.0"
    )

    fun decide(ctx: RetryContext): RetryDecision {
        val code = ctx.lastStatusCode

        return when {
            code == 403 -> handleForbidden(ctx)
            code == 429 -> handleRateLimit(ctx)
            code == 503 -> handleServiceUnavailable(ctx)
            code == 401 -> handleUnauthorized(ctx)
            code == 404 -> RetryDecision(
                shouldRetry       = false,
                delayMs           = 0,
                newUserAgent      = null,
                newCookies        = null,
                additionalHeaders = emptyMap(),
                reason            = "404 Not Found — URL does not exist"
            )
            code in 500..599 -> RetryDecision(
                shouldRetry       = ctx.attempt < 5,
                delayMs           = exponentialDelay(ctx.attempt),
                newUserAgent      = null,
                newCookies        = null,
                additionalHeaders = emptyMap(),
                reason            = "Server error $code — waiting ${exponentialDelay(ctx.attempt)}ms"
            )
            ctx.lastError?.contains("timeout", ignoreCase = true) == true -> RetryDecision(
                shouldRetry       = ctx.attempt < 8,
                delayMs           = exponentialDelay(ctx.attempt, base = 2000),
                newUserAgent      = null,
                newCookies        = null,
                additionalHeaders = emptyMap(),
                reason            = "Timeout — increasing delay"
            )
            ctx.lastError?.contains("connection", ignoreCase = true) == true -> RetryDecision(
                shouldRetry       = ctx.attempt < 6,
                delayMs           = exponentialDelay(ctx.attempt),
                newUserAgent      = null,
                newCookies        = null,
                additionalHeaders = emptyMap(),
                reason            = "Connection error — retrying"
            )
            else -> RetryDecision(
                shouldRetry       = ctx.attempt < 5,
                delayMs           = exponentialDelay(ctx.attempt),
                newUserAgent      = null,
                newCookies        = null,
                additionalHeaders = emptyMap(),
                reason            = "Unknown error — generic retry"
            )
        }
    }

    private fun handleForbidden(ctx: RetryContext): RetryDecision {
        val nextUa = rotateUserAgent(ctx.currentUserAgent, ctx.attempt)
        val addHeaders = mutableMapOf<String, String>()

        if (ctx.attempt == 0) {
            addHeaders["Referer"] = ctx.url.substringBefore('/', ctx.url).let {
                val proto = it.substringBefore(':')
                val host  = ctx.url.substringAfter("://").substringBefore('/')
                "$proto://$host/"
            }
        }
        if (ctx.attempt == 2) {
            addHeaders["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
            addHeaders["Accept-Language"] = "en-US,en;q=0.5"
            addHeaders["Accept-Encoding"] = "gzip, deflate, br"
            addHeaders["DNT"] = "1"
            addHeaders["Connection"] = "keep-alive"
        }
        if (ctx.attempt == 3) {
            addHeaders["Cache-Control"] = "no-cache"
            addHeaders["Pragma"] = "no-cache"
        }

        return RetryDecision(
            shouldRetry       = ctx.attempt < 5,
            delayMs           = exponentialDelay(ctx.attempt, base = 1500),
            newUserAgent      = nextUa,
            newCookies        = ctx.currentCookies,
            additionalHeaders = addHeaders,
            reason            = "403 Forbidden — rotating UA to: ${nextUa?.take(30)}..."
        )
    }

    private fun handleRateLimit(ctx: RetryContext): RetryDecision {
        val delayMs = when (ctx.attempt) {
            0    -> 5_000L
            1    -> 15_000L
            2    -> 30_000L
            3    -> 60_000L
            else -> 120_000L
        }
        return RetryDecision(
            shouldRetry       = ctx.attempt < 5,
            delayMs           = delayMs,
            newUserAgent      = if (ctx.attempt >= 2) rotateUserAgent(ctx.currentUserAgent, ctx.attempt) else null,
            newCookies        = ctx.currentCookies,
            additionalHeaders = emptyMap(),
            reason            = "429 Rate Limited — waiting ${delayMs / 1000}s"
        )
    }

    private fun handleServiceUnavailable(ctx: RetryContext): RetryDecision {
        return RetryDecision(
            shouldRetry       = ctx.attempt < 6,
            delayMs           = exponentialDelay(ctx.attempt, base = 3000),
            newUserAgent      = if (ctx.attempt >= 2) rotateUserAgent(ctx.currentUserAgent, ctx.attempt) else null,
            newCookies        = ctx.currentCookies,
            additionalHeaders = emptyMap(),
            reason            = "503 Service Unavailable — waiting ${exponentialDelay(ctx.attempt, 3000) / 1000}s"
        )
    }

    private fun handleUnauthorized(ctx: RetryContext): RetryDecision {
        return RetryDecision(
            shouldRetry       = ctx.attempt < 2,
            delayMs           = 2000L,
            newUserAgent      = rotateUserAgent(ctx.currentUserAgent, ctx.attempt),
            newCookies        = ctx.currentCookies,
            additionalHeaders = emptyMap(),
            reason            = "401 Unauthorized — may need auth cookies"
        )
    }

    suspend fun probeUrl(url: String, config: DownloadConfig): Pair<Int, String?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout  = minOf(config.timeoutMs, 10_000)
                conn.readTimeout     = minOf(config.timeoutMs, 10_000)
                conn.requestMethod   = "HEAD"
                conn.setRequestProperty("User-Agent", config.userAgent)
                conn.instanceFollowRedirects = true
                val code = conn.responseCode
                conn.disconnect()
                code to null
            }.getOrElse { e -> 0 to e.message }
        }

    suspend fun refreshCookies(
        url: String,
        config: DownloadConfig
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout    = 10_000
            conn.setRequestProperty("User-Agent", config.userAgent)
            conn.instanceFollowRedirects = true
            conn.connect()
            val setCookie = conn.headerFields["Set-Cookie"]
            conn.disconnect()
            setCookie?.joinToString("; ") { it.substringBefore(';').trim() }
        }.getOrNull()
    }

    private fun rotateUserAgent(current: String, attempt: Int): String {
        val currentIdx = userAgentPool.indexOf(current)
        val nextIdx    = (currentIdx + attempt + 1) % userAgentPool.size
        return userAgentPool[nextIdx]
    }

    private fun exponentialDelay(attempt: Int, base: Long = 1000L): Long =
        minOf(base * (1L shl attempt), 60_000L)

    fun logDecision(decision: RetryDecision, downloadId: Long) {
        Log.d(TAG, "[$downloadId] Retry decision: retry=${decision.shouldRetry} delay=${decision.delayMs}ms reason=${decision.reason}")
    }
}