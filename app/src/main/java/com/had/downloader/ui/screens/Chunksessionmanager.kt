package com.had.downloader.ui.screens

import android.content.Context
import com.had.downloader.service.ChunkInfo
import com.had.downloader.service.ChunkStatus
import java.io.File

data class ChunkSessionData(
    val url: String,
    val outputPath: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val chunks: List<ChunkInfo>,
    val savedAt: Long
)

class ChunkSessionManager(private val context: Context) {

    private val dir: File by lazy {
        File(context.filesDir, "chunk_sessions").apply { mkdirs() }
    }

    private fun fileFor(id: Long) = File(dir, "session_$id.dat")

    fun save(
        downloadId: Long,
        url: String,
        outputPath: String,
        totalBytes: Long,
        downloadedBytes: Long,
        chunks: List<ChunkInfo>
    ) {
        runCatching {
            val sb = StringBuilder()
            sb.appendLine(url)
            sb.appendLine(outputPath)
            sb.appendLine(totalBytes)
            sb.appendLine(downloadedBytes)
            sb.appendLine(System.currentTimeMillis())
            chunks.forEach { c ->
                sb.appendLine("${c.index},${c.start},${c.end},${c.downloaded},${c.status.name}")
            }
            fileFor(downloadId).writeText(sb.toString())
        }
    }

    fun load(downloadId: Long): ChunkSessionData? = runCatching {
        val f = fileFor(downloadId)
        if (!f.exists()) return null
        val lines = f.readText().lines().filter { it.isNotBlank() }
        if (lines.size < 5) return null
        val url = lines[0]
        val outputPath = lines[1]
        val totalBytes = lines[2].toLong()
        val downloadedBytes = lines[3].toLong()
        val savedAt = lines[4].toLong()
        val chunks = lines.drop(5).mapNotNull { line ->
            val p = line.split(",")
            if (p.size < 5) return@mapNotNull null
            ChunkInfo(
                index = p[0].toInt(),
                start = p[1].toLong(),
                end = p[2].toLong(),
                downloaded = p[3].toLong(),
                status = runCatching { ChunkStatus.valueOf(p[4]) }.getOrDefault(ChunkStatus.PENDING)
            )
        }
        ChunkSessionData(url, outputPath, totalBytes, downloadedBytes, chunks, savedAt)
    }.getOrNull()

    fun exists(downloadId: Long): Boolean = fileFor(downloadId).exists()

    fun delete(downloadId: Long) {
        runCatching { fileFor(downloadId).delete() }
    }

    fun cleanOld(maxAgeMillis: Long = 7L * 24 * 60 * 60 * 1000) {
        runCatching {
            val now = System.currentTimeMillis()
            dir.listFiles()?.forEach { f ->
                if (now - f.lastModified() > maxAgeMillis) f.delete()
            }
        }
    }
}