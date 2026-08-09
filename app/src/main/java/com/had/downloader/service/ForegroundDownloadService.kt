package com.had.downloader.service

import android.app.*
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.had.downloader.MainActivity
import com.had.downloader.data.model.DownloadItem
import com.had.downloader.data.model.DownloadMode
import com.had.downloader.data.model.DownloadStatus
import com.had.downloader.data.model.toHumanSize
import com.had.downloader.data.model.toSpeedString
import com.had.downloader.data.repository.DownloadDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

private const val CHANNEL_DOWNLOAD = "had_download"
private const val CHANNEL_COMPLETE = "had_complete"
private const val NOTIF_FOREGROUND = 2001
private const val NOTIF_COMPLETE_BASE = 3000
private const val PREFS_NAME = "had_settings"

const val ACTION_START = "com.had.downloader.START"
const val ACTION_STOP = "com.had.downloader.STOP"
const val ACTION_STOP_ALL = "com.had.downloader.STOP_ALL"
const val ACTION_START_ALL = "com.had.downloader.START_ALL"
const val ACTION_DISMISS_NOTIF = "com.had.downloader.DISMISS_NOTIF"
const val ACTION_CHECK_QUEUE = "com.had.downloader.CHECK_QUEUE"
const val EXTRA_ITEM_ID = "item_id"

@AndroidEntryPoint
class ForegroundDownloadService : Service() {

    @Inject lateinit var dao: DownloadDao
    @Inject lateinit var smartDownloader: SmartDownloader
    @Inject lateinit var hlsDownloader: HlsDownloader
    @Inject lateinit var analyticsRepository: com.had.downloader.data.repository.AnalyticsRepository

    private val hlsSessions = java.util.concurrent.ConcurrentHashMap<Long, HlsDownloader>()

    inner class LocalBinder : Binder() {
        fun getService() = this@ForegroundDownloadService
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(
        Dispatchers.Default + SupervisorJob() + CoroutineExceptionHandler { _, e ->
            
            Log.e("HAD", "Unhandled exception in download service scope: ${e.message}", e)
        }
    )
    private var wakeLock: PowerManager.WakeLock? = null

    private val _activeIds = mutableSetOf<Long>()
    private val activeIdsLock = Any()
    private val stoppedIds = mutableSetOf<Long>()
    private val stoppedLock = Any()

    private var notificationDismissed = false
    private var cachedAllItems: List<DownloadItem> = emptyList()
    private val cacheLock = Mutex()
    private var cachedMaxConcurrent = 2
    private var lastMaxCheck = 0L

    override fun onCreate() {
        super.onCreate()
        createChannels()
        acquireWakeLock()
        startForegroundCompat()
        observeProgress()
        observeResults()
        observeAllItems()
        refreshMaxConcurrent()
        resumeOrphanedDownloads()
        startStuckDownloadChecker()
        startNotificationUpdater()
    }

    private fun resumeOrphanedDownloads() {
        scope.launch {
            val orphaned = dao.getAllSync().filter {
                it.status == DownloadStatus.DOWNLOADING ||
                        it.status == DownloadStatus.CONNECTING ||
                        it.status == DownloadStatus.MERGING
            }
            if (orphaned.isEmpty()) return@launch
            orphaned.forEach { item ->
                dao.update(item.copy(status = DownloadStatus.QUEUED, startedAt = null))
            }
            delay(500) 
            checkAndStartNext()
        }
    }

    private fun startNotificationUpdater() {
        scope.launch {
            while (true) {
                delay(2000L)
                if (!notificationDismissed) {
                    updateForegroundNotif()
                }
            }
        }
    }

    private fun refreshMaxConcurrent() {
        cachedMaxConcurrent = getMaxConcurrentFromPrefs()
        lastMaxCheck = System.currentTimeMillis()
    }

    private fun getMaxConcurrentFromPrefs(): Int {
        return applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("maxConcurrent", 2)
    }

    private fun getMaxConcurrent(): Int {
        if (System.currentTimeMillis() - lastMaxCheck > 5000) {
            refreshMaxConcurrent()
        }
        return cachedMaxConcurrent
    }

    private fun observeAllItems() {
        scope.launch {
            dao.observeAll().collect { list ->
                cacheLock.withLock { cachedAllItems = list }
                checkAndResetDismissState()
            }
        }
    }

    private fun checkAndResetDismissState() {
        val hasActiveOrQueued = cachedAllItems.any {
            it.status == DownloadStatus.DOWNLOADING ||
                    it.status == DownloadStatus.CONNECTING ||
                    it.status == DownloadStatus.MERGING ||
                    it.status == DownloadStatus.QUEUED ||
                    it.status == DownloadStatus.PAUSED
        }

        if (hasActiveOrQueued && notificationDismissed) {
            notificationDismissed = false
        }

        if (!hasActiveOrQueued && !notificationDismissed) {
            
        }
    }

    private suspend fun getAllItemsCached(): List<DownloadItem> =
        cacheLock.withLock { cachedAllItems }

    private fun getActiveIdsCount(): Int {
        synchronized(activeIdsLock) {
            return _activeIds.size
        }
    }

    private fun startStuckDownloadChecker() {
        scope.launch {
            while (true) {
                delay(10_000L)
                try {
                    val allItems = getAllItemsCached()
                    val maxConcurrent = getMaxConcurrent()
                    val activeCount = getActiveIdsCount()

                    val hangingDownloads = allItems.filter { item ->
                        (item.status == DownloadStatus.DOWNLOADING ||
                                item.status == DownloadStatus.CONNECTING ||
                                item.status == DownloadStatus.MERGING) &&
                                item.totalBytes > 0 &&
                                item.downloadedBytes >= item.totalBytes * 0.95f
                    }

                    hangingDownloads.forEach { item ->
                        val file = File(item.outputDir, item.filename)
                        if (file.exists() && file.length() >= item.totalBytes * 0.95f) {
                            handleDownloadComplete(item.id, file.length())
                        }
                    }

                    val availableSlots = maxConcurrent - activeCount

                    if (availableSlots > 0) {
                        
                        val queuedItems = allItems.filter {
                            it.status == DownloadStatus.QUEUED &&
                                    it.scheduleFrom.isBlank()
                        }.sortedWith(compareBy({ it.queuePriority }, { it.createdAt }, { it.id }))
                            .take(availableSlots)

                        queuedItems.forEach { item ->
                            startQueuedItem(item)
                        }
                        updateForegroundNotif()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun startQueuedItem(item: DownloadItem) {
        synchronized(stoppedLock) { stoppedIds.remove(item.id) }
        synchronized(activeIdsLock) {
            if (item.id !in _activeIds) {
                _activeIds.add(item.id)
            }
        }
        scope.launch {
            val updated = item.copy(
                status = DownloadStatus.CONNECTING,
                startedAt = System.currentTimeMillis()
            )
            dao.update(updated)
            
            analyticsRepository.recordStart(updated)
            launchDownload(updated)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val id = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
                if (id >= 0) {
                    notificationDismissed = false
                    scope.launch {
                        val maxConcurrent = getMaxConcurrent()
                        val activeCount = getActiveIdsCount()

                        if (activeCount < maxConcurrent) {
                            val item = dao.getById(id)
                            if (item != null) {
                                startQueuedItem(item)
                            }
                        } else {
                            dao.getById(id)?.let { item ->
                                if (item.status != DownloadStatus.QUEUED &&
                                    item.status != DownloadStatus.COMPLETED &&
                                    item.status != DownloadStatus.CANCELLED &&
                                    item.status != DownloadStatus.FAILED) {
                                    dao.update(item.copy(status = DownloadStatus.QUEUED))
                                }
                            }
                            updateForegroundNotif()
                        }
                    }
                }
            }
            ACTION_STOP -> {
                val id = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
                if (id >= 0) stopItem(id)
            }
            ACTION_STOP_ALL -> {
                stopAllItems()
            }
            ACTION_START_ALL -> {
                scope.launch { startAllQueued() }
            }
            ACTION_DISMISS_NOTIF -> {
                notificationDismissed = true
                NotificationManagerCompat.from(this).cancel(NOTIF_FOREGROUND)
                if (getActiveIdsCount() == 0) {
                    stopSelf()
                }
            }
            ACTION_CHECK_QUEUE -> {
                scope.launch {
                    delay(300)
                    checkAndStartNext()
                }
            }
            else -> {
                startForegroundCompat()
            }
        }
        return START_STICKY
    }

    private suspend fun startAllQueued() {
        val maxConcurrent = getMaxConcurrent()
        val activeCount = getActiveIdsCount()
        val availableSlots = maxConcurrent - activeCount

        if (availableSlots <= 0) {
            return
        }

        val allItems = getAllItemsCached()
        val queued = allItems.filter {
            it.status == DownloadStatus.QUEUED && it.scheduleFrom.isBlank()
        }.sortedWith(compareBy({ it.queuePriority }, { it.createdAt }, { it.id }))
            .take(availableSlots)

        queued.forEach { item ->
            startQueuedItem(item)
        }

        updateForegroundNotif()
    }

    private fun stopItem(id: Long) {
        synchronized(stoppedLock) { stoppedIds.add(id) }
        smartDownloader.cancelDownload(id)
        hlsSessions.remove(id)?.cancel()
        synchronized(activeIdsLock) { _activeIds.remove(id) }
        scope.launch {
            dao.markFailed(id, DownloadStatus.CANCELLED, "Stopped by user")
        }
        updateForegroundNotif()
    }

    private fun stopAllItems() {
        val idsToStop: List<Long>
        synchronized(activeIdsLock) {
            idsToStop = _activeIds.toList()
            _activeIds.clear()
        }
        idsToStop.forEach { id ->
            synchronized(stoppedLock) { stoppedIds.add(id) }
            smartDownloader.cancelDownload(id)
            hlsSessions.remove(id)?.cancel()
            scope.launch {
                dao.markFailed(id, DownloadStatus.CANCELLED, "Stopped by user")
            }
        }
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
            scope.launch {
                dao.update(item.copy(filename = resolvedFilename))
            }
        }

        val downloader = HlsDownloader()
        hlsSessions[item.id] = downloader

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
                downloader.download(
                    m3u8Url = sanitizeUrl(item.url),
                    outputPath = outputPath,
                    config = config
                ) { done, total, pct, status, chunks ->
                    val stopped = synchronized(stoppedLock) { item.id in stoppedIds }
                    if (stopped) {
                        return@download
                    }

                    scope.launch {
                        when {
                            status.startsWith("COMPLETED") -> {
                                val fileSize = status.substringAfter("COMPLETED:", "").toLongOrNull() ?: 0L
                                handleDownloadComplete(item.id, fileSize)
                            }
                            status.startsWith("FAILED") -> {
                                handleDownloadFailed(item.id, status)
                            }
                            status == "CANCELLED" -> {
                                handleDownloadCancelled(item.id)
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
                                    
                                    val speedBps = chunks.sumOf { it.speedBps }
                                    trackSpeed(item.id, speedBps)
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
                val stopped = synchronized(stoppedLock) { item.id in stoppedIds }
                if (!stopped) {
                    handleDownloadFailed(item.id, e.message ?: "Unknown error")
                }
            }
        }
    }

    private data class SpeedStats(var sumBps: Long = 0L, var samples: Int = 0, var peakBps: Long = 0L)
    private val speedTrackers = java.util.concurrent.ConcurrentHashMap<Long, SpeedStats>()
    private var lastSpeedSampleAt = java.util.concurrent.ConcurrentHashMap<Long, Long>()

    private fun trackSpeed(id: Long, speedBps: Long) {
        if (speedBps <= 0L) return
        val stats = speedTrackers.getOrPut(id) { SpeedStats() }
        synchronized(stats) {
            stats.sumBps += speedBps
            stats.samples += 1
            if (speedBps > stats.peakBps) stats.peakBps = speedBps
        }
        
        val now = System.currentTimeMillis()
        val last = lastSpeedSampleAt[id] ?: 0L
        if (now - last >= 2000) {
            lastSpeedSampleAt[id] = now
            scope.launch { analyticsRepository.recordSpeedSample(id, speedBps) }
        }
    }

    private fun consumeSpeedStats(id: Long): Pair<Long, Long> {
        val stats = speedTrackers.remove(id)
        lastSpeedSampleAt.remove(id)
        if (stats == null || stats.samples == 0) return 0L to 0L
        return (stats.sumBps / stats.samples) to stats.peakBps
    }

    private suspend fun handleDownloadComplete(id: Long, fileSize: Long) {
        val now = System.currentTimeMillis()
        val item = dao.getById(id)
        val file = item?.let { File(it.outputDir, it.filename) }
        val actualFileSize = file?.length() ?: 0L
        val sizeToReport = if (actualFileSize > 0) actualFileSize else fileSize

        dao.markCompleted(id, DownloadStatus.COMPLETED, now)
        dao.updateProgress(
            id = id, status = DownloadStatus.COMPLETED,
            progress = 1f,
            downloaded = sizeToReport,
            total = sizeToReport,
            speed = 0L, eta = 0
        )

        dao.getById(id)?.let { updatedItem ->
            if (updatedItem.status != DownloadStatus.COMPLETED) {
                dao.update(updatedItem.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 1f,
                    downloadedBytes = sizeToReport,
                    totalBytes = sizeToReport,
                    completedAt = now
                ))
            }
            showCompleteNotif(updatedItem)

            val (avgBps, peakBps) = consumeSpeedStats(id)
            analyticsRepository.recordComplete(
                downloadId = id,
                totalBytes = sizeToReport,
                avgSpeedBps = avgBps,
                peakSpeedBps = peakBps,
                retries = updatedItem.retryCount,
                success = true
            )
        }

        synchronized(activeIdsLock) { _activeIds.remove(id) }
        synchronized(stoppedLock) { stoppedIds.remove(id) }
        hlsSessions.remove(id)
        updateForegroundNotif()

        delay(500)
        checkAndStartNext()
    }

    private val pendingRetries = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>()

    private suspend fun handleDownloadFailed(id: Long, error: String) {
        synchronized(activeIdsLock) { _activeIds.remove(id) }
        synchronized(stoppedLock) { stoppedIds.remove(id) }
        hlsSessions.remove(id)

        val item = dao.getById(id)
        val nextRetryCount = (item?.retryCount ?: 0) + 1
        val maxRetries = item?.maxRetries ?: 0
        val timeoutSec = (item?.timeoutSec ?: 30).coerceAtLeast(5)

        if (item != null && nextRetryCount <= maxRetries && pendingRetries.add(id)) {
            
            dao.update(
                item.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = "$error - retrying in ${timeoutSec}s (${nextRetryCount}/$maxRetries)",
                    retryCount = nextRetryCount
                )
            )
            updateForegroundNotif()

            scope.launch {
                delay(timeoutSec * 1000L)
                pendingRetries.remove(id)
                val current = dao.getById(id)
                
                if (current != null && current.status == DownloadStatus.FAILED) {
                    dao.update(current.copy(status = DownloadStatus.QUEUED, errorMessage = null))
                    ForegroundDownloadService.checkQueue(applicationContext)
                    checkAndStartNext()
                }
            }
        } else {
            dao.markFailed(id, DownloadStatus.FAILED, error)
            updateForegroundNotif()
            val (avgBps, peakBps) = consumeSpeedStats(id)
            analyticsRepository.recordComplete(
                downloadId = id,
                totalBytes = item?.downloadedBytes ?: 0L,
                avgSpeedBps = avgBps,
                peakSpeedBps = peakBps,
                retries = item?.retryCount ?: 0,
                success = false
            )
        }

        checkAndStartNext()
    }

    private suspend fun handleDownloadCancelled(id: Long) {
        pendingRetries.remove(id)
        consumeSpeedStats(id) 
        dao.markFailed(id, DownloadStatus.CANCELLED, "Cancelled")
        synchronized(activeIdsLock) { _activeIds.remove(id) }
        synchronized(stoppedLock) { stoppedIds.remove(id) }
        hlsSessions.remove(id)
        updateForegroundNotif()
        checkAndStartNext()
    }

    private suspend fun checkAndStartNext() {
        val maxConcurrent = getMaxConcurrent()
        val activeCount = getActiveIdsCount()
        val availableSlots = maxConcurrent - activeCount

        if (availableSlots <= 0) {
            return
        }

        val allItems = getAllItemsCached()

        val queued = allItems.filter {
            it.status == DownloadStatus.QUEUED &&
                    it.scheduleFrom.isBlank() &&
                    !synchronized(activeIdsLock) { it.id in _activeIds }
        }.sortedWith(compareBy({ it.queuePriority }, { it.createdAt }, { it.id }))
            .take(availableSlots)

        queued.forEach { item ->
            startQueuedItem(item)
        }

        updateForegroundNotif()
    }

    private fun observeProgress() {
        scope.launch {
            smartDownloader.progress.collect { (id, prog) ->
                val stopped = synchronized(stoppedLock) { id in stoppedIds }
                if (stopped) {
                    return@collect
                }

                val currentItem = dao.getById(id)
                if (currentItem?.status == DownloadStatus.CANCELLED ||
                    currentItem?.status == DownloadStatus.FAILED) {
                    return@collect
                }

                val fileExists = currentItem?.let { item ->
                    val file = File(item.outputDir, item.filename)
                    file.exists() && file.length() > 0
                } ?: false

                val status = when {
                    prog.status.startsWith("COMPLETED") -> DownloadStatus.COMPLETED
                    prog.status == "CANCELLED" -> DownloadStatus.CANCELLED
                    prog.status.startsWith("FAILED") || prog.status.startsWith("CHECKSUM") -> DownloadStatus.FAILED
                    prog.status == "VERIFYING" -> DownloadStatus.VERIFYING
                    prog.status == "MERGING" -> DownloadStatus.MERGING
                    prog.percent >= 1f && fileExists && prog.totalBytes > 0 -> DownloadStatus.COMPLETED
                    else -> DownloadStatus.DOWNLOADING
                }

                when (status) {
                    DownloadStatus.COMPLETED -> {
                        val fileSize = currentItem?.let {
                            File(it.outputDir, it.filename).length()
                        } ?: 0L
                        val sizeToReport = if (fileSize > 0) fileSize else prog.totalBytes
                        handleDownloadComplete(id, sizeToReport)
                    }
                    DownloadStatus.FAILED -> {
                        handleDownloadFailed(id, prog.status)
                    }
                    DownloadStatus.CANCELLED -> {
                        handleDownloadCancelled(id)
                    }
                    else -> {
                        trackSpeed(id, prog.speedBps)
                        dao.updateProgress(
                            id = id, status = status, progress = prog.percent,
                            downloaded = prog.downloadedBytes, total = prog.totalBytes,
                            speed = prog.speedBps, eta = prog.etaSeconds
                        )
                        synchronized(activeIdsLock) {
                            if (id !in _activeIds && status == DownloadStatus.DOWNLOADING) {
                                _activeIds.add(id)
                            }
                        }
                        updateForegroundNotif()
                    }
                }
            }
        }
    }

    private fun observeResults() {
        scope.launch {
            smartDownloader.result.collect { (id, result) ->
                when (result) {
                    is DownloadResult.Success -> {
                        handleDownloadComplete(id, 0L)
                    }
                    is DownloadResult.Failed -> {
                        handleDownloadFailed(id, result.error)
                    }
                    DownloadResult.Cancelled -> {
                        handleDownloadCancelled(id)
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
        if (notificationDismissed) {
            return
        }
        val notif = buildForegroundNotif()
        try {
            NotificationManagerCompat.from(this).notify(NOTIF_FOREGROUND, notif)
        } catch (_: SecurityException) {}
    }

    private fun buildForegroundNotif(): Notification {
        val allItems = cachedAllItems
        val activeItems = allItems.filter {
            it.status == DownloadStatus.DOWNLOADING ||
                    it.status == DownloadStatus.CONNECTING ||
                    it.status == DownloadStatus.MERGING
        }
        val queuedItems = allItems.filter { it.status == DownloadStatus.QUEUED }
        val scheduledItems = allItems.filter {
            it.status == DownloadStatus.QUEUED && it.scheduleFrom.isNotBlank()
        }
        val pausedItems = allItems.filter { it.status == DownloadStatus.PAUSED }
        val completedItems = allItems.filter { it.status == DownloadStatus.COMPLETED }

        val openPi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        )
        val stopAllPi = PendingIntent.getService(
            this, 99,
            Intent(this, ForegroundDownloadService::class.java).apply { action = ACTION_STOP_ALL },
            FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        )
        val startAllPi = PendingIntent.getService(
            this, 98,
            Intent(this, ForegroundDownloadService::class.java).apply { action = ACTION_START_ALL },
            FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        )
        val dismissNotifPi = PendingIntent.getService(
            this, 97,
            Intent(this, ForegroundDownloadService::class.java).apply { action = ACTION_DISMISS_NOTIF },
            FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_DOWNLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(openPi)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        val hasAnyItem = allItems.isNotEmpty()

        when {
            activeItems.isNotEmpty() -> {
                val totalDownloaded = activeItems.sumOf { it.downloadedBytes }
                val totalSize = activeItems.sumOf { it.totalBytes }
                val avgProgress = activeItems.map { it.progress }.average().toFloat()
                val totalSpeed = activeItems.sumOf { it.speedBps }
                val progressPct = (avgProgress * 100).toInt().coerceIn(0, 100)

                if (activeItems.size == 1) {
                    val item = activeItems[0]
                    val title = item.filename.ifBlank { "Downloading..." }.take(40)
                    val sizeStr = if (item.totalBytes > 0)
                        "${item.downloadedBytes.toHumanSize()} / ${item.totalBytes.toHumanSize()}"
                    else item.downloadedBytes.toHumanSize()
                    val speedStr = if (item.speedBps > 0) "  ⚡${item.speedBps.toSpeedString()}" else ""
                    val etaStr = if (item.etaSeconds > 0) "  ⏱${formatRemainingTime(item.etaSeconds)}" else ""

                    builder.setContentTitle(title)
                    builder.setContentText("$sizeStr$speedStr$etaStr")
                    builder.setProgress(100, progressPct, false)

                    val subText = buildString {
                        if (scheduledItems.isNotEmpty()) append("⏰ ${scheduledItems.size} scheduled")
                        if (queuedItems.isNotEmpty()) {
                            if (isNotEmpty()) append(" • ")
                            append("📋 ${queuedItems.size} queued")
                        }
                        if (pausedItems.isNotEmpty()) {
                            if (isNotEmpty()) append(" • ")
                            append("⏸ ${pausedItems.size} paused")
                        }
                        if (isNotEmpty()) append("  •  $progressPct%")
                        else append("$progressPct%")
                    }
                    builder.setSubText(subText)

                } else {
                    val speedStr = if (totalSpeed > 0) "  ⚡${totalSpeed.toSpeedString()}" else ""
                    val sizeStr = if (totalSize > 0)
                        "${totalDownloaded.toHumanSize()} / ${totalSize.toHumanSize()}"
                    else totalDownloaded.toHumanSize()

                    builder.setContentTitle("HAD — ${activeItems.size} downloads active")
                    builder.setContentText("$sizeStr$speedStr")
                    builder.setProgress(100, progressPct, false)

                    val subText = buildString {
                        if (scheduledItems.isNotEmpty()) append("⏰ ${scheduledItems.size} scheduled")
                        if (queuedItems.isNotEmpty()) {
                            if (isNotEmpty()) append(" • ")
                            append("📋 ${queuedItems.size} queued")
                        }
                        if (pausedItems.isNotEmpty()) {
                            if (isNotEmpty()) append(" • ")
                            append("⏸ ${pausedItems.size} paused")
                        }
                        if (isNotEmpty()) append("  •  $progressPct%")
                        else append("$progressPct%")
                    }
                    builder.setSubText(subText)
                }

                builder.addAction(
                    NotificationCompat.Action.Builder(
                        android.R.drawable.ic_media_pause, "Stop All", stopAllPi
                    ).build()
                )
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissNotifPi
                    ).build()
                )
            }

            scheduledItems.isNotEmpty() -> {
                val now = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                val sorted = scheduledItems.mapNotNull { item ->
                    item.scheduleFrom.toLongOrNull()?.let { epoch -> item to epoch }
                }.filter { it.second > now }.sortedBy { it.second }

                if (sorted.isNotEmpty()) {
                    val (item, epoch) = sorted.first()
                    val remaining = (epoch - now) / 1000
                    val remainingStr = when {
                        remaining <= 0 -> "Starting now..."
                        remaining < 60 -> "${remaining}s"
                        remaining < 3600 -> "${remaining / 60}m ${remaining % 60}s"
                        else -> "${remaining / 3600}h ${(remaining % 3600) / 60}m"
                    }
                    val timeStr = dateFormat.format(Date(epoch))
                    val title = item.filename.take(30)

                    builder.setContentTitle("⏰ Scheduled: $title")
                    builder.setContentText("Starts at $timeStr • $remainingStr left")
                    builder.setProgress(0, 0, true)

                    val subText = buildString {
                        if (sorted.size > 1) append("${sorted.size} items scheduled")
                        if (queuedItems.isNotEmpty()) {
                            if (isNotEmpty()) append(" • ")
                            append("📋 ${queuedItems.size} queued")
                        }
                        if (pausedItems.isNotEmpty()) {
                            if (isNotEmpty()) append(" • ")
                            append("⏸ ${pausedItems.size} paused")
                        }
                        if (isNotEmpty()) append("")
                        else append("${sorted.size} item${if (sorted.size > 1) "s" else ""} waiting")
                    }
                    builder.setSubText(subText)

                } else {
                    builder.setContentTitle("⏰ Scheduled Downloads")
                    builder.setContentText("${scheduledItems.size} items waiting")
                    builder.setProgress(0, 0, true)

                    val subText = buildString {
                        if (queuedItems.isNotEmpty()) append("📋 ${queuedItems.size} queued")
                        if (pausedItems.isNotEmpty()) {
                            if (isNotEmpty()) append(" • ")
                            append("⏸ ${pausedItems.size} paused")
                        }
                        if (isNotEmpty()) append("")
                        else append("${scheduledItems.size} item${if (scheduledItems.size > 1) "s" else ""} scheduled")
                    }
                    builder.setSubText(subText)
                }

                builder.addAction(
                    NotificationCompat.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissNotifPi
                    ).build()
                )
            }

            queuedItems.isNotEmpty() -> {
                val count = queuedItems.size
                val fileNames = queuedItems.take(3).joinToString(", ") { it.filename.take(20) }
                val moreText = if (count > 3) " and ${count - 3} more" else ""

                builder.setContentTitle("📋 ${count} Downloads Queued")
                builder.setContentText("$fileNames$moreText")
                builder.setProgress(0, 0, false)

                val subText = buildString {
                    if (pausedItems.isNotEmpty()) {
                        append("⏸ ${pausedItems.size} paused")
                        if (count > 0) append(" • ")
                    }
                    append("Tap Start All to begin")
                }
                builder.setSubText(subText)

                builder.addAction(
                    NotificationCompat.Action.Builder(
                        android.R.drawable.ic_media_play, "Start All", startAllPi
                    ).build()
                )
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissNotifPi
                    ).build()
                )
            }

            pausedItems.isNotEmpty() -> {
                val count = pausedItems.size
                val fileNames = pausedItems.take(3).joinToString(", ") { it.filename.take(20) }
                val moreText = if (count > 3) " and ${count - 3} more" else ""

                builder.setContentTitle("⏸ ${count} Downloads Paused")
                builder.setContentText("$fileNames$moreText")
                builder.setProgress(0, 0, false)
                builder.setSubText("Waiting for schedule window")

                builder.addAction(
                    NotificationCompat.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissNotifPi
                    ).build()
                )
            }

            else -> {
                val totalSaved = completedItems.sumOf { it.downloadedBytes }
                val completedCount = completedItems.size

                builder.setContentTitle("HAD Downloader")
                builder.setContentText(
                    if (completedCount > 0) "$completedCount completed · ${totalSaved.toHumanSize()} saved"
                    else "No downloads"
                )
                builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                builder.setOngoing(false)
                builder.setSubText("Tap + to add a download")
                builder.setProgress(0, 0, false)

                builder.addAction(
                    NotificationCompat.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissNotifPi
                    ).build()
                )
            }
        }

        return builder.build()
    }

    private fun formatRemainingTime(seconds: Int): String {
        return when {
            seconds < 0 -> "--"
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> {
                val minutes = seconds / 60
                val secs = seconds % 60
                if (secs > 0) "${minutes}m ${secs}s" else "${minutes}m"
            }
            seconds < 86400 -> {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
            }
            else -> {
                val days = seconds / 86400
                val hours = (seconds % 86400) / 3600
                "${days}d ${hours}h"
            }
        }
    }

    private fun showCompleteNotif(item: DownloadItem) {
        val pi = PendingIntent.getActivity(
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

            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOAD,
                "Active Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows while downloads are running"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(downloadChannel)

            val completeChannel = NotificationChannel(
                CHANNEL_COMPLETE,
                "Download Complete",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when a download finishes"
                setShowBadge(true)
                enableVibration(true)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(completeChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        scope.cancel()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (getActiveIdsCount() > 0) {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        fun stopDownload(ctx: Context, itemId: Long) {
            ctx.startService(Intent(ctx, ForegroundDownloadService::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_ITEM_ID, itemId)
            })
        }

        fun ensureRunning(ctx: Context) {
            val intent = Intent(ctx, ForegroundDownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        fun checkQueue(ctx: Context) {
            val intent = Intent(ctx, ForegroundDownloadService::class.java).apply {
                action = ACTION_CHECK_QUEUE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }
    }
}