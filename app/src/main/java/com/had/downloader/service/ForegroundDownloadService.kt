package com.had.downloader.service

import android.app.*
import android.app.PendingIntent.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.had.downloader.MainActivity
import com.had.downloader.data.model.*
import com.had.downloader.data.repository.DownloadDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val CHANNEL_DOWNLOAD = "had_download"
private const val CHANNEL_COMPLETE = "had_complete"
private const val NOTIF_FOREGROUND = 2001
private const val NOTIF_COMPLETE_BASE = 3000

const val ACTION_START = "com.had.downloader.START"
const val ACTION_STOP = "com.had.downloader.STOP"
const val ACTION_STOP_ALL = "com.had.downloader.STOP_ALL"
const val ACTION_START_ALL = "com.had.downloader.START_ALL"
const val ACTION_CLOSE_NOTIF = "com.had.downloader.CLOSE_NOTIF"
const val EXTRA_ITEM_ID = "item_id"

@AndroidEntryPoint
class ForegroundDownloadService : Service() {

    @Inject lateinit var dao: DownloadDao
    @Inject lateinit var smartDownloader: SmartDownloader
    @Inject lateinit var hlsDownloader: HlsDownloader

    inner class LocalBinder : Binder() {
        fun getService() = this@ForegroundDownloadService
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private val activeIds = mutableSetOf<Long>()
    private val stoppedIds = mutableSetOf<Long>()
    private var isNotifDismissed = false

    private var cachedAllItems: List<DownloadItem> = emptyList()
    private val cacheLock = Mutex()

    override fun onCreate() {
        super.onCreate()
        createChannels()
        acquireWakeLock()
        startForegroundCompat()
        observeProgress()
        observeAllItems()
    }

    private fun observeAllItems() {
        scope.launch {
            dao.observeAll().collect { list ->
                cacheLock.withLock { cachedAllItems = list }
            }
        }
    }

    private suspend fun getAllItemsCached(): List<DownloadItem> =
        cacheLock.withLock { cachedAllItems }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val id = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
                if (id >= 0) {
                    synchronized(stoppedIds) { stoppedIds.remove(id) }
                    isNotifDismissed = false
                    startForegroundCompat()
                    scope.launch { startItem(id) }
                }
            }
            ACTION_STOP -> {
                val id = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
                if (id >= 0) stopItem(id)
            }
            ACTION_STOP_ALL -> stopAllItems()
            ACTION_START_ALL -> scope.launch { startAllQueued() }
            ACTION_CLOSE_NOTIF -> {
                isNotifDismissed = true
                try { NotificationManagerCompat.from(this).cancel(NOTIF_FOREGROUND) } catch (_: Exception) {}
                if (activeIds.isEmpty()) stopSelf()
            }
            else -> startForegroundCompat()
        }
        return START_STICKY
    }

    private suspend fun startAllQueued() {
        val queued = getAllItemsCached().filter {
            it.status == DownloadStatus.QUEUED && it.scheduleFrom.isBlank()
        }
        queued.forEach { item ->
            synchronized(stoppedIds) { stoppedIds.remove(item.id) }
            activeIds.add(item.id)
            val updated = item.copy(status = DownloadStatus.CONNECTING)
            dao.update(updated)
            launchDownload(updated)
        }
        isNotifDismissed = false
        updateForegroundNotif()
    }

    private suspend fun startItem(id: Long) {
        synchronized(stoppedIds) { stoppedIds.remove(id) }
        val item = dao.getById(id) ?: return

        val readyItem = when (item.status) {
            DownloadStatus.CANCELLED,
            DownloadStatus.FAILED,
            DownloadStatus.QUEUED -> item.copy(
                status = DownloadStatus.CONNECTING,
                errorMessage = null,
                scheduleFrom = ""
            )
            DownloadStatus.CONNECTING,
            DownloadStatus.DOWNLOADING -> item
            else -> item.copy(status = DownloadStatus.CONNECTING, errorMessage = null)
        }

        if (id in activeIds) return

        activeIds.add(id)
        dao.update(readyItem)
        isNotifDismissed = false
        updateForegroundNotif()
        launchDownload(readyItem)
    }

    private fun stopItem(id: Long) {
        synchronized(stoppedIds) { stoppedIds.add(id) }
        smartDownloader.cancelDownload(id)
        activeIds.remove(id)
        scope.launch { dao.markFailed(id, DownloadStatus.CANCELLED, "Stopped by user") }
        updateForegroundNotif()
    }

    private fun stopAllItems() {
        activeIds.toList().forEach { id ->
            synchronized(stoppedIds) { stoppedIds.add(id) }
            smartDownloader.cancelDownload(id)
            scope.launch { dao.markFailed(id, DownloadStatus.CANCELLED, "Stopped by user") }
        }
        activeIds.clear()
        updateForegroundNotif()
    }

    private fun launchDownload(item: DownloadItem) {
        when (item.mode) {
            DownloadMode.HLS -> launchHls(item)
            else -> launchSmart(item)
        }
    }

    private fun launchSmart(item: DownloadItem) {
        val config = DownloadConfig(
            url = sanitizeUrl(item.url),
            outputPath = "${item.outputDir}/${item.filename}",
            threads = item.threads,
            proxy = item.proxy,
            headers = parseHeaders(item.customHeaders),
            cookies = item.cookies.ifBlank { null },
            userAgent = item.userAgent,
            method = item.httpMethod,
            useResume = item.useResume,
            maxRetries = item.maxRetries,
            timeoutMs = item.timeoutSec * 1000,
            mirrors = item.mirrors.lines().filter { it.isNotBlank() }.map { sanitizeUrl(it) },
            checksumAlgo = item.checksumAlgo.ifBlank { null },
            checksumExpected = item.checksumExpected.ifBlank { null },
            maxSpeedBps = item.maxSpeedBps
        )
        smartDownloader.startDownload(item.id, config)
    }

    private fun launchHls(item: DownloadItem) {
        val resolvedFilename = hlsDownloader.resolveHlsFilename(item.filename)
        val outputPath = "${item.outputDir}/$resolvedFilename"

        if (resolvedFilename != item.filename) {
            scope.launch { dao.update(item.copy(filename = resolvedFilename)) }
        }

        val config = DownloadConfig(
            url = sanitizeUrl(item.url),
            outputPath = outputPath,
            threads = item.threads,
            headers = parseHeaders(item.customHeaders),
            cookies = item.cookies.ifBlank { null },
            userAgent = item.userAgent,
            timeoutMs = item.timeoutSec * 1000
        )

        scope.launch {
            runCatching {
                hlsDownloader.download(
                    m3u8Url = sanitizeUrl(item.url),
                    outputPath = outputPath,
                    config = config
                ) { done, total, pct, status, chunks ->
                    val stopped = synchronized(stoppedIds) { item.id in stoppedIds }
                    if (stopped) return@download

                    scope.launch {
                        when {
                            status.startsWith("COMPLETED") -> {
                                val fileSize = status.substringAfter("COMPLETED:", "").toLongOrNull() ?: 0L
                                dao.markCompleted(item.id, DownloadStatus.COMPLETED, System.currentTimeMillis())
                                if (fileSize > 0) {
                                    dao.getById(item.id)?.let { current ->
                                        dao.update(
                                            current.copy(
                                                status = DownloadStatus.COMPLETED,
                                                progress = 1f,
                                                downloadedBytes = fileSize,
                                                totalBytes = fileSize,
                                                speedBps = 0L,
                                                etaSeconds = 0,
                                                hlsSegmentsDone = total,
                                                hlsSegmentCount = total
                                            )
                                        )
                                    }
                                }
                                dao.getById(item.id)?.let { showCompleteNotif(it) }
                                activeIds.remove(item.id)
                                updateForegroundNotif()
                                checkAndStop()
                            }

                            status.startsWith("FAILED") -> {
                                dao.markFailed(item.id, DownloadStatus.FAILED, status)
                                activeIds.remove(item.id)
                                updateForegroundNotif()
                                checkAndStop()
                            }

                            status == "CANCELLED" -> {
                                dao.markFailed(item.id, DownloadStatus.CANCELLED, "Cancelled")
                                activeIds.remove(item.id)
                                updateForegroundNotif()
                                checkAndStop()
                            }

                            status == "PARSING" || status == "CONNECTING" -> {
                                dao.updateProgress(
                                    id = item.id,
                                    status = DownloadStatus.CONNECTING,
                                    progress = 0f,
                                    downloaded = 0L,
                                    total = 0L,
                                    speed = 0L,
                                    eta = -1
                                )
                                updateForegroundNotif()
                            }

                            status.startsWith("RESUMING") -> {
                                val estimatedTotal = status.substringAfter("RESUMING:", "").toLongOrNull() ?: 0L
                                dao.getById(item.id)?.let { current ->
                                    dao.update(
                                        current.copy(
                                            status = DownloadStatus.CONNECTING,
                                            progress = pct,
                                            downloadedBytes = done.toLong(),
                                            totalBytes = if (estimatedTotal > 0) estimatedTotal else current.totalBytes,
                                            hlsSegmentsDone = done,
                                            hlsSegmentCount = total
                                        )
                                    )
                                }
                                updateForegroundNotif()
                            }

                            status == "MERGING" || status.startsWith("MERGING:") -> {
                                val estimatedTotal = status.substringAfter("MERGING:", "").toLongOrNull()
                                dao.getById(item.id)?.let { current ->
                                    dao.update(
                                        current.copy(
                                            status = DownloadStatus.MERGING,
                                            progress = pct,
                                            downloadedBytes = done.toLong(),
                                            totalBytes = estimatedTotal ?: current.totalBytes,
                                            speedBps = 0L,
                                            etaSeconds = -1,
                                            hlsSegmentsDone = total,
                                            hlsSegmentCount = total
                                        )
                                    )
                                }
                                updateForegroundNotif()
                            }

                            status == "CONVERTING" || status.startsWith("CONVERTING:") -> {
                                val estimatedTotal = status.substringAfter("CONVERTING:", "").toLongOrNull()
                                dao.getById(item.id)?.let { current ->
                                    dao.update(
                                        current.copy(
                                            status = DownloadStatus.MERGING,
                                            progress = pct,
                                            downloadedBytes = done.toLong(),
                                            totalBytes = estimatedTotal ?: current.totalBytes,
                                            speedBps = 0L,
                                            etaSeconds = -1,
                                            hlsSegmentsDone = total,
                                            hlsSegmentCount = total
                                        )
                                    )
                                }
                                updateForegroundNotif()
                            }

                            status.startsWith("DOWNLOADING:") -> {
                                val estimatedTotal = status.substringAfter("DOWNLOADING:").toLongOrNull()
                                dao.getById(item.id)?.let { current ->
                                    val speedBps = if (chunks.isNotEmpty()) chunks.firstOrNull()?.speedBps ?: 0L else 0L
                                    dao.update(
                                        current.copy(
                                            status = DownloadStatus.DOWNLOADING,
                                            progress = pct,
                                            downloadedBytes = done.toLong(),
                                            totalBytes = estimatedTotal ?: current.totalBytes,
                                            speedBps = speedBps,
                                            etaSeconds = if (speedBps > 0 && estimatedTotal != null && estimatedTotal > done)
                                                ((estimatedTotal - done) / speedBps).toInt() else -1,
                                            hlsSegmentsDone = done,
                                            hlsSegmentCount = total
                                        )
                                    )
                                }
                                updateForegroundNotif()
                            }

                            else -> {
                                dao.updateProgress(
                                    id = item.id,
                                    status = DownloadStatus.DOWNLOADING,
                                    progress = pct,
                                    downloaded = done.toLong(),
                                    total = total.toLong(),
                                    speed = 0L,
                                    eta = -1
                                )
                                updateForegroundNotif()
                            }
                        }
                    }
                }
            }.onFailure { e ->
                val stopped = synchronized(stoppedIds) { item.id in stoppedIds }
                if (!stopped) {
                    dao.markFailed(item.id, DownloadStatus.FAILED, e.message)
                    activeIds.remove(item.id)
                    updateForegroundNotif()
                    checkAndStop()
                }
            }
        }
    }

    private suspend fun checkAndStop() {
        val hasQueued = getAllItemsCached().any { it.status == DownloadStatus.QUEUED }
        if (!hasQueued && activeIds.isEmpty()) stopSelf()
    }

    private fun observeProgress() {
        scope.launch {
            smartDownloader.progress.collect { (id, prog) ->
                val stopped = synchronized(stoppedIds) { id in stoppedIds }
                if (stopped) return@collect

                val currentItem = dao.getById(id)
                if (currentItem?.status == DownloadStatus.CANCELLED ||
                    currentItem?.status == DownloadStatus.FAILED) return@collect

                val status = when {
                    prog.status.startsWith("COMPLETED") -> DownloadStatus.COMPLETED
                    prog.status == "CANCELLED" -> DownloadStatus.CANCELLED
                    prog.status.startsWith("FAILED") || prog.status.startsWith("CHECKSUM") -> DownloadStatus.FAILED
                    prog.status == "VERIFYING" -> DownloadStatus.VERIFYING
                    prog.status == "MERGING" -> DownloadStatus.MERGING
                    else -> DownloadStatus.DOWNLOADING
                }

                when (status) {
                    DownloadStatus.COMPLETED -> {
                        dao.markCompleted(id, DownloadStatus.COMPLETED, System.currentTimeMillis())
                        dao.updateProgress(
                            id = id, status = DownloadStatus.COMPLETED,
                            progress = 1f, downloaded = prog.totalBytes,
                            total = prog.totalBytes, speed = 0L, eta = 0
                        )
                        val item = dao.getById(id)
                        if (item != null) showCompleteNotif(item)
                        activeIds.remove(id)
                        synchronized(stoppedIds) { stoppedIds.remove(id) }
                        updateForegroundNotif()
                        checkAndStop()
                    }
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                        val err = if (status == DownloadStatus.CANCELLED) "Stopped by user" else prog.status
                        dao.markFailed(id, status, err)
                        activeIds.remove(id)
                        synchronized(stoppedIds) { stoppedIds.remove(id) }
                        updateForegroundNotif()
                        checkAndStop()
                    }
                    else -> {
                        dao.updateProgress(
                            id = id, status = status, progress = prog.percent,
                            downloaded = prog.downloadedBytes, total = prog.totalBytes,
                            speed = prog.speedBps, eta = prog.etaSeconds
                        )
                        updateForegroundNotif()
                    }
                }
            }
        }
    }

    private fun startForegroundCompat() {
        val notif = buildForegroundNotif()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_FOREGROUND, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_FOREGROUND, notif)
        }
    }

    fun updateForegroundNotif() {
        if (isNotifDismissed && activeIds.isEmpty()) return
        val notif = buildForegroundNotif()
        try {
            NotificationManagerCompat.from(this).notify(NOTIF_FOREGROUND, notif)
        } catch (_: SecurityException) {}
    }

    private fun buildForegroundNotif(): Notification {
        val activeCount = activeIds.size
        val allItems = cachedAllItems
        val queuedItems = allItems.filter { it.status == DownloadStatus.QUEUED }
        val scheduledItems = queuedItems.filter { it.scheduleFrom.isNotBlank() }

        val openPi = getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        )
        val stopAllPi = getService(
            this, 99,
            Intent(this, ForegroundDownloadService::class.java).apply { action = ACTION_STOP_ALL },
            FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        )
        val startAllPi = getService(
            this, 98,
            Intent(this, ForegroundDownloadService::class.java).apply { action = ACTION_START_ALL },
            FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        )
        val closePi = getService(
            this, 97,
            Intent(this, ForegroundDownloadService::class.java).apply { action = ACTION_CLOSE_NOTIF },
            FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_DOWNLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(openPi)
            .setOngoing(activeCount > 0 || scheduledItems.isNotEmpty())
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        if (activeCount > 0) {
            val activeItems = allItems.filter { activeIds.contains(it.id) }
            val totalDownloaded = activeItems.sumOf { it.downloadedBytes }
            val totalSize = activeItems.sumOf { it.totalBytes }
            val avgProgress = if (activeItems.isNotEmpty())
                activeItems.map { it.progress }.average().toFloat() else 0f
            val totalSpeed = activeItems.sumOf { it.speedBps }
            val progressPct = (avgProgress * 100).toInt().coerceIn(0, 100)

            if (activeItems.size == 1) {
                val item = activeItems[0]
                val title = item.filename.ifBlank { "Downloading..." }.take(40)
                val sizeStr = if (item.totalBytes > 0)
                    "${item.downloadedBytes.toHumanSize()} / ${item.totalBytes.toHumanSize()}"
                else item.downloadedBytes.toHumanSize()
                val speedStr = if (item.speedBps > 0) "  ⚡${item.speedBps.toSpeedString()}" else ""
                val etaStr = if (item.etaSeconds > 0) "  ⏱${item.etaSeconds.toEtaString()}" else ""

                builder.setContentTitle(title)
                builder.setContentText("$sizeStr$speedStr$etaStr")
                builder.setProgress(100, progressPct, false)
                builder.setSubText("$progressPct%")
            } else {
                val speedStr = if (totalSpeed > 0) "  ⚡${totalSpeed.toSpeedString()}" else ""
                val sizeStr = if (totalSize > 0)
                    "${totalDownloaded.toHumanSize()} / ${totalSize.toHumanSize()}"
                else totalDownloaded.toHumanSize()

                builder.setContentTitle("HAD — $activeCount downloads active")
                builder.setContentText("$sizeStr$speedStr")
                builder.setProgress(100, progressPct, false)
                builder.setSubText("$progressPct% avg")
            }

            builder.addAction(NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause, "Stop All", stopAllPi
            ).build())
            builder.addAction(NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel, "Close", closePi
            ).build())

        } else if (scheduledItems.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val sorted = scheduledItems.mapNotNull { item ->
                item.scheduleFrom.toLongOrNull()?.let { epoch -> item to epoch }
            }.filter { it.second > now }.sortedBy { it.second }

            val nearest = sorted.firstOrNull()
            if (nearest != null) {
                val (item, epoch) = nearest
                val remaining = (epoch - now) / 1000
                val remainingStr = when {
                    remaining <= 0 -> "Starting now..."
                    remaining < 60 -> "${remaining}s"
                    remaining < 3600 -> "${remaining / 60}m ${remaining % 60}s"
                    else -> "${remaining / 3600}h ${(remaining % 3600) / 60}m"
                }
                builder.setContentTitle("⏰ ${sorted.size} download${if (sorted.size > 1) "s" else ""} scheduled")
                builder.setContentText("${item.filename.take(25)} at ${sdf.format(Date(epoch))}")
                builder.setSubText("Starts in $remainingStr")
            } else {
                builder.setContentTitle("HAD — ${scheduledItems.size} downloads pending")
                builder.setContentText("Waiting to start...")
            }
            builder.addAction(NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel, "Close", closePi
            ).build())

        } else if (queuedItems.isNotEmpty()) {
            builder.setContentTitle("HAD — ${queuedItems.size} queued")
            builder.setContentText("Tap Start All to begin downloading")
            builder.addAction(NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play, "Start All", startAllPi
            ).build())
            builder.addAction(NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel, "Close", closePi
            ).build())
        } else {
            val completedCount = allItems.count { it.status == DownloadStatus.COMPLETED }
            val totalSaved = allItems.filter { it.status == DownloadStatus.COMPLETED }.sumOf { it.downloadedBytes }
            builder.setContentTitle("HAD — Ready")
            builder.setContentText(
                if (completedCount > 0) "$completedCount completed · ${totalSaved.toHumanSize()} saved"
                else "Tap + to add a download"
            )
            builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
            builder.setOngoing(false)
            builder.addAction(NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel, "Close", closePi
            ).build())
        }

        return builder.build()
    }

    private fun showCompleteNotif(item: DownloadItem) {
        val pi = getActivity(
            this, item.id.toInt(),
            Intent(this, MainActivity::class.java),
            FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        )
        val fileSize = if (item.downloadedBytes > 0) " · ${item.downloadedBytes.toHumanSize()}" else ""
        val notif = NotificationCompat.Builder(this, CHANNEL_COMPLETE)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("✓ Download complete")
            .setContentText(item.filename.ifBlank { item.url.substringAfterLast('/') })
            .setSubText("Saved$fileSize")
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(NOTIF_COMPLETE_BASE + item.id.toInt(), notif)
        } catch (_: SecurityException) {}
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HAD::DownloadWakeLock").apply {
            setReferenceCounted(false)
            acquire(12 * 60 * 60 * 1000L)
        }
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_DOWNLOAD, "Active Downloads", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Shows while downloads are running"
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_COMPLETE, "Download Complete", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Notifies when a download finishes"
                    setShowBadge(true)
                }
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        scope.cancel()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (activeIds.isNotEmpty()) {
            val restartIntent = Intent(applicationContext, ForegroundDownloadService::class.java)
            restartIntent.setPackage(packageName)
            val pi = PendingIntent.getService(
                applicationContext, 1, restartIntent,
                PendingIntent.FLAG_ONE_SHOT or FLAG_IMMUTABLE
            )
            val am = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000L, pi)
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun parseHeaders(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.lines().filter { it.contains(':') }.associate { line ->
            val idx = line.indexOf(':')
            line.substring(0, idx).trim() to line.substring(idx + 1).trim()
        }
    }

    companion object {
        fun startDownload(ctx: Context, itemId: Long) {
            val intent = Intent(ctx, ForegroundDownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ITEM_ID, itemId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ctx.startForegroundService(intent)
            else ctx.startService(intent)
        }

        fun stopDownload(ctx: Context, itemId: Long) {
            ctx.startService(Intent(ctx, ForegroundDownloadService::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_ITEM_ID, itemId)
            })
        }

        fun ensureRunning(ctx: Context) {
            val intent = Intent(ctx, ForegroundDownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ctx.startForegroundService(intent)
            else ctx.startService(intent)
        }
    }
}