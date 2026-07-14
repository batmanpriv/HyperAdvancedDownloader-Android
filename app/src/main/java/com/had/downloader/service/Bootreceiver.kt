package com.had.downloader.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.had.downloader.data.model.DownloadStatus
import com.had.downloader.data.repository.DownloadDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

private const val TAG = "BootReceiver"

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var dao: DownloadDao

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive: action=$action")

        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.LOCKED_BOOT_COMPLETED" &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val allItems = dao.getAllSync()
                Log.d(TAG, "Total items in DB: ${allItems.size}")

                val toResume = allItems.filter { item ->
                    item.status == DownloadStatus.DOWNLOADING ||
                            item.status == DownloadStatus.CONNECTING ||
                            item.status == DownloadStatus.MERGING
                }

                Log.d(TAG, "Items to resume: ${toResume.size}")

                toResume.forEach { item ->
                    dao.markFailed(item.id, DownloadStatus.QUEUED, null)
                    ForegroundDownloadService.startDownload(context, item.id)
                    Log.d(TAG, "Resuming download id=${item.id}")
                }

                val scheduledItems = allItems.filter { item ->
                    item.status == DownloadStatus.QUEUED &&
                            item.scheduleFrom.isNotBlank() &&
                            (item.scheduleFrom.toLongOrNull() ?: 0L) > System.currentTimeMillis()
                }

                Log.d(TAG, "Scheduled items to re-alarm: ${scheduledItems.size}")

                scheduledItems.forEach { item ->
                    val epoch = item.scheduleFrom.toLongOrNull() ?: return@forEach
                    Scheduler.schedule(context, item.id, epoch)
                    Log.d(TAG, "Re-scheduled alarm for id=${item.id} at $epoch")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in BootReceiver: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}