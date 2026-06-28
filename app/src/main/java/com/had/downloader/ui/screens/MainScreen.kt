package com.had.downloader.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.had.downloader.data.model.*
import com.had.downloader.service.FileSizeResult
import com.had.downloader.ui.components.*
import com.had.downloader.ui.theme.*
import kotlinx.coroutines.launch

enum class DrawerDestination(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ALL("All Downloads", Icons.Outlined.Inbox),
    ACTIVE("Active", Icons.Outlined.CloudDownload),
    DONE("Completed", Icons.Outlined.CheckCircle),
    FAILED("Failed", Icons.Outlined.ErrorOutline),
    BROWSER("Browser", Icons.Outlined.Language),
    TORRENT("Torrent", Icons.Outlined.CloudDownload),
    REMOTE_SERVER("Remote Server", Icons.Outlined.Wifi),
    ANALYTICS("Analytics", Icons.Outlined.BarChart),
    SETTINGS("Settings", Icons.Outlined.Settings),
    ABOUT("About", Icons.Outlined.Info),
    WEB_ARCHIVE("Web Archive", Icons.Outlined.Archive),
    GUIDE("Guide", Icons.Outlined.MenuBook)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val form by vm.form.collectAsState()
    val settings by vm.settings.collectAsState()
    val clipboardEvent by vm.clipboardEvent.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentDest by remember { mutableStateOf(DrawerDestination.ALL) }
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val path = it.path ?: ""
            vm.saveSettings(settings.copy(defaultOutputDir = path))
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HADDrawerContent(
                current = currentDest,
                stats = state.stats,
                onSelect = { dest ->
                    currentDest = dest
                    scope.launch { drawerState.close() }
                    when (dest) {
                        DrawerDestination.ALL           -> vm.setTab(0)
                        DrawerDestination.ACTIVE        -> vm.setTab(1)
                        DrawerDestination.DONE          -> vm.setTab(2)
                        DrawerDestination.FAILED        -> vm.setTab(3)
                        DrawerDestination.BROWSER       -> vm.setTab(6)
                        DrawerDestination.TORRENT       -> vm.setTab(7)
                        DrawerDestination.REMOTE_SERVER -> vm.setTab(8)
                        DrawerDestination.ANALYTICS     -> vm.setTab(9)
                        DrawerDestination.WEB_ARCHIVE   -> vm.setTab(10)
                        DrawerDestination.SETTINGS      -> { vm.setTab(0); vm.showSettingsDialog() }
                        DrawerDestination.ABOUT         -> vm.setTab(4)
                        DrawerDestination.GUIDE         -> vm.setTab(5)
                    }
                }
            )
        },
        scrimColor = SpaceBlack.copy(alpha = 0.7f)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(SpaceBlack)) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CyanGlow, Color.Transparent),
                        center = Offset(size.width * 0.5f, 0f),
                        radius = size.width * 0.8f
                    )
                )
            }

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    HADTopBar(
                        state = state,
                        currentDest = currentDest,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onPasteClick = {
                            vm.smartPasteToAddDialog()
                            if (state.clipboardUrls.isEmpty()) vm.showAddDialog()
                        }
                    )
                },
                floatingActionButton = {
                    val showFab = currentDest in listOf(
                        DrawerDestination.ALL,
                        DrawerDestination.ACTIVE,
                        DrawerDestination.DONE,
                        DrawerDestination.FAILED
                    )
                    if (showFab) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SmallFloatingActionButton(
                                onClick = { vm.showScraperDialog() },
                                containerColor = PurpleAccent,
                                contentColor = SpaceBlack,
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.TravelExplore,
                                    contentDescription = "Scrape links",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            FloatingActionButton(
                                onClick = { vm.showAddDialog() },
                                containerColor = CyanPrimary,
                                contentColor = SpaceBlack,
                                shape = CircleShape,
                                modifier = Modifier.size(60.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "New download",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    when (currentDest) {
                        DrawerDestination.ALL,
                        DrawerDestination.ACTIVE,
                        DrawerDestination.DONE,
                        DrawerDestination.FAILED -> {
                            StatsRow(state.stats, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                            val queued = state.downloads.filter {
                                it.status == DownloadStatus.QUEUED && it.scheduleFrom.isBlank()
                            }
                            if (queued.isNotEmpty()) {
                                StartAllQueuedBar(count = queued.size, onStartAll = vm::startAllQueued)
                            }
                            DownloadListContent(state, vm)
                        }
                        DrawerDestination.BROWSER       -> BrowserTabScreen(vm)
                        DrawerDestination.TORRENT       -> TorrentTab(vm)
                        DrawerDestination.REMOTE_SERVER -> RemoteServerTab(vm)
                        DrawerDestination.ANALYTICS     -> AnalyticsTab(vm)
                        DrawerDestination.ABOUT         -> AboutTab()
                        DrawerDestination.GUIDE         -> GuideTab()
                        DrawerDestination.WEB_ARCHIVE   -> WebArchiveTab(vm)
                        DrawerDestination.SETTINGS      -> {
                            StatsRow(state.stats, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                            DownloadListContent(state, vm)
                        }
                    }
                }
            }

            if (state.showAddDialog) {
                AddDownloadDialog(
                    form = form,
                    settings = settings,
                    onUpdate = vm::updateForm,
                    onUrlChange = { url ->
                        vm.updateForm(form.copy(url = url))
                        vm.triggerFileSizeFetch(url)
                    },
                    onStart = vm::startDownload,
                    onAddToQueue = vm::addToQueue,
                    onDismiss = vm::hideAddDialog
                )
            }

            if (state.showSettingsDialog) {
                SettingsDialog(
                    settings = settings,
                    onSave = vm::saveSettings,
                    onDismiss = vm::hideSettingsDialog,
                    onFolderSelect = { folderPickerLauncher.launch(null) }
                )
            }

            if (state.showScraperDialog) {
                ScraperDialog(
                    result = state.scraperResult,
                    maxConcurrent = settings.maxConcurrent,
                    onScrape = vm::scrape,
                    onAddLink = vm::addScrapedLinkAndStart,
                    onRemoveLink = vm::removeScrapedLink,
                    onDownloadLinks = vm::downloadScrapedLinks,
                    onQueueLinks = vm::queueScrapedLinks,
                    onDismiss = vm::hideScraperDialog
                )
            }

            if (state.showDuplicateDialog) {
                val dupResult = state.duplicateResult
                val pendingItem = state.pendingDuplicateItem
                if (dupResult != null && pendingItem != null) {
                    DuplicateDialog(
                        filename = pendingItem.filename,
                        url = pendingItem.url,
                        result = dupResult,
                        onAction = vm::resolveDuplicate,
                        onDismiss = vm::hideDuplicateDialog
                    )
                }
            }

            if (state.showClipboardDialog && state.clipboardUrls.isNotEmpty()) {
                ClipboardDownloadDialog(
                    urls = state.clipboardUrls,
                    onDownload = vm::downloadClipboardUrls,
                    onQueue = vm::queueClipboardUrls,
                    onDismiss = vm::dismissClipboardDialog
                )
            }
        }
    }
}

@Composable
private fun StartAllQueuedBar(count: Int, onStartAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(PurpleAccent.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .border(1.dp, PurpleAccent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$count item${if (count > 1) "s" else ""} queued", color = TextSecondary, fontSize = 13.sp)
        Button(
            onClick = onStartAll,
            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent, contentColor = SpaceBlack),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Start All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HADDrawerContent(
    current: DrawerDestination,
    stats: DownloadStats,
    onSelect: (DrawerDestination) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = SurfaceDark,
        drawerContentColor = TextPrimary,
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 360.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(CyanPrimary.copy(alpha = 0.15f), PurpleAccent.copy(alpha = 0.08f))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Brush.linearGradient(listOf(CyanPrimary, PurpleAccent)),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("H", color = SpaceBlack, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "HAD", color = TextPrimary, fontSize = 20.sp,
                                fontWeight = FontWeight.Black, letterSpacing = 3.sp
                            )
                            Text(
                                "Hyper Advanced Downloader", color = CyanPrimary,
                                fontSize = 10.sp, fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DrawerStatChip("${stats.active}", "active", CyanPrimary)
                        DrawerStatChip("${stats.completed}", "done", GreenSuccess)
                        DrawerStatChip(stats.totalDownloaded.toHumanSize(), "saved", PurpleAccent)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            DrawerSectionLabel("DOWNLOADS")
            listOf(
                DrawerDestination.ALL,
                DrawerDestination.ACTIVE,
                DrawerDestination.DONE,
                DrawerDestination.FAILED
            ).forEach { dest ->
                DrawerItem(dest = dest, selected = current == dest, onClick = { onSelect(dest) })
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = BorderColor, thickness = 0.5.dp
            )

            DrawerSectionLabel("TOOLS")
            DrawerItem(
                dest = DrawerDestination.BROWSER,
                selected = current == DrawerDestination.BROWSER,
                onClick = { onSelect(DrawerDestination.BROWSER) }
            )
            DrawerItem(
                dest = DrawerDestination.TORRENT,
                selected = current == DrawerDestination.TORRENT,
                onClick = { onSelect(DrawerDestination.TORRENT) },
                icon = Icons.Outlined.Share
            )
            DrawerItem(
                dest = DrawerDestination.REMOTE_SERVER,
                selected = current == DrawerDestination.REMOTE_SERVER,
                onClick = { onSelect(DrawerDestination.REMOTE_SERVER) }
            )
            DrawerItem(
                dest = DrawerDestination.ANALYTICS,
                selected = current == DrawerDestination.ANALYTICS,
                onClick = { onSelect(DrawerDestination.ANALYTICS) }
            )
            DrawerItem(
                dest = DrawerDestination.WEB_ARCHIVE,
                selected = current == DrawerDestination.WEB_ARCHIVE,
                onClick = { onSelect(DrawerDestination.WEB_ARCHIVE) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = BorderColor, thickness = 0.5.dp
            )

            DrawerSectionLabel("APP")
            listOf(
                DrawerDestination.ABOUT,
                DrawerDestination.SETTINGS,
                DrawerDestination.GUIDE
            ).forEach { dest ->
                DrawerItem(dest = dest, selected = current == dest, onClick = { onSelect(dest) })
            }

            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    "v2.0.0  •  Android 8+",
                    color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DrawerSectionLabel(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
}

@Composable
private fun DrawerItem(
    dest: DrawerDestination,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector = dest.icon
) {
    val bg = if (selected) CyanPrimary.copy(alpha = 0.12f) else Color.Transparent
    val tint = if (selected) CyanPrimary else TextSecondary
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .background(bg, shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            dest.label,
            color = if (selected) TextPrimary else TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (selected) {
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.size(6.dp).background(CyanPrimary, CircleShape))
        }
    }
}

@Composable
private fun DrawerStatChip(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HADTopBar(
    state: UiState,
    currentDest: DrawerDestination,
    onMenuClick: () -> Unit,
    onPasteClick: () -> Unit
) {
    val showDownloadActions = currentDest in listOf(
        DrawerDestination.ALL,
        DrawerDestination.ACTIVE,
        DrawerDestination.DONE,
        DrawerDestination.FAILED
    )

    TopAppBar(
        title = {
            Text(
                currentDest.label, color = TextPrimary,
                fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = CyanPrimary)
            }
        },
        actions = {
            if (showDownloadActions) {
                if (state.stats.active > 0) {
                    Box(
                        modifier = Modifier
                            .background(CyanPrimary.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${state.stats.active} active",
                            color = CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                IconButton(onClick = onPasteClick) {
                    Icon(
                        Icons.Outlined.ContentPaste,
                        contentDescription = "Paste URL",
                        tint = CyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun DownloadListContent(state: UiState, vm: MainViewModel) {
    val filtered = when (state.activeTab) {
        1 -> state.downloads.filter {
            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.CONNECTING
        }
        2 -> state.downloads.filter { it.status == DownloadStatus.COMPLETED }
        3 -> state.downloads.filter {
            it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED
        }
        else -> state.downloads
    }

    if (filtered.isEmpty()) {
        EmptyState(state.activeTab)
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = state.selectionMode) {
            SelectionActionBar(
                selectedCount = state.selectedDownloadIds.size,
                totalCount = filtered.size,
                onSelectAll = {
                    if (state.selectedDownloadIds.size == filtered.size) {
                        vm.clearSelection()
                    } else {
                        filtered.forEach { vm.toggleSelection(it.id) }
                    }
                },
                onClearSelection = vm::clearSelection,
                onDelete = {
                    vm.deleteSelected(withFiles = false)
                },
                onDeleteWithFiles = {
                    vm.deleteSelected(withFiles = true)
                }
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = 8.dp,
                bottom = if (state.selectionMode) 20.dp else 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it.id }) { item ->
                val chunkState = state.chunkStates[item.id]
                val isSelected = item.id in state.selectedDownloadIds

                AnimatedVisibility(
                    visible = true,
                    enter = slideInHorizontally() + fadeIn()
                ) {
                    DownloadCard(
                        item = item,
                        chunkState = chunkState,
                        isSelected = isSelected,
                        selectionMode = state.selectionMode,
                        onStop = { vm.stopDownload(item.id) },
                        onDelete = { vm.deleteDownload(item) },
                        onDeleteWithFile = { vm.deleteDownloadWithFile(item) },
                        onRetry = { vm.retryDownload(item) },
                        onViewMode = { mode -> vm.setThreadViewMode(item.id, mode) },
                        onLongPress = { vm.toggleSelection(item.id) },
                        onTap = {
                            if (state.selectionMode) {
                                vm.toggleSelection(item.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(stats: DownloadStats, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("${stats.total}", "Total", TextSecondary, Modifier.weight(1f))
        StatCard("${stats.active}", "Active", CyanPrimary, Modifier.weight(1f))
        StatCard("${stats.completed}", "Done", GreenSuccess, Modifier.weight(1f))
        StatCard(stats.totalDownloaded.toHumanSize(), "Saved", PurpleAccent, Modifier.weight(1f))
    }
}

@Composable
private fun EmptyState(tab: Int) {
    val (icon, msg) = when (tab) {
        1 -> Icons.Outlined.CloudDownload to "No active downloads"
        2 -> Icons.Outlined.CheckCircle to "No completed downloads yet"
        3 -> Icons.Outlined.ErrorOutline to "No failed downloads"
        else -> Icons.Outlined.Inbox to "No downloads yet"
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(56.dp), tint = TextMuted)
            Spacer(Modifier.height(12.dp))
            Text(msg, color = TextMuted, fontSize = 15.sp)
            if (tab == 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tap + to add a download",
                    color = TextMuted.copy(alpha = 0.6f), fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDownloadDialog(
    form: NewDownloadForm,
    settings: AppSettings,
    onUpdate: (NewDownloadForm) -> Unit,
    onUrlChange: (String) -> Unit,
    onStart: () -> Unit,
    onAddToQueue: () -> Unit,
    onDismiss: () -> Unit
) {
    var showAdvanced by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { onUpdate(form.copy(outputDir = it.path ?: form.outputDir)) }
    }

    val scheduleWindowActive = settings.defaultScheduleFrom.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CloudDownload, null,
                    tint = CyanPrimary, modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("New Download", fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(PurpleAccent.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        form.mode.name, color = PurpleAccent,
                        fontSize = 9.sp, fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (scheduleWindowActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OrangeWarn.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
                            .border(1.dp, OrangeWarn.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Schedule, null,
                            tint = OrangeWarn, modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Schedule window: ${settings.defaultScheduleFrom} → ${settings.defaultScheduleTo}",
                            color = OrangeWarn, fontSize = 11.sp, fontFamily = FontFamily.Monospace
                        )
                    }
                }

                OutlinedTextField(
                    value = form.url,
                    onValueChange = { onUrlChange(it) },
                    label = { Text("URL *", color = TextSecondary, fontSize = 12.sp) },
                    placeholder = { Text("https://example.com/file.zip", color = TextMuted, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Link, null, modifier = Modifier.size(18.dp), tint = CyanDim) },
                    trailingIcon = {
                        if (form.fetchingFileSize) {
                            CircularProgressIndicator(
                                color = CyanPrimary,
                                modifier = Modifier.size(18.dp).padding(2.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (form.url.isNotBlank()) {
                            IconButton(onClick = { onUrlChange("") }) {
                                Icon(Icons.Filled.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = CyanPrimary,
                        cursorColor = CyanPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        unfocusedContainerColor = ElevatedSurf,
                        focusedContainerColor = ElevatedSurf
                    ),
                    shape = RoundedCornerShape(10.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                )

                FileSizeInfoCard(result = form.fileSizeResult, loading = form.fetchingFileSize)

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(DownloadMode.HTTP, DownloadMode.HLS, DownloadMode.MULTI).forEach { mode ->
                        ModeChip(
                            label = mode.name,
                            selected = form.mode == mode,
                            onClick = { onUpdate(form.copy(mode = mode)) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HADTextField(
                        value = form.threads,
                        onValueChange = { onUpdate(form.copy(threads = it)) },
                        label = "Threads", placeholder = "4",
                        icon = Icons.Outlined.Speed,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(0.35f)
                    )
                    OutlinedTextField(
                        value = form.outputDir,
                        onValueChange = { onUpdate(form.copy(outputDir = it)) },
                        label = { Text("Save to", color = TextSecondary, fontSize = 12.sp) },
                        placeholder = {
                            Text("/sdcard/Downloads/HAD", color = TextMuted, fontSize = 11.sp)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.FolderOpen, null,
                                modifier = Modifier.size(18.dp), tint = CyanDim
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                                Icon(
                                    Icons.Outlined.Folder, null,
                                    tint = CyanPrimary, modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(0.65f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = CyanPrimary,
                            cursorColor = CyanPrimary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            unfocusedContainerColor = ElevatedSurf,
                            focusedContainerColor = ElevatedSurf
                        ),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvanced = !showAdvanced }
                        .background(ElevatedSurf, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        null, tint = CyanPrimary, modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Advanced Options", color = CyanPrimary, fontSize = 13.sp)
                }

                AnimatedVisibility(visible = showAdvanced) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        HADTextField(
                            value = form.proxy,
                            onValueChange = { onUpdate(form.copy(proxy = it)) },
                            label = "Proxy", placeholder = "socks5://127.0.0.1:1080",
                            icon = Icons.Outlined.VpnLock
                        )
                        HADTextField(
                            value = form.userAgent,
                            onValueChange = { onUpdate(form.copy(userAgent = it)) },
                            label = "User-Agent", placeholder = "Mozilla/5.0 ...",
                            icon = Icons.Outlined.Computer
                        )
                        HADTextField(
                            value = form.cookies,
                            onValueChange = { onUpdate(form.copy(cookies = it)) },
                            label = "Cookies", placeholder = "session=abc; token=xyz",
                            icon = Icons.Outlined.Cookie
                        )
                        HADTextField(
                            value = form.customHeaders,
                            onValueChange = { onUpdate(form.copy(customHeaders = it)) },
                            label = "Headers (key: value per line)",
                            placeholder = "Referer: https://site.com\nAuthorization: Bearer xxx",
                            icon = Icons.Outlined.Code, singleLine = false
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HADTextField(
                                value = form.httpMethod,
                                onValueChange = { onUpdate(form.copy(httpMethod = it)) },
                                label = "Method", placeholder = "GET",
                                icon = Icons.Outlined.SwapHoriz,
                                modifier = Modifier.weight(0.4f)
                            )
                            HADTextField(
                                value = form.maxSpeedKbps,
                                onValueChange = { onUpdate(form.copy(maxSpeedKbps = it)) },
                                label = "Max KB/s (0=∞)", placeholder = "0",
                                icon = Icons.Outlined.Speed,
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(0.6f)
                            )
                        }
                        HADTextField(
                            value = form.mirrors,
                            onValueChange = { onUpdate(form.copy(mirrors = it)) },
                            label = "Mirror URLs (one per line)",
                            placeholder = "https://mirror1.com/file.zip",
                            icon = Icons.Outlined.Hub, singleLine = false
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HADTextField(
                                value = form.checksumAlgo,
                                onValueChange = { onUpdate(form.copy(checksumAlgo = it)) },
                                label = "Hash algo", placeholder = "SHA-256",
                                icon = Icons.Outlined.Verified,
                                modifier = Modifier.weight(0.35f)
                            )
                            HADTextField(
                                value = form.checksumExpected,
                                onValueChange = { onUpdate(form.copy(checksumExpected = it)) },
                                label = "Expected hash", placeholder = "a1b2c3...",
                                icon = Icons.Outlined.Tag,
                                modifier = Modifier.weight(0.65f)
                            )
                        }
                        ToggleChip("Resume on retry", form.useResume) {
                            onUpdate(form.copy(useResume = it))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onAddToQueue,
                    enabled = form.url.isNotBlank(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent),
                    border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Outlined.AddCircleOutline, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Queue", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = onStart,
                    enabled = form.url.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary, contentColor = SpaceBlack
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Start", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun FileSizeInfoCard(result: FileSizeResult?, loading: Boolean) {
    AnimatedVisibility(
        visible = result != null || loading,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        if (loading && result == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ElevatedSurf, RoundedCornerShape(10.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(color = CyanPrimary, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Text("Fetching file info...", color = TextMuted, fontSize = 11.sp)
            }
            return@AnimatedVisibility
        }

        result ?: return@AnimatedVisibility

        if (result.error != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RedError.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                    .border(1.dp, RedError.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.ErrorOutline, null, tint = RedError, modifier = Modifier.size(14.dp))
                Text(result.error, color = RedError, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            return@AnimatedVisibility
        }

        val sizeColor = if (result.sizeBytes > 0) GreenSuccess else TextMuted
        val sizeText = if (result.sizeBytes > 0) result.sizeBytes.toHumanSize() else "Unknown size"

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = GreenSuccess.copy(alpha = 0.06f),
            border = BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Outlined.InsertDriveFile, null, tint = sizeColor, modifier = Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    if (result.filename.isNotBlank()) {
                        Text(
                            result.filename,
                            color = TextPrimary, fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(sizeText, color = sizeColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        if (result.contentType.isNotBlank()) {
                            Text(
                                result.contentType.substringBefore(';').trim(),
                                color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                if (result.acceptsRanges) {
                    Box(
                        modifier = Modifier
                            .background(CyanPrimary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("Resumable", color = CyanPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun ScraperDialog(
    result: ScraperResult,
    maxConcurrent: Int,
    onScrape: (String) -> Unit,
    onAddLink: (ScrapedLinkUi) -> Unit,
    onRemoveLink: (ScrapedLinkUi) -> Unit,
    onDownloadLinks: (List<ScrapedLinkUi>) -> Unit,
    onQueueLinks: (List<ScrapedLinkUi>) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf(result.pageUrl) }
    var extFilter by remember { mutableStateOf("") }
    var nameFilter by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("") }

    val selectedUrls = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(result.links) {
        val existing = selectedUrls.keys.toSet()
        result.links.forEach { link ->
            if (link.url !in existing) selectedUrls[link.url] = false
        }
    }

    val visibleLinks = remember(result.links) {
        result.links.filter { link ->
            link.linkType != com.had.downloader.service.LinkType.OTHER &&
                    link.url.startsWith("http")
        }
    }

    val filteredLinks = remember(visibleLinks, extFilter, nameFilter, typeFilter) {
        visibleLinks.filter { link ->
            val ext = link.url.substringAfterLast('.').substringBefore('?').lowercase()
            val name = link.url.substringAfterLast('/').substringBefore('?').lowercase()
            val extOk = extFilter.isBlank() || ext.contains(extFilter.trim().lowercase())
            val nameOk = nameFilter.isBlank() || name.contains(nameFilter.trim().lowercase())
            val typeOk = typeFilter.isBlank() || link.typeLabel.contains(typeFilter.trim().uppercase())
            extOk && nameOk && typeOk
        }
    }

    val selectedCount = filteredLinks.count { selectedUrls[it.url] == true }
    val allSelected = filteredLinks.isNotEmpty() && filteredLinks.all { selectedUrls[it.url] == true }

    val typeGroups = remember(visibleLinks) {
        visibleLinks.groupBy { it.typeLabel }.keys.sorted()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.TravelExplore, null,
                    tint = PurpleAccent, modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Link Scraper", fontWeight = FontWeight.Bold, color = TextPrimary)
                if (visibleLinks.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(PurpleAccent.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${visibleLinks.size}",
                            color = PurpleAccent, fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("Page URL", color = TextSecondary, fontSize = 12.sp) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleAccent, unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        unfocusedContainerColor = ElevatedSurf, focusedContainerColor = ElevatedSurf
                    ),
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        IconButton(onClick = { onScrape(url) }) {
                            Icon(Icons.Filled.Search, null, tint = PurpleAccent)
                        }
                    }
                )

                Button(
                    onClick = { onScrape(url) },
                    enabled = url.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleAccent, contentColor = SpaceBlack
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (result.loading) {
                        CircularProgressIndicator(
                            color = SpaceBlack,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Scraping...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Outlined.TravelExplore, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Scrape Links", fontWeight = FontWeight.Bold)
                    }
                }

                if (!result.error.isNullOrBlank()) {
                    Text(result.error, color = RedError, fontSize = 12.sp)
                }

                if (visibleLinks.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyanPrimary.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.Layers, null, tint = CyanPrimary, modifier = Modifier.size(13.dp))
                        Text(
                            "Max $maxConcurrent simultaneous · rest queued automatically",
                            color = CyanPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                        )
                    }

                    if (typeGroups.size > 1) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TypeFilterChip(label = "ALL", selected = typeFilter.isBlank()) {
                                typeFilter = ""
                            }
                            typeGroups.forEach { type ->
                                TypeFilterChip(label = type, selected = typeFilter == type) {
                                    typeFilter = if (typeFilter == type) "" else type
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = extFilter, onValueChange = { extFilter = it },
                            label = { Text("Extension", color = TextSecondary, fontSize = 10.sp) },
                            placeholder = { Text("mp4, zip…", color = TextMuted, fontSize = 10.sp) },
                            singleLine = true, modifier = Modifier.weight(0.4f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleAccent, unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                unfocusedContainerColor = ElevatedSurf, focusedContainerColor = ElevatedSurf
                            ),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                        )
                        OutlinedTextField(
                            value = nameFilter, onValueChange = { nameFilter = it },
                            label = { Text("Name contains", color = TextSecondary, fontSize = 10.sp) },
                            placeholder = { Text("episode, part…", color = TextMuted, fontSize = 10.sp) },
                            singleLine = true, modifier = Modifier.weight(0.6f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleAccent, unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                unfocusedContainerColor = ElevatedSurf, focusedContainerColor = ElevatedSurf
                            ),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${filteredLinks.size} of ${visibleLinks.size} shown",
                            color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val newVal = !allSelected
                                filteredLinks.forEach { selectedUrls[it.url] = newVal }
                            }
                        ) {
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = { v ->
                                    filteredLinks.forEach { selectedUrls[it.url] = v }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = PurpleAccent, checkmarkColor = SpaceBlack
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Select all", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    if (selectedCount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val toRemove = filteredLinks.filter { selectedUrls[it.url] == true }
                                    toRemove.forEach { onRemoveLink(it) }
                                    toRemove.forEach { selectedUrls.remove(it.url) }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError),
                                border = BorderStroke(1.dp, RedError.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Delete, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Remove $selectedCount", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                    filteredLinks.forEach { link ->
                        val isSelected = selectedUrls[link.url] == true
                        val rawName = link.url.substringAfterLast('/').substringBefore('?')
                        val ext = rawName.substringAfterLast('.').lowercase().take(5)
                        val displayName = when {
                            rawName.length > 32 -> "${rawName.take(18)}…${rawName.takeLast(10)}"
                            rawName.isBlank()   -> link.url.take(28)
                            else               -> rawName
                        }
                        val typeColor = typeLabelColor(link.typeLabel)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedUrls[link.url] = !(selectedUrls[link.url] ?: false) }
                                .background(
                                    if (isSelected) PurpleAccent.copy(alpha = 0.07f) else ElevatedSurf,
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) PurpleAccent.copy(alpha = 0.4f) else BorderColor,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { selectedUrls[link.url] = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = PurpleAccent,
                                    checkmarkColor = SpaceBlack,
                                    uncheckedColor = BorderColor
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    displayName,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                if (ext.isNotBlank() && !ext.contains('/') && !ext.contains('?')) {
                                    Box(
                                        modifier = Modifier
                                            .background(typeColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            ".${ext}", color = typeColor, fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(typeColor.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        link.typeLabel,
                                        color = typeColor.copy(alpha = 0.8f),
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Spacer(Modifier.width(2.dp))
                            IconButton(onClick = { onAddLink(link) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.Add, null, tint = CyanPrimary, modifier = Modifier.size(15.dp))
                            }
                            IconButton(
                                onClick = { onRemoveLink(link); selectedUrls.remove(link.url) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Delete, null, tint = RedError, modifier = Modifier.size(15.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            if (selectedCount > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val toQueue = filteredLinks.filter { selectedUrls[it.url] == true }
                            onQueueLinks(toQueue)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent),
                        border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Outlined.AddCircleOutline, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Queue $selectedCount", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            val toDownload = filteredLinks.filter { selectedUrls[it.url] == true }
                            onDownloadLinks(toDownload)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurpleAccent, contentColor = SpaceBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Filled.CloudDownload, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Start $selectedCount", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary) }
        }
    )
}

@Composable
private fun TypeFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .background(
                if (selected) PurpleAccent.copy(alpha = 0.18f) else ElevatedSurf,
                RoundedCornerShape(20.dp)
            )
            .border(
                1.dp,
                if (selected) PurpleAccent.copy(alpha = 0.6f) else BorderColor,
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            color = if (selected) PurpleAccent else TextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun typeLabelColor(typeLabel: String): Color = when (typeLabel) {
    "VIDEO"                    -> GreenSuccess
    "AUDIO"                    -> PurpleAccent
    "HLS", "DASH"              -> GreenSuccess
    "ARCHIVE"                  -> OrangeWarn
    "DOCUMENT"                 -> CyanPrimary
    "EBOOK"                    -> CyanPrimary
    "IMAGE"                    -> PurpleAccent
    "SUBTITLE"                 -> TextSecondary
    "TORRENT", "MAGNET"        -> RedError
    "EXECUTABLE"               -> RedError
    "SPREADSHEET", "PRESENTATION" -> OrangeWarn
    "FONT", "CODE"             -> TextSecondary
    else                       -> TextSecondary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HADTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isRequired: Boolean = false,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(if (isRequired) "$label *" else label, color = TextSecondary, fontSize = 12.sp) },
        placeholder = { Text(placeholder, color = TextMuted, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp), tint = CyanDim) },
        singleLine = singleLine,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanPrimary, unfocusedBorderColor = BorderColor,
            focusedLabelColor = CyanPrimary, cursorColor = CyanPrimary,
            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
            unfocusedContainerColor = ElevatedSurf, focusedContainerColor = ElevatedSurf
        ),
        shape = RoundedCornerShape(10.dp),
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
    )
}

@Composable
fun ToggleChip(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .clickable { onToggle(!checked) }
            .background(
                if (checked) CyanPrimary.copy(alpha = 0.12f) else ElevatedSurf,
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (checked) CyanPrimary.copy(alpha = 0.4f) else BorderColor,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Switch(
            checked = checked, onCheckedChange = onToggle,
            modifier = Modifier.scale(0.7f).size(24.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = SpaceBlack,
                checkedTrackColor = CyanPrimary,
                uncheckedTrackColor = BorderColor
            )
        )
        Text(
            label,
            color = if (checked) CyanPrimary else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .background(
                if (selected) PurpleAccent.copy(alpha = 0.15f) else ElevatedSurf,
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                if (selected) PurpleAccent.copy(alpha = 0.6f) else BorderColor,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            label,
            color = if (selected) PurpleAccent else TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}