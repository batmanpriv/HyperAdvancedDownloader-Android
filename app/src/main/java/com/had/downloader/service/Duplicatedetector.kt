package com.had.downloader.service

import android.util.Log
import com.had.downloader.data.model.DownloadItem
import com.had.downloader.data.model.DownloadStatus
import com.had.downloader.data.repository.DownloadDao
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DuplicateDetector"

enum class DuplicateAction {
    OVERWRITE,
    RENAME,
    SKIP,
    PROCEED   
}

data class DuplicateResult(
    val isDuplicate: Boolean,
    val existingFilePath: String? = null,
    val existingDownloadId: Long? = null,
    val suggestedNewName: String? = null,
    val existingFileSize: Long = 0L,
    val existingCompletedAt: Long? = null
)

@Singleton
class DuplicateDetector @Inject constructor(
    private val dao: DownloadDao
) {

    suspend fun check(
        url: String,
        filename: String,
        outputDir: String
    ): DuplicateResult {
        
        val targetFile = File(outputDir, filename)
        if (targetFile.exists() && targetFile.length() > 0) {
            Log.d(TAG, "File exists on disk: ${targetFile.absolutePath}")
            return DuplicateResult(
                isDuplicate = true,
                existingFilePath = targetFile.absolutePath,
                existingFileSize = targetFile.length(),
                suggestedNewName = generateNewName(filename, outputDir)
            )
        }

        val allCompleted = dao.getAllSync().filter {
            it.status == DownloadStatus.COMPLETED &&
                    (it.url == url || normalizeFilename(it.filename) == normalizeFilename(filename))
        }
        if (allCompleted.isNotEmpty()) {
            val existing = allCompleted.first()
            val existingFile = File(existing.outputDir, existing.filename)
            return DuplicateResult(
                isDuplicate = true,
                existingFilePath = if (existingFile.exists()) existingFile.absolutePath else null,
                existingDownloadId = existing.id,
                existingFileSize = existing.downloadedBytes,
                existingCompletedAt = existing.completedAt,
                suggestedNewName = generateNewName(filename, outputDir)
            )
        }

        return DuplicateResult(isDuplicate = false)
    }

    fun generateNewName(filename: String, outputDir: String): String {
        val ext = if (filename.contains('.')) ".${filename.substringAfterLast('.')}" else ""
        val base = if (ext.isNotEmpty()) filename.substringBeforeLast('.') else filename
        var counter = 1
        var candidate = "$base ($counter)$ext"
        while (File(outputDir, candidate).exists()) {
            counter++
            candidate = "$base ($counter)$ext"
        }
        return candidate
    }

    fun applyAction(
        action: DuplicateAction,
        filename: String,
        outputDir: String,
        duplicateResult: DuplicateResult
    ): String? {
        return when (action) {
            DuplicateAction.OVERWRITE -> {
                
                duplicateResult.existingFilePath?.let { File(it).delete() }
                filename
            }
            DuplicateAction.RENAME -> {
                duplicateResult.suggestedNewName ?: generateNewName(filename, outputDir)
            }
            DuplicateAction.SKIP -> null
            DuplicateAction.PROCEED -> filename
        }
    }

    private fun normalizeFilename(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9.]"), "")
}