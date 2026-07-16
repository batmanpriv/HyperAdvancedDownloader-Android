package com.had.downloader.service

sealed class DownloadResult {
    object Success : DownloadResult()
    data class Failed(val error: String, val bytesDownloaded: Long = 0) : DownloadResult()
    object Cancelled : DownloadResult()
}