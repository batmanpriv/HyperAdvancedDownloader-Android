package com.had.downloader.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.had.downloader.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

private const val CHANNEL_ID   = "had_scheduler"
private const val NOTIF_ID     = 1001
private const val EXTRA_ID     = "download_id"
private const val EXTRA_EPOCH  = "trigger_epoch"

@AndroidEntryPoint
class DownloadSchedulerService : Service() {

    @Inject lateinit var smartDownloader: SmartDownloader
    @Inject lateinit var hlsDownloader: HlsDownloader

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val waitJobs = mutableMapOf<Long, Job>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getLongExtra(EXTRA_ID, -1L) ?: -1L
        val epoch = intent?.getLongExtra(EXTRA_EPOCH, 0L) ?: 0L
        if (id < 0) return START_NOT_STICKY

        startForeground(NOTIF_ID, buildNotification("Scheduler running"))

        val delay = (epoch - System.currentTimeMillis()).coerceAtLeast(0L)
        waitJobs[id] = scope.launch {
            delay(delay)
            
            val triggerIntent = Intent("com.had.downloader.TRIGGER_DOWNLOAD").apply {
                putExtra(EXTRA_ID, id)
                setPackage(packageName)
            }
            
            sendOrderedBroadcast(triggerIntent, null)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Download Scheduler",
                NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("HAD Scheduler")
            .setContentText(text)
            .setContentIntent(pi)
            .build()
    }

    companion object {
        fun schedule(ctx: Context, downloadId: Long, triggerEpoch: Long) {
            val intent = Intent(ctx, DownloadSchedulerService::class.java).apply {
                putExtra(EXTRA_ID, downloadId)
                putExtra(EXTRA_EPOCH, triggerEpoch)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ctx.startForegroundService(intent)
            else
                ctx.startService(intent)
        }

        fun cancel(ctx: Context) {
            ctx.stopService(Intent(ctx, DownloadSchedulerService::class.java))
        }
    }
}