package com.had.downloader.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.had.downloader.data.model.DownloadStatus
import com.had.downloader.data.repository.DownloadDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class TriggerReceiver : BroadcastReceiver() {

    @Inject lateinit var dao: DownloadDao

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val id = intent.getLongExtra("download_id", -1L)
        if (id < 0L) return

        when (action) {
            "com.had.downloader.TRIGGER_DOWNLOAD",
            Intent.ACTION_BOOT_COMPLETED -> {
                scope.launch {
                    val item = dao.getById(id) ?: return@launch
                    if (item.status == DownloadStatus.QUEUED) {
                        dao.update(item.copy(status = DownloadStatus.CONNECTING))
                        context.sendBroadcast(
                            Intent("com.had.downloader.START_QUEUED").apply {
                                putExtra("download_id", id)
                                setPackage(context.packageName)
                            }
                        )
                    }
                }
            }
        }
    }
}