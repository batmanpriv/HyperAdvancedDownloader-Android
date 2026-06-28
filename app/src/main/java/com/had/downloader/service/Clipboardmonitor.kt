package com.had.downloader.service

import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ClipboardMonitor"

data class ClipboardEvent(
    val urls: List<String>,
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val dismissed: Boolean = false
)

@Singleton
class ClipboardMonitor @Inject constructor() {

    private val _event = MutableStateFlow<ClipboardEvent?>(null)
    val event: StateFlow<ClipboardEvent?> = _event

    private val urlRegex = Regex("""https?://[^\s"'<>\]\[)(\u0000-\u001F]+""")

    private val downloadableExtensions = setOf(
        "mp4", "mkv", "avi", "webm", "mov", "flv", "ts", "m2ts",
        "mp3", "flac", "aac", "ogg", "wav", "m4a", "opus",
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz",
        "pdf", "epub", "mobi", "docx", "doc", "xlsx", "xls", "pptx", "ppt",
        "apk", "exe", "dmg", "deb", "rpm",
        "iso", "img", "torrent", "m3u8", "mpd"
    )

    fun start(context: Context) {
        Log.d(TAG, "ClipboardMonitor ready (paste-on-demand mode)")
    }

    fun stop() {
        Log.d(TAG, "ClipboardMonitor stopped")
    }

    fun dismiss() {
        _event.value = null
    }

    fun pasteAndDetect(context: Context): List<String> {
        val urls = readCurrentClipboard(context)
        if (urls.isNotEmpty()) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val rawText = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
            _event.value = ClipboardEvent(urls = urls, rawText = rawText)
        }
        return urls
    }

    fun readCurrentClipboard(context: Context): List<String> {
        return runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip ?: return emptyList()

            val allText = buildString {
                for (i in 0 until clip.itemCount) {
                    val item = clip.getItemAt(i) ?: continue
                    val text = item.coerceToText(context)?.toString()
                    if (!text.isNullOrBlank()) {
                        append(text)
                        append("\n")
                    }
                }
            }

            if (allText.isBlank()) return emptyList()

            val found = urlRegex.findAll(allText)
                .map { it.value.trimEnd('.', ',', ')', ']', ';', '"', '\'') }
                .filter { isInterestingUrl(it) }
                .distinct()
                .toList()

            Log.d(TAG, "Found ${found.size} URLs in clipboard")
            found
        }.getOrDefault(emptyList())
    }

    private fun isInterestingUrl(url: String): Boolean {
        val ext = url.substringAfterLast('.').substringBefore('?').substringBefore('#').lowercase().trim()
        if (ext in downloadableExtensions) return true

        val lower = url.lowercase()
        return lower.contains("download") ||
                lower.contains("/dl/") ||
                lower.contains("file=") ||
                lower.contains("attachment") ||
                lower.endsWith(".m3u8") ||
                lower.contains("stream") ||
                lower.contains("torrent") ||
                lower.contains("magnet:")
    }
}