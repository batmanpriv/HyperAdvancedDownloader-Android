package com.had.downloader.ui.screens

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.had.downloader.data.model.*
import com.had.downloader.data.repository.AnalyticsRepository
import com.had.downloader.data.repository.DownloadDao
import com.had.downloader.service.*
import com.had.downloader.ui.components.ThreadViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class UiState(
    val downloads: List<DownloadItem> = emptyList(),
    val logs: Map<Long, List<LogEntry>> = emptyMap(),
    val chunkStates: Map<Long, LiveChunkState> = emptyMap(),
    val activeTab: Int = 0,
    val showAddDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showScraperDialog: Boolean = false,
    val stats: DownloadStats = DownloadStats(),
    val scraperResult: ScraperResult = ScraperResult(""),
    val showDuplicateDialog: Boolean = false,
    val duplicateResult: DuplicateResult? = null,
    val pendingDuplicateItem: DownloadItem? = null,
    val showClipboardDialog: Boolean = false,
    val clipboardUrls: List<String> = emptyList(),
    val selectedDownloadIds: Set<Long> = emptySet(),
    val selectionMode: Boolean = false
)

data class NewDownloadForm(
    val url: String = "",
    val threads: String = "4",
    val outputDir: String = Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DOWNLOADS
    ).absolutePath + "/HAD",
    val proxy: String = "",
    val verbose: Boolean = false,
    val useResume: Boolean = true,
    val userAgent: String = "",
    val customHeaders: String = "",
    val cookies: String = "",
    val httpMethod: String = "GET",
    val mirrors: String = "",
    val checksumAlgo: String = "",
    val checksumExpected: String = "",
    val maxSpeedKbps: String = "0",
    val mode: DownloadMode = DownloadMode.HTTP,
    
    val fileSizeResult: FileSizeResult? = null,
    val fetchingFileSize: Boolean = false
)

private const val PREFS_NAME = "had_settings"

@HiltViewModel
class MainViewModel @Inject constructor(
    private val app: Application,
    private val dao: DownloadDao,
    private val smartDownloader: SmartDownloader,
    private val hlsDownloader: HlsDownloader,
    private val scraperEngine: ScraperEngine,
    private val analyticsRepository: AnalyticsRepository,
    private val remoteDownloadServer: RemoteDownloadServer,
    private val clipboardMonitor: ClipboardMonitor,
    private val duplicateDetector: DuplicateDetector,
    private val webArchiveEngine: WebArchiveEngine,
    private val fileSizeFetcher: FileSizeFetcher
) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val sessionDir: File by lazy {
        File(app.filesDir, ".had_sessions").also { it.mkdirs() }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _form = MutableStateFlow(NewDownloadForm())
    val form: StateFlow<NewDownloadForm> = _form.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _browserUiState = MutableStateFlow(BrowserUiState().let { s ->
        val tab = BrowserTab()
        s.copy(tabs = listOf(tab), activeTabId = tab.id)
    })
    val browserUiState: StateFlow<BrowserUiState> = _browserUiState.asStateFlow()

    private val torrentEngine = TorrentEngine()
    private val _torrentState = MutableStateFlow(TorrentUiState())
    val torrentState: StateFlow<TorrentUiState> = _torrentState.asStateFlow()

    private val _parsedTorrentInfo = MutableStateFlow<TorrentInfo?>(null)
    val parsedTorrentInfo: StateFlow<TorrentInfo?> = _parsedTorrentInfo.asStateFlow()

    private val _archiveSessions = MutableStateFlow<List<ArchiveSession>>(emptyList())
    val archiveSessions: StateFlow<List<ArchiveSession>> = _archiveSessions.asStateFlow()

    private val _archiveProgress = MutableStateFlow<Map<Long, ArchiveProgress>>(emptyMap())
    val archiveProgress: StateFlow<Map<Long, ArchiveProgress>> = _archiveProgress.asStateFlow()

    val overallStats = analyticsRepository.observeOverallStats()
    val monthlyStats = analyticsRepository.observeMonthlyStats()
    val hourlyStats = analyticsRepository.observeHourlyDistribution()
    val recentAnalyticsEvents = analyticsRepository.observeRecentEvents()
    val currentSpeedHistory = analyticsRepository.observeSpeedHistory(0L)
    val remoteServerState: StateFlow<RemoteServerState> = remoteDownloadServer.state
    val clipboardEvent: StateFlow<ClipboardEvent?> = clipboardMonitor.event

    private val stoppedIds = mutableSetOf<Long>()
    private val stoppedLock = Any()
    private val scheduledLaunchIds = mutableSetOf<Long>()
    private val scheduleLaunchLock = Any()

    private var fileSizeFetchJob: Job? = null

    private val activeCount: Int
        get() = _state.value.downloads.count {
            it.status == DownloadStatus.DOWNLOADING ||
                    it.status == DownloadStatus.CONNECTING ||
                    it.status == DownloadStatus.MERGING
        }

    private val triggerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra("download_id", -1L)
            if (id < 0) return
            viewModelScope.launch {
                dao.getById(id) ?: return@launch
                synchronized(stoppedLock) { stoppedIds.remove(id) }
                ForegroundDownloadService.startDownload(app, id)
            }
        }
    }

    init {
        smartDownloader.sessionDir = sessionDir

        _form.update {
            it.copy(
                outputDir = _settings.value.defaultOutputDir.ifBlank { it.outputDir },
                userAgent = _settings.value.defaultUserAgent,
                threads = _settings.value.defaultThreads.toString()
            )
        }

        ForegroundDownloadService.ensureRunning(app)

        val filter = IntentFilter("com.had.downloader.START_QUEUED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            app.registerReceiver(triggerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            app.registerReceiver(triggerReceiver, filter)
        }

        viewModelScope.launch {
            dao.observeAll().collect { list ->
                _state.update {
                    it.copy(
                        downloads = list,
                        stats = DownloadStats(
                            total = list.size,
                            completed = list.count { d -> d.status == DownloadStatus.COMPLETED },
                            failed = list.count { d -> d.status == DownloadStatus.FAILED },
                            active = list.count { d ->
                                d.status == DownloadStatus.DOWNLOADING ||
                                        d.status == DownloadStatus.CONNECTING ||
                                        d.status == DownloadStatus.MERGING
                            },
                            totalDownloaded = list
                                .filter { d -> d.status == DownloadStatus.COMPLETED }
                                .sumOf { d -> d.downloadedBytes }
                        )
                    )
                }
                promoteFromQueue()
            }
        }

        viewModelScope.launch {
            smartDownloader.progress.collect { (id, prog) ->
                val isStopped = synchronized(stoppedLock) { id in stoppedIds }
                if (isStopped) return@collect

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
                        analyticsRepository.recordComplete(id, prog.totalBytes, prog.speedBps, prog.speedBps, 0, true)
                        synchronized(stoppedLock) { stoppedIds.remove(id) }
                        synchronized(scheduleLaunchLock) { scheduledLaunchIds.remove(id) }
                    }
                    DownloadStatus.FAILED -> {
                        dao.markFailed(id, DownloadStatus.FAILED, prog.status)
                        analyticsRepository.recordComplete(id, prog.downloadedBytes, 0L, 0L, 0, false)
                        synchronized(stoppedLock) { stoppedIds.remove(id) }
                        synchronized(scheduleLaunchLock) { scheduledLaunchIds.remove(id) }
                    }
                    DownloadStatus.CANCELLED -> {
                        dao.markFailed(id, DownloadStatus.CANCELLED, "Stopped by user")
                        synchronized(stoppedLock) { stoppedIds.remove(id) }
                        synchronized(scheduleLaunchLock) { scheduledLaunchIds.remove(id) }
                    }
                    else -> {
                        dao.updateProgress(
                            id = id, status = status, progress = prog.percent,
                            downloaded = prog.downloadedBytes, total = prog.totalBytes,
                            speed = prog.speedBps, eta = prog.etaSeconds
                        )
                        analyticsRepository.recordSpeedSample(id, prog.speedBps)
                    }
                }

                val current = _state.value.chunkStates[id] ?: LiveChunkState(id)
                _state.update {
                    it.copy(chunkStates = it.chunkStates + (id to current.copy(chunks = prog.chunks)))
                }
            }
        }

        viewModelScope.launch {
            torrentEngine.progress.collect { prog ->
                _torrentState.update { current ->
                    val active = current.active.toMutableList()
                    val completed = current.completed.toMutableList()
                    val failed = current.failed.toMutableList()
                    active.removeAll { it.infoHashHex == prog.infoHashHex }
                    completed.removeAll { it.infoHashHex == prog.infoHashHex }
                    failed.removeAll { it.infoHashHex == prog.infoHashHex }
                    when {
                        prog.status == "COMPLETED" -> completed.add(0, prog)
                        prog.status.startsWith("FAILED") -> failed.add(0, prog)
                        else -> active.add(0, prog)
                    }
                    current.copy(active = active, completed = completed, failed = failed)
                }
            }
        }

        viewModelScope.launch {
            webArchiveEngine.progress.collect { prog ->
                _archiveProgress.update { current ->
                    val session = _archiveSessions.value.firstOrNull {
                        current[it.id]?.status == "RUNNING" || prog.status == "RUNNING"
                    }
                    if (session != null) current + (session.id to prog)
                    else {
                        val running = _archiveSessions.value.firstOrNull {
                            current[it.id]?.status?.let { s -> s == "RUNNING" || s == "IDLE" } ?: false
                        }
                        if (running != null) current + (running.id to prog) else current
                    }
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                checkScheduleWindow()
            }
        }

        viewModelScope.launch {
            while (true) {
                delay(5_000L)
            }
        }

        viewModelScope.launch { analyticsRepository.purgeOlderThan(30) }

        clipboardMonitor.start(app)

        remoteDownloadServer.onDownloadRequested = { url, filename, headers ->
            viewModelScope.launch {
                val resolvedFilename = filename
                    ?: sanitizeUrl(url).substringAfterLast('/').substringBefore('?').ifBlank { "download" }
                val s = _settings.value
                val outputDir = s.defaultOutputDir.ifBlank {
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/HAD"
                }
                val dupResult = duplicateDetector.check(url, resolvedFilename, outputDir)
                if (dupResult.isDuplicate) {
                    val canStart = activeCount < s.maxConcurrent
                    val item = DownloadItem(
                        url = url, filename = resolvedFilename, outputDir = outputDir,
                        mode = detectMode(url), threads = s.defaultThreads,
                        userAgent = s.defaultUserAgent.ifBlank { DEFAULT_USER_AGENT },
                        customHeaders = headers.entries.joinToString("\n") { "${it.key}: ${it.value}" },
                        maxRetries = s.defaultRetries, timeoutSec = s.defaultTimeoutSec,
                        status = if (canStart) DownloadStatus.CONNECTING else DownloadStatus.QUEUED
                    )
                    _state.update {
                        it.copy(showDuplicateDialog = true, duplicateResult = dupResult, pendingDuplicateItem = item)
                    }
                } else {
                    startRemoteDownload(url, resolvedFilename, headers)
                }
            }
        }
    }

    fun triggerFileSizeFetch(url: String) {
        fileSizeFetchJob?.cancel()
        if (url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            _form.update { it.copy(fileSizeResult = null, fetchingFileSize = false) }
            return
        }
        _form.update { it.copy(fileSizeResult = FileSizeResult(url, -1L, false, "", "", loading = true), fetchingFileSize = true) }
        fileSizeFetchJob = viewModelScope.launch {
            delay(800L) 
            val currentUrl = _form.value.url
            if (currentUrl != url) return@launch 

            val headers = parseHeadersFromForm(_form.value.customHeaders)
            val cookies = _form.value.cookies.ifBlank { null }
            val ua = _form.value.userAgent.ifBlank { DEFAULT_USER_AGENT }

            val result = fileSizeFetcher.fetch(url, headers, cookies, ua)
            _form.update { it.copy(fileSizeResult = result, fetchingFileSize = false) }

            if (result.filename.isNotBlank() && _form.value.url == url) {
                val currentFilename = sanitizeUrl(url).substringAfterLast('/').substringBefore('?')
                if (result.filename != currentFilename && result.filename.contains('.')) {
                    
                }
            }
        }
    }

    private fun parseHeadersFromForm(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.lines().filter { it.contains(':') }.associate { line ->
            val idx = line.indexOf(':')
            line.substring(0, idx).trim() to line.substring(idx + 1).trim()
        }
    }

    fun smartPasteToAddDialog() {
        val rawText = readRawClipboard() ?: return
        val urls = extractUrlsFromText(rawText)

        when {
            urls.isEmpty() -> {
                
                if (rawText.isNotBlank()) {
                    _form.update { it.copy(url = rawText.trim()) }
                }
            }
            urls.size == 1 -> {
                val url = urls.first()
                val mode = detectMode(url)
                _form.update { it.copy(url = url, mode = mode, fileSizeResult = null) }
                triggerFileSizeFetch(url)
            }
            else -> {
                
                _state.update { it.copy(showClipboardDialog = true, clipboardUrls = urls) }
            }
        }
    }

    private fun readRawClipboard(): String? {
        return runCatching {
            val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = cm.primaryClip ?: return null
            buildString {
                for (i in 0 until clip.itemCount) {
                    val text = clip.getItemAt(i)?.coerceToText(app)?.toString()
                    if (!text.isNullOrBlank()) {
                        append(text)
                        append("\n")
                    }
                }
            }.trim()
        }.getOrNull()
    }

    private fun extractUrlsFromText(text: String): List<String> {
        val urlRegex = Regex("""https?://[^\s"'<>\]\[)(\u0000-\u001F]+""")
        return urlRegex.findAll(text)
            .map { it.value.trimEnd('.', ',', ')', ']', ';', '"', '\'') }
            .filter { it.length > 10 }
            .distinct()
            .toList()
    }

    fun downloadTelegramFile(url: String, filename: String, outputDir: String, startNow: Boolean) {
        val s = _settings.value
        val safeOutputDir = outputDir.ifBlank {
            s.defaultOutputDir.ifBlank {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/HAD"
            }
        }
        viewModelScope.launch {
            val dupResult = duplicateDetector.check(url, filename, safeOutputDir)
            val finalFilename = if (dupResult.isDuplicate)
                duplicateDetector.generateNewName(filename, safeOutputDir) else filename
            val canStart = startNow && activeCount < s.maxConcurrent
            val item = DownloadItem(
                url = url,
                filename = finalFilename,
                outputDir = safeOutputDir,
                mode = DownloadMode.HTTP,
                threads = s.defaultThreads,
                userAgent = "TelegramBot/1.0",
                maxRetries = s.defaultRetries,
                timeoutSec = s.defaultTimeoutSec,
                status = if (canStart) DownloadStatus.CONNECTING else DownloadStatus.QUEUED
            )
            val id = dao.insert(item)
            if (canStart) {
                analyticsRepository.recordStart(item.copy(id = id))
                synchronized(stoppedLock) { stoppedIds.remove(id) }
                ForegroundDownloadService.startDownload(app, id)
            }
        }
    }

    private fun detectMode(url: String): DownloadMode = when {
        hlsDownloader.isHlsUrl(url) -> DownloadMode.HLS
        url.contains(".mpd", ignoreCase = true) -> DownloadMode.HTTP
        else -> DownloadMode.HTTP
    }

    private fun resolveFilenameForMode(filename: String, mode: DownloadMode): String {
        return if (mode == DownloadMode.HLS) {
            hlsDownloader.resolveHlsFilename(filename)
        } else {
            filename
        }
    }

    private suspend fun promoteFromQueue() {
        val maxConcurrent = _settings.value.maxConcurrent
        val currentActive = activeCount
        if (currentActive >= maxConcurrent) return

        val slots = maxConcurrent - currentActive
        val candidates = _state.value.downloads
            .filter { item -> item.status == DownloadStatus.QUEUED && item.scheduleFrom.isBlank() }
            .sortedBy { it.queuePriority }
            .take(slots)

        candidates.forEach { item ->
            val alreadyLaunching = synchronized(scheduleLaunchLock) {
                if (item.id in scheduledLaunchIds) true
                else { scheduledLaunchIds.add(item.id); false }
            }
            if (alreadyLaunching) return@forEach
            val updated = item.copy(status = DownloadStatus.CONNECTING)
            dao.update(updated)
            synchronized(stoppedLock) { stoppedIds.remove(item.id) }
            analyticsRepository.recordStart(updated)
            ForegroundDownloadService.startDownload(app, item.id)
        }
    }

    private suspend fun triggerScheduledDownloads() {
        val now = System.currentTimeMillis()
        val candidates = _state.value.downloads.filter { item ->
            item.status == DownloadStatus.QUEUED &&
                    item.scheduleFrom.isNotBlank() &&
                    (item.scheduleFrom.toLongOrNull() ?: Long.MAX_VALUE) <= now
        }
        candidates.forEach { item ->
            val alreadyLaunching = synchronized(scheduleLaunchLock) {
                if (item.id in scheduledLaunchIds) true
                else { scheduledLaunchIds.add(item.id); false }
            }
            if (alreadyLaunching) return@forEach
            dao.update(item.copy(scheduleFrom = "", scheduleTo = "", status = DownloadStatus.QUEUED, errorMessage = null))
            synchronized(stoppedLock) { stoppedIds.remove(item.id) }
            ForegroundDownloadService.startDownload(app, item.id)
        }
    }

    private fun scheduleWindowActive(): Boolean {
        val s = _settings.value
        if (s.defaultScheduleFrom.isBlank() || s.defaultScheduleTo.isBlank()) return true
        return try {
            val fromParts = s.defaultScheduleFrom.split(":")
            val toParts = s.defaultScheduleTo.split(":")
            val fromH = fromParts[0].toInt(); val fromM = fromParts[1].toInt()
            val toH = toParts[0].toInt(); val toM = toParts[1].toInt()
            val now = java.util.Calendar.getInstance()
            val curMins = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
            val fromMins = fromH * 60 + fromM
            val toMins = toH * 60 + toM
            if (fromMins <= toMins) curMins in fromMins..toMins
            else curMins >= fromMins || curMins <= toMins
        } catch (e: Exception) { true }
    }

    private suspend fun checkScheduleWindow() {
        val s = _settings.value
        if (s.defaultScheduleFrom.isBlank()) return
        val windowOpen = scheduleWindowActive()
        val activeDownloads = _state.value.downloads.filter {
            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.CONNECTING
        }
        if (!windowOpen && activeDownloads.isNotEmpty()) {
            activeDownloads.forEach { item -> stopDownloadInternal(item.id, scheduledStop = true) }
        } else if (windowOpen) {
            _state.value.downloads
                .filter { it.status == DownloadStatus.PAUSED }
                .forEach { item ->
                    val updated = item.copy(status = DownloadStatus.CONNECTING, errorMessage = null)
                    dao.update(updated)
                    synchronized(stoppedLock) { stoppedIds.remove(item.id) }
                    ForegroundDownloadService.startDownload(app, item.id)
                }
        }
    }

    fun showAddDialog() = _state.update { it.copy(showAddDialog = true) }
    fun hideAddDialog() {
        fileSizeFetchJob?.cancel()
        _state.update { it.copy(showAddDialog = false) }
    }
    fun showSettingsDialog() = _state.update { it.copy(showSettingsDialog = true) }
    fun hideSettingsDialog() = _state.update { it.copy(showSettingsDialog = false) }
    fun showScraperDialog() = _state.update { it.copy(showScraperDialog = true) }
    fun hideScraperDialog() = _state.update {
        it.copy(showScraperDialog = false, scraperResult = ScraperResult(""))
    }
    fun setTab(tab: Int) = _state.update { it.copy(activeTab = tab) }
    fun updateForm(f: NewDownloadForm) {
        _form.update { f }
    }

    fun hideDuplicateDialog() = _state.update {
        it.copy(showDuplicateDialog = false, duplicateResult = null, pendingDuplicateItem = null)
    }

    fun setThreadViewMode(id: Long, mode: ThreadViewMode) {
        val current = _state.value.chunkStates[id] ?: LiveChunkState(id)
        _state.update {
            it.copy(chunkStates = it.chunkStates + (id to current.copy(threadViewMode = mode)))
        }
    }

    fun toggleSelection(id: Long) {
        _state.update { s ->
            val newSet = s.selectedDownloadIds.toMutableSet()
            if (id in newSet) newSet.remove(id) else newSet.add(id)
            s.copy(selectedDownloadIds = newSet, selectionMode = newSet.isNotEmpty())
        }
    }

    fun clearSelection() = _state.update { it.copy(selectedDownloadIds = emptySet(), selectionMode = false) }

    fun deleteSelected(withFiles: Boolean) {
        val toDelete = _state.value.selectedDownloadIds.toSet()
        viewModelScope.launch {
            toDelete.forEach { id ->
                val item = dao.getById(id) ?: return@forEach
                synchronized(stoppedLock) { stoppedIds.add(id) }
                smartDownloader.cancelDownload(id)
                smartDownloader.deleteSession(id)
                if (withFiles) {
                    runCatching {
                        val f = File(item.outputDir, item.filename)
                        if (f.exists()) f.delete()
                    }
                }
                dao.delete(item)
                synchronized(stoppedLock) { stoppedIds.remove(id) }
            }
            _state.update {
                it.copy(
                    chunkStates = it.chunkStates - toDelete,
                    selectedDownloadIds = emptySet(),
                    selectionMode = false
                )
            }
        }
    }

    fun saveSettings(s: AppSettings) {
        prefs.edit().apply {
            putInt("threads", s.defaultThreads)
            putString("outputDir", s.defaultOutputDir)
            putInt("maxConcurrent", s.maxConcurrent)
            putString("proxy", s.defaultProxy)
            putLong("maxSpeed", s.defaultMaxSpeedBps)
            putInt("retries", s.defaultRetries)
            putInt("timeout", s.defaultTimeoutSec)
            putString("schedFrom", s.defaultScheduleFrom)
            putString("schedTo", s.defaultScheduleTo)
            putBoolean("gzip", s.enableGzip)
            putBoolean("session", s.saveSession)
            putBoolean("notif", s.showNotifications)
            putString("userAgent", s.defaultUserAgent)
            putString("threadViewMode", s.defaultThreadViewMode.name)
        }.apply()
        _settings.update { s }
        hideSettingsDialog()
    }

    private fun loadSettings(): AppSettings = AppSettings(
        defaultThreads = prefs.getInt("threads", 4),
        defaultOutputDir = prefs.getString("outputDir", "") ?: "",
        maxConcurrent = prefs.getInt("maxConcurrent", 2),
        defaultProxy = prefs.getString("proxy", "") ?: "",
        defaultMaxSpeedBps = prefs.getLong("maxSpeed", 0L),
        defaultRetries = prefs.getInt("retries", 5),
        defaultTimeoutSec = prefs.getInt("timeout", 30),
        defaultScheduleFrom = prefs.getString("schedFrom", "") ?: "",
        defaultScheduleTo = prefs.getString("schedTo", "") ?: "",
        enableGzip = prefs.getBoolean("gzip", false),
        saveSession = prefs.getBoolean("session", true),
        showNotifications = prefs.getBoolean("notif", true),
        defaultUserAgent = prefs.getString("userAgent", "") ?: "",
        defaultThreadViewMode = runCatching {
            ThreadViewMode.valueOf(prefs.getString("threadViewMode", "SEGMENT_BAR") ?: "SEGMENT_BAR")
        }.getOrDefault(ThreadViewMode.SEGMENT_BAR)
    )

    fun startDownload() {
        val f = _form.value
        if (f.url.isBlank()) return
        fileSizeFetchJob?.cancel()
        viewModelScope.launch {
            val s = _settings.value
            val rawFilename = sanitizeUrl(f.url).substringAfterLast('/').substringBefore('?').ifBlank { "download" }
            val filename = resolveFilenameForMode(rawFilename, f.mode)
            val outputDir = f.outputDir.trim().ifBlank { s.defaultOutputDir }
            val dupResult = duplicateDetector.check(f.url, filename, outputDir)

            if (dupResult.isDuplicate) {
                val canStart = activeCount < s.maxConcurrent
                val item = buildDownloadItem(f, startNow = canStart)
                hideAddDialog()
                _state.update {
                    it.copy(showDuplicateDialog = true, duplicateResult = dupResult, pendingDuplicateItem = item)
                }
            } else {
                val canStart = activeCount < s.maxConcurrent
                val item = buildDownloadItem(f, startNow = canStart)
                val id = dao.insert(item)
                hideAddDialog()
                resetForm()

                val schedEpoch = item.scheduleFrom.toLongOrNull()
                when {
                    schedEpoch != null && schedEpoch > System.currentTimeMillis() -> {
                        
                        Scheduler.schedule(app, id, schedEpoch)
                    }
                    item.status == DownloadStatus.CONNECTING -> {
                        analyticsRepository.recordStart(item.copy(id = id))
                        synchronized(stoppedLock) { stoppedIds.remove(id) }
                        ForegroundDownloadService.startDownload(app, id)
                    }
                }
            }
        }
    }

    fun resolveDuplicate(action: DuplicateAction) {
        val item = _state.value.pendingDuplicateItem ?: return
        val dupResult = _state.value.duplicateResult ?: return
        hideDuplicateDialog()
        resetForm()

        val finalFilename = duplicateDetector.applyAction(action, item.filename, item.outputDir, dupResult) ?: return

        viewModelScope.launch {
            val s = _settings.value
            val canStart = activeCount < s.maxConcurrent
            val finalItem = item.copy(
                filename = finalFilename,
                status = if (canStart) DownloadStatus.CONNECTING else DownloadStatus.QUEUED
            )
            val id = dao.insert(finalItem)
            if (canStart) {
                analyticsRepository.recordStart(finalItem.copy(id = id))
                synchronized(stoppedLock) { stoppedIds.remove(id) }
                ForegroundDownloadService.startDownload(app, id)
            }
        }
    }

    fun addToQueue() {
        val f = _form.value
        if (f.url.isBlank()) return
        fileSizeFetchJob?.cancel()
        viewModelScope.launch {
            val item = buildDownloadItem(f, startNow = false)
            val id = dao.insert(item)
            hideAddDialog()
            resetForm()

            val schedEpoch = item.scheduleFrom.toLongOrNull()
            if (schedEpoch != null && schedEpoch > System.currentTimeMillis()) {
                Scheduler.schedule(app, id, schedEpoch)
            }
        }
    }

    fun startAllQueued() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val maxC = _settings.value.maxConcurrent
            var started = activeCount
            _state.value.downloads
                .filter { item ->
                    item.status == DownloadStatus.QUEUED && (
                            item.scheduleFrom.isBlank() ||
                                    (item.scheduleFrom.toLongOrNull() ?: Long.MAX_VALUE) <= now
                            )
                }
                .sortedBy { it.queuePriority }
                .forEach { item ->
                    if (started < maxC) {
                        val updated = item.copy(status = DownloadStatus.CONNECTING)
                        dao.update(updated)
                        analyticsRepository.recordStart(updated)
                        synchronized(stoppedLock) { stoppedIds.remove(item.id) }
                        ForegroundDownloadService.startDownload(app, item.id)
                        started++
                    }
                }
        }
    }

    fun stopDownload(id: Long) = stopDownloadInternal(id, scheduledStop = false)

    private fun stopDownloadInternal(id: Long, scheduledStop: Boolean) {
        synchronized(stoppedLock) { stoppedIds.add(id) }
        smartDownloader.cancelDownload(id)
        ForegroundDownloadService.stopDownload(app, id)
        viewModelScope.launch {
            if (scheduledStop) {
                val item = dao.getById(id)
                if (item != null) {
                    dao.markFailed(id, DownloadStatus.PAUSED,
                        "Paused by schedule · ${item.downloadedBytes.toHumanSize()} saved")
                }
            } else {
                dao.markFailed(id, DownloadStatus.CANCELLED, "Stopped by user")
            }
        }
    }

    fun deleteDownload(item: DownloadItem) {
        synchronized(stoppedLock) { stoppedIds.add(item.id) }
        smartDownloader.cancelDownload(item.id)
        smartDownloader.deleteSession(item.id)
        synchronized(scheduleLaunchLock) { scheduledLaunchIds.remove(item.id) }
        viewModelScope.launch {
            dao.delete(item)
            synchronized(stoppedLock) { stoppedIds.remove(item.id) }
        }
        _state.update { it.copy(chunkStates = it.chunkStates - item.id) }
    }

    fun deleteDownloadWithFile(item: DownloadItem) {
        synchronized(stoppedLock) { stoppedIds.add(item.id) }
        smartDownloader.cancelDownload(item.id)
        smartDownloader.deleteSession(item.id)
        synchronized(scheduleLaunchLock) { scheduledLaunchIds.remove(item.id) }
        viewModelScope.launch {
            runCatching {
                val f = File(item.outputDir, item.filename)
                if (f.exists()) f.delete()
            }
            dao.delete(item)
            synchronized(stoppedLock) { stoppedIds.remove(item.id) }
        }
        _state.update { it.copy(chunkStates = it.chunkStates - item.id) }
    }

    fun retryDownload(item: DownloadItem) {
        synchronized(stoppedLock) { stoppedIds.remove(item.id) }
        synchronized(scheduleLaunchLock) { scheduledLaunchIds.remove(item.id) }
        viewModelScope.launch {
            val hasSession = smartDownloader.hasSession(item.id)
            val sessionProgress = if (hasSession) smartDownloader.getSessionProgress(item.id) else null
            val (restoredDownloaded, restoredProgress) = sessionProgress ?: (item.downloadedBytes to item.progress)
            val canStart = activeCount < _settings.value.maxConcurrent
            val reset = item.copy(
                status = if (canStart) DownloadStatus.CONNECTING else DownloadStatus.QUEUED,
                progress = restoredProgress,
                downloadedBytes = restoredDownloaded,
                errorMessage = null,
                scheduleFrom = ""
            )
            dao.update(reset)
            analyticsRepository.recordStart(reset)
            if (canStart) ForegroundDownloadService.startDownload(app, item.id)
        }
    }

    fun pasteFromClipboard() {
        val urls = clipboardMonitor.pasteAndDetect(app)
        if (urls.isNotEmpty()) {
            _state.update { it.copy(showClipboardDialog = true, clipboardUrls = urls) }
        }
    }

    fun pasteToAddDialog() {
        val urls = clipboardMonitor.readCurrentClipboard(app)
        when {
            urls.isEmpty() -> {}
            urls.size == 1 -> {
                val url = urls.first()
                val mode = detectMode(url)
                _form.update { it.copy(url = url, mode = mode) }
                triggerFileSizeFetch(url)
            }
            else -> {
                _state.update { it.copy(showClipboardDialog = true, clipboardUrls = urls) }
            }
        }
    }

    fun startRemoteServer() = remoteDownloadServer.start(app)
    fun stopRemoteServer() = remoteDownloadServer.stop()

    private fun startRemoteDownload(url: String, filename: String, headers: Map<String, String>) {
        val s = _settings.value
        viewModelScope.launch {
            val outputDir = s.defaultOutputDir.ifBlank {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/HAD"
            }
            val mode = detectMode(url)
            val resolvedFilename = resolveFilenameForMode(filename, mode)
            val canStart = activeCount < s.maxConcurrent
            val item = DownloadItem(
                url = url, filename = resolvedFilename, outputDir = outputDir, mode = mode,
                threads = s.defaultThreads,
                userAgent = s.defaultUserAgent.ifBlank { DEFAULT_USER_AGENT },
                customHeaders = headers.entries.joinToString("\n") { "${it.key}: ${it.value}" },
                maxRetries = s.defaultRetries, timeoutSec = s.defaultTimeoutSec,
                status = if (canStart) DownloadStatus.CONNECTING else DownloadStatus.QUEUED
            )
            val id = dao.insert(item)
            if (canStart) {
                analyticsRepository.recordStart(item.copy(id = id))
                synchronized(stoppedLock) { stoppedIds.remove(id) }
                ForegroundDownloadService.startDownload(app, id)
            }
        }
    }

    fun dismissClipboardDialog() {
        clipboardMonitor.dismiss()
        _state.update { it.copy(showClipboardDialog = false, clipboardUrls = emptyList()) }
    }

    fun downloadClipboardUrls(urls: List<String>) {
        val s = _settings.value
        viewModelScope.launch {
            var started = activeCount
            urls.forEach { url ->
                val mode = detectMode(url)
                val rawFilename = sanitizeUrl(url).substringAfterLast('/').substringBefore('?').ifBlank { "download" }
                val filename = resolveFilenameForMode(rawFilename, mode)
                val outputDir = s.defaultOutputDir.ifBlank {
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/HAD"
                }
                val dupResult = duplicateDetector.check(url, filename, outputDir)
                val finalFilename = if (dupResult.isDuplicate)
                    duplicateDetector.generateNewName(filename, outputDir) else filename
                val canStart = started < s.maxConcurrent
                val item = DownloadItem(
                    url = url.trim(), filename = finalFilename, outputDir = outputDir, mode = mode,
                    threads = s.defaultThreads,
                    userAgent = s.defaultUserAgent.ifBlank { DEFAULT_USER_AGENT },
                    maxRetries = s.defaultRetries, timeoutSec = s.defaultTimeoutSec,
                    status = if (canStart) DownloadStatus.CONNECTING else DownloadStatus.QUEUED
                )
                val id = dao.insert(item)
                if (canStart) {
                    analyticsRepository.recordStart(item.copy(id = id))
                    synchronized(stoppedLock) { stoppedIds.remove(id) }
                    ForegroundDownloadService.startDownload(app, id)
                    started++
                }
            }
        }
        dismissClipboardDialog()
    }

    fun queueClipboardUrls(urls: List<String>) {
        val s = _settings.value
        viewModelScope.launch {
            urls.forEach { url ->
                val mode = detectMode(url)
                val rawFilename = sanitizeUrl(url).substringAfterLast('/').substringBefore('?').ifBlank { "download" }
                val filename = resolveFilenameForMode(rawFilename, mode)
                val outputDir = s.defaultOutputDir.ifBlank {
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/HAD"
                }
                val dupResult = duplicateDetector.check(url, filename, outputDir)
                val finalFilename = if (dupResult.isDuplicate)
                    duplicateDetector.generateNewName(filename, outputDir) else filename
                dao.insert(DownloadItem(
                    url = url.trim(), filename = finalFilename, outputDir = outputDir, mode = mode,
                    threads = s.defaultThreads,
                    userAgent = s.defaultUserAgent.ifBlank { DEFAULT_USER_AGENT },
                    maxRetries = s.defaultRetries, timeoutSec = s.defaultTimeoutSec,
                    status = DownloadStatus.QUEUED
                ))
            }
        }
        dismissClipboardDialog()
    }

    fun scrape(url: String) {
        if (url.isBlank()) return
        _state.update { it.copy(scraperResult = ScraperResult(url, loading = true)) }
        val config = DownloadConfig(
            url = sanitizeUrl(url), outputPath = "",
            userAgent = _form.value.userAgent.ifBlank { DEFAULT_USER_AGENT }
        )
        viewModelScope.launch {
            runCatching {
                val links = scraperEngine.scrape(sanitizeUrl(url), emptySet(), config)
                _state.update {
                    it.copy(scraperResult = ScraperResult(
                        pageUrl = url,
                        links = links.map { l ->
                            ScrapedLinkUi(url = l.url, text = l.text,
                                typeLabel = scraperEngine.typeLabelFor(l.type), linkType = l.type)
                        },
                        loading = false
                    ))
                }
            }.onFailure { e ->
                _state.update { it.copy(scraperResult = ScraperResult(url, loading = false, error = e.message)) }
            }
        }
    }

    fun removeScrapedLink(link: ScrapedLinkUi) {
        _state.update { s ->
            s.copy(scraperResult = s.scraperResult.copy(links = s.scraperResult.links.filter { it.url != link.url }))
        }
    }

    fun addScrapedLinkAndStart(link: ScrapedLinkUi) {
        val mode = when (link.linkType) {
            LinkType.HLS -> DownloadMode.HLS
            else -> DownloadMode.HTTP
        }
        _form.update { it.copy(url = link.url, mode = mode) }
        hideScraperDialog()
        showAddDialog()
        triggerFileSizeFetch(link.url)
    }

    fun downloadScrapedLinks(links: List<ScrapedLinkUi>) {
        if (links.isEmpty()) return
        val s = _settings.value
        viewModelScope.launch {
            var started = activeCount
            links.forEach { link ->
                val mode = when (link.linkType) {
                    LinkType.HLS -> DownloadMode.HLS
                    else -> DownloadMode.HTTP
                }
                val rawFilename = sanitizeUrl(link.url).substringAfterLast('/').substringBefore('?').ifBlank { "download" }
                val filename = resolveFilenameForMode(rawFilename, mode)
                val outputDir = s.defaultOutputDir.ifBlank {
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/HAD"
                }
                val dupResult = duplicateDetector.check(link.url, filename, outputDir)
                val finalFilename = if (dupResult.isDuplicate)
                    duplicateDetector.generateNewName(filename, outputDir) else filename
                val canStart = started < s.maxConcurrent
                val item = DownloadItem(
                    url = link.url.trim(), filename = finalFilename, outputDir = outputDir, mode = mode,
                    threads = s.defaultThreads, proxy = s.defaultProxy.ifBlank { null },
                    userAgent = s.defaultUserAgent.ifBlank { DEFAULT_USER_AGENT },
                    maxRetries = s.defaultRetries, timeoutSec = s.defaultTimeoutSec,
                    status = if (canStart) DownloadStatus.CONNECTING else DownloadStatus.QUEUED
                )
                val id = dao.insert(item)
                if (canStart) {
                    analyticsRepository.recordStart(item.copy(id = id))
                    synchronized(stoppedLock) { stoppedIds.remove(id) }
                    ForegroundDownloadService.startDownload(app, id)
                    started++
                }
            }
            hideScraperDialog()
        }
    }

    fun queueScrapedLinks(links: List<ScrapedLinkUi>) {
        if (links.isEmpty()) return
        val s = _settings.value
        viewModelScope.launch {
            links.forEach { link ->
                val mode = when (link.linkType) {
                    LinkType.HLS -> DownloadMode.HLS
                    else -> DownloadMode.HTTP
                }
                val rawFilename = sanitizeUrl(link.url).substringAfterLast('/').substringBefore('?').ifBlank { "download" }
                val filename = resolveFilenameForMode(rawFilename, mode)
                val outputDir = s.defaultOutputDir.ifBlank {
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/HAD"
                }
                dao.insert(DownloadItem(
                    url = link.url.trim(), filename = filename, outputDir = outputDir, mode = mode,
                    threads = s.defaultThreads, proxy = s.defaultProxy.ifBlank { null },
                    userAgent = s.defaultUserAgent.ifBlank { DEFAULT_USER_AGENT },
                    maxRetries = s.defaultRetries, timeoutSec = s.defaultTimeoutSec,
                    status = DownloadStatus.QUEUED
                ))
            }
            hideScraperDialog()
        }
    }

    fun getLogs(id: Long): List<LogEntry> = _state.value.logs[id] ?: emptyList()

    fun updateBrowserTab(tabId: String, update: (BrowserTab) -> BrowserTab) {
        _browserUiState.update { state ->
            state.copy(tabs = state.tabs.map { if (it.id == tabId) update(it) else it })
        }
    }

    fun addBrowserTab() {
        val newTab = BrowserTab()
        _browserUiState.update { state ->
            state.copy(tabs = state.tabs + newTab, activeTabId = newTab.id, showTabSwitcher = false)
        }
    }

    fun closeBrowserTab(tabId: String) {
        _browserUiState.update { state ->
            val remaining = state.tabs.filter { it.id != tabId }
            val tabs = if (remaining.isEmpty()) listOf(BrowserTab()) else remaining
            val activeId = if (state.activeTabId == tabId) tabs.last().id else state.activeTabId
            state.copy(tabs = tabs, activeTabId = activeId)
        }
    }

    fun switchBrowserTab(tabId: String) {
        _browserUiState.update { it.copy(activeTabId = tabId, showTabSwitcher = false) }
    }

    fun toggleTabSwitcher() = _browserUiState.update { it.copy(showTabSwitcher = !it.showTabSwitcher) }
    fun toggleInterceptedPanel() = _browserUiState.update { it.copy(showIntercepted = !it.showIntercepted) }

    fun addInterceptedRequest(tabId: String, request: InterceptedRequest) {
        _browserUiState.update { state ->
            state.copy(tabs = state.tabs.map { tab ->
                if (tab.id != tabId) tab
                else {
                    if (tab.interceptedRequests.any { it.url == request.url }) tab
                    else tab.copy(interceptedRequests = tab.interceptedRequests + request)
                }
            })
        }
    }

    fun clearIntercepted(tabId: String) {
        _browserUiState.update { state ->
            state.copy(tabs = state.tabs.map { tab ->
                if (tab.id == tabId) tab.copy(interceptedRequests = emptyList()) else tab
            })
        }
    }

    fun downloadIntercepted(request: InterceptedRequest) {
        val s = _settings.value
        val mode = when (request.linkType) {
            LinkType.HLS -> DownloadMode.HLS
            else -> DownloadMode.HTTP
        }
        val rawFilename = request.url.substringAfterLast('/').substringBefore('?').ifBlank { "download" }
        val filename = resolveFilenameForMode(rawFilename, mode)
        viewModelScope.launch {
            val outputDir = s.defaultOutputDir.ifBlank {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/HAD"
            }
            val dupResult = duplicateDetector.check(request.url, filename, outputDir)
            val finalFilename = if (dupResult.isDuplicate)
                duplicateDetector.generateNewName(filename, outputDir) else filename
            val canStart = activeCount < s.maxConcurrent
            val headers = request.headers.toMutableMap()
            request.referer?.let { headers["Referer"] = it }
            val item = DownloadItem(
                url = request.url, filename = finalFilename, outputDir = outputDir, mode = mode,
                threads = s.defaultThreads,
                userAgent = request.userAgent ?: s.defaultUserAgent.ifBlank { DEFAULT_USER_AGENT },
                cookies = request.cookies ?: "",
                customHeaders = headers.entries.joinToString("\n") { "${it.key}: ${it.value}" },
                maxRetries = s.defaultRetries, timeoutSec = s.defaultTimeoutSec,
                status = if (canStart) DownloadStatus.CONNECTING else DownloadStatus.QUEUED
            )
            val id = dao.insert(item)
            if (canStart) {
                analyticsRepository.recordStart(item.copy(id = id))
                synchronized(stoppedLock) { stoppedIds.remove(id) }
                ForegroundDownloadService.startDownload(app, id)
            }
        }
    }

    fun parseTorrentFile(bytes: ByteArray) {
        viewModelScope.launch { _parsedTorrentInfo.value = torrentEngine.parseTorrentFile(bytes) }
    }

    fun startTorrentDownload(info: TorrentInfo, fileSelection: List<Boolean>) {
        val s = _settings.value
        val outputDir = s.defaultOutputDir.ifBlank {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/HAD"
        }
        val updatedFiles = info.files.mapIndexed { idx, f -> f.copy(selected = fileSelection.getOrElse(idx) { true }) }
        torrentEngine.startDownload(info.copy(files = updatedFiles), outputDir)
        _parsedTorrentInfo.value = null
    }

    fun startMagnetDownload(magnet: String) {
        val magnetInfo = torrentEngine.parseMagnet(magnet) ?: return
        val s = _settings.value
        val outputDir = s.defaultOutputDir.ifBlank {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/HAD"
        }
        torrentEngine.startMagnetDownload(magnetInfo, outputDir)
    }

    fun stopTorrent(infoHashHex: String) {
        torrentEngine.stopDownload(infoHashHex)
        _torrentState.update { state ->
            val prog = state.active.find { it.infoHashHex == infoHashHex }
            if (prog != null) {
                state.copy(
                    active = state.active.filter { it.infoHashHex != infoHashHex },
                    failed = listOf(prog.copy(status = "CANCELLED")) + state.failed
                )
            } else state
        }
    }

    fun updateTorrentFileSelection(infoHashHex: String, fileIndex: Int, selected: Boolean) {
        torrentEngine.updateFileSelection(infoHashHex, fileIndex, selected)
    }

    fun clearParsedTorrent() { _parsedTorrentInfo.value = null }

    fun startArchive(config: ArchiveConfig) {
        val session = ArchiveSession(config = config)
        _archiveSessions.update { it + session }
        _archiveProgress.update { it + (session.id to ArchiveProgress(status = "RUNNING")) }
        viewModelScope.launch {
            webArchiveEngine.progress
                .onEach { prog ->
                    _archiveProgress.update { current -> current + (session.id to prog) }
                    if (prog.status == "COMPLETED" || prog.status.startsWith("FAILED")) {
                        _archiveSessions.update { list ->
                            list.map { s ->
                                if (s.id == session.id) s.copy(progress = prog, completedAt = System.currentTimeMillis())
                                else s
                            }
                        }
                    }
                }
                .launchIn(this)
        }
        webArchiveEngine.startArchive(session)
    }

    fun stopArchive(sessionId: Long) {
        webArchiveEngine.stopArchive(sessionId)
        _archiveProgress.update { current ->
            current[sessionId]?.let { current + (sessionId to it.copy(status = "STOPPED")) } ?: current
        }
        _archiveSessions.update { list ->
            list.map { s ->
                if (s.id == sessionId) s.copy(progress = s.progress.copy(status = "STOPPED"), completedAt = System.currentTimeMillis())
                else s
            }
        }
    }

    fun deleteArchive(sessionId: Long) {
        webArchiveEngine.stopArchive(sessionId)
        _archiveSessions.update { it.filter { s -> s.id != sessionId } }
        _archiveProgress.update { it - sessionId }
    }

    fun openArchiveFolder(session: ArchiveSession) {
        runCatching {
            val host = java.net.URL(session.config.targetUrl).host.replace(Regex("[.:/\\\\]"), "_")
            val folder = java.io.File(session.config.outputDir, host)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(folder), "resource/folder")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(intent)
        }
    }

    private fun buildDownloadItem(f: NewDownloadForm, startNow: Boolean): DownloadItem {
        val s = _settings.value
        val rawFilename = sanitizeUrl(f.url).substringAfterLast('/').substringBefore('?').ifBlank { "download" }
        val filename = resolveFilenameForMode(rawFilename, f.mode)
        val outputDir = f.outputDir.trim().ifBlank { s.defaultOutputDir }

        val hasWindow = s.defaultScheduleFrom.isNotBlank() && s.defaultScheduleTo.isNotBlank()
        val windowNowOpen = if (hasWindow) scheduleWindowActive() else true
        val nextWindowEpoch = if (hasWindow) computeNextWindowStart(s.defaultScheduleFrom) else null

        val (status, scheduleFrom) = when {
            startNow && windowNowOpen -> DownloadStatus.CONNECTING to ""
            startNow && !windowNowOpen -> DownloadStatus.QUEUED to (nextWindowEpoch?.toString() ?: "")
            !startNow && hasWindow -> DownloadStatus.QUEUED to (nextWindowEpoch?.toString() ?: "")
            else -> DownloadStatus.QUEUED to ""
        }

        return DownloadItem(
            url = f.url.trim(),
            filename = filename,
            outputDir = outputDir,
            mode = f.mode,
            threads = f.threads.toIntOrNull()?.coerceIn(1, 32) ?: s.defaultThreads,
            proxy = f.proxy.trim().ifBlank { s.defaultProxy.ifBlank { null } },
            userAgent = f.userAgent.trim().ifBlank { s.defaultUserAgent.ifBlank { DEFAULT_USER_AGENT } },
            customHeaders = f.customHeaders.trim(),
            cookies = f.cookies.trim(),
            httpMethod = f.httpMethod.trim().uppercase().ifBlank { "GET" },
            mirrors = f.mirrors.trim(),
            checksumAlgo = f.checksumAlgo.trim(),
            checksumExpected = f.checksumExpected.trim(),
            maxSpeedBps = (f.maxSpeedKbps.toLongOrNull() ?: 0L) * 1024L,
            verbose = f.verbose,
            useResume = f.useResume,
            maxRetries = s.defaultRetries,
            timeoutSec = s.defaultTimeoutSec,
            scheduleFrom = scheduleFrom,
            scheduleTo = s.defaultScheduleTo,
            status = status
        )
    }

    private fun computeNextWindowStart(fromHHMM: String): Long {
        return try {
            val parts = fromHHMM.split(":")
            val h = parts[0].toInt(); val m = parts[1].toInt()
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, h)
            cal.set(java.util.Calendar.MINUTE, m)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            if (cal.timeInMillis <= System.currentTimeMillis())
                cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
            cal.timeInMillis
        } catch (e: Exception) { System.currentTimeMillis() }
    }

    private fun resetForm() {
        val s = _settings.value
        _form.update {
            NewDownloadForm(
                outputDir = s.defaultOutputDir.ifBlank { it.outputDir },
                threads = s.defaultThreads.toString(),
                userAgent = s.defaultUserAgent
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        fileSizeFetchJob?.cancel()
        runCatching { app.unregisterReceiver(triggerReceiver) }
        clipboardMonitor.stop()
        remoteDownloadServer.stop()
        smartDownloader.stopAll()
        torrentEngine.stopAll()
        webArchiveEngine.stopAll()
    }
}