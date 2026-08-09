package com.had.downloader.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.had.downloader.data.model.DownloadStatus
import com.had.downloader.data.repository.DownloadDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "DownloadAlarmReceiver"

@AndroidEntryPoint
class DownloadAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var dao: DownloadDao

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${intent.action}")

        if (intent.action != "com.had.downloader.ALARM_TRIGGER") return

        val id = intent.getLongExtra("download_id", -1L)
        if (id < 0) {
            Log.w(TAG, "Invalid download id in alarm")
            return
        }

        Log.d(TAG, "Alarm triggered for download id=$id")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val item = dao.getById(id)
                if (item == null) {
                    Log.w(TAG, "Download id=$id not found in DB")
                    pendingResult.finish()
                    return@launch
                }

                Log.d(TAG, "Download status=${item.status}, scheduleFrom=${item.scheduleFrom}")

                if (item.status != DownloadStatus.QUEUED &&
                    item.status != DownloadStatus.FAILED &&
                    item.status != DownloadStatus.CANCELLED &&
                    item.status != DownloadStatus.PAUSED
                ) {
                    Log.d(TAG, "Download id=$id is not in a startable state: ${item.status}")
                    pendingResult.finish()
                    return@launch
                }

                dao.update(
                    item.copy(
                        scheduleFrom = "",
                        scheduleTo = "",
                        status = DownloadStatus.QUEUED,
                        errorMessage = null
                    )
                )

                Scheduler.cancel(context, id)

                ForegroundDownloadService.startDownload(context, id)

                Log.d(TAG, "Started download id=$id via alarm")

            } catch (e: Exception) {
                Log.e(TAG, "Error in alarm receiver for id=$id: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}