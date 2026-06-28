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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.had.downloader.data.model.toHumanSize
import com.had.downloader.service.ArchiveCrawlMode
import com.had.downloader.service.ArchiveProgress
import com.had.downloader.service.ArchiveSession
import com.had.downloader.ui.theme.*

@Composable
fun WebArchiveTab(vm: MainViewModel) {
    val archiveSessions by vm.archiveSessions.collectAsState()
    val archiveProgress by vm.archiveProgress.collectAsState()
    var showNewDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(SpaceBlack)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PurpleAccent.copy(alpha = 0.04f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.2f),
                    radius = size.width * 0.7f
                )
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            ArchiveStatsHeader(sessions = archiveSessions, progress = archiveProgress)

            if (archiveSessions.isEmpty()) {
                ArchiveEmptyState(onNew = { showNewDialog = true })
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(archiveSessions, key = { it.id }) { session ->
                        val prog = archiveProgress[session.id] ?: session.progress
                        ArchiveSessionCard(
                            session = session,
                            progress = prog,
                            onStop = { vm.stopArchive(session.id) },
                            onDelete = { vm.deleteArchive(session.id) },
                            onOpenFolder = { vm.openArchiveFolder(session) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showNewDialog = true },
            containerColor = PurpleAccent,
            contentColor = SpaceBlack,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 24.dp)
                .size(60.dp)
        ) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(28.dp))
        }
    }

    if (showNewDialog) {
        NewArchiveDialog(
            onStart = { config ->
                vm.startArchive(config)
                showNewDialog = false
            },
            onDismiss = { showNewDialog = false }
        )
    }
}

@Composable
private fun ArchiveStatsHeader(
    sessions: List<ArchiveSession>,
    progress: Map<Long, ArchiveProgress>
) {
    val totalPages = sessions.sumOf { progress[it.id]?.pages ?: it.progress.pages }
    val totalAssets = sessions.sumOf { progress[it.id]?.assets ?: it.progress.assets }
    val totalBytes = sessions.sumOf { progress[it.id]?.bytes ?: it.progress.bytes }
    val active = sessions.count { (progress[it.id]?.status ?: it.progress.status) == "RUNNING" }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ArchiveStatPill("$active", "Active", CyanPrimary, Modifier.weight(1f))
        ArchiveStatPill("$totalPages", "Pages", GreenSuccess, Modifier.weight(1f))
        ArchiveStatPill("$totalAssets", "Assets", PurpleAccent, Modifier.weight(1f))
        ArchiveStatPill(totalBytes.toHumanSize(), "Saved", OrangeWarn, Modifier.weight(1f))
    }
}

@Composable
private fun ArchiveStatPill(value: String, label: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun ArchiveEmptyState(onNew: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Outlined.Archive, null, modifier = Modifier.size(64.dp), tint = TextMuted)
            Text("No archives yet", color = TextMuted, fontSize = 16.sp)
            Text(
                "Backup any website for offline use",
                color = TextMuted.copy(alpha = 0.6f), fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNew,
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent, contentColor = SpaceBlack),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("New Archive", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ArchiveSessionCard(
    session: ArchiveSession,
    progress: ArchiveProgress,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onOpenFolder: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isRunning = progress.status == "RUNNING"
    val isFailed = progress.status.startsWith("FAILED")
    val isDone = progress.status == "COMPLETED"

    val borderColor = when {
        isRunning -> PurpleAccent.copy(alpha = 0.4f)
        isDone -> GreenSuccess.copy(alpha = 0.3f)
        isFailed -> RedError.copy(alpha = 0.3f)
        else -> BorderColor
    }

    val pulse by rememberInfiniteTransition(label = "apulse").animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "ap"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                when {
                                    isDone -> GreenSuccess.copy(alpha = 0.1f)
                                    isFailed -> RedError.copy(alpha = 0.1f)
                                    else -> PurpleAccent.copy(alpha = 0.1f)
                                }, RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                isDone -> Icons.Outlined.CheckCircle
                                isFailed -> Icons.Outlined.ErrorOutline
                                else -> Icons.Outlined.Archive
                            },
                            null,
                            tint = when {
                                isDone -> GreenSuccess
                                isFailed -> RedError
                                else -> PurpleAccent.copy(alpha = if (isRunning) pulse else 1f)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(Modifier.weight(1f)) {
                        val host = runCatching { java.net.URL(session.config.targetUrl).host }.getOrDefault(session.config.targetUrl)
                        Text(
                            host,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            session.config.targetUrl,
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))
                ArchiveStatusBadge(progress.status)
            }

            Spacer(Modifier.height(10.dp))

            if (isRunning && progress.currentUrl.isNotBlank()) {
                Text(
                    "↳ ${progress.currentUrl.substringAfterLast('/').take(40)}",
                    color = PurpleAccent.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            if (isRunning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = PurpleAccent,
                    trackColor = BorderColor
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArchiveInfoChip("${progress.pages}", "pages", GreenSuccess)
                    ArchiveInfoChip("${progress.assets}", "assets", CyanPrimary)
                    ArchiveInfoChip(progress.bytes.toHumanSize(), "saved", PurpleAccent)
                }
                if (progress.errors > 0) {
                    Text("${progress.errors} err", color = RedError, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ArchiveModeChip(
                            if (session.config.mode == ArchiveCrawlMode.SINGLE_PAGE) "Single Page" else "Full Site",
                            PurpleAccent
                        )
                        if (session.config.mode == ArchiveCrawlMode.FULL_SITE)
                            ArchiveModeChip("Max ${session.config.maxPages} pages", TextSecondary)
                        ArchiveModeChip("${session.config.concurrency} workers", CyanPrimary)
                        if (session.config.minifyOutput) ArchiveModeChip("Minify", OrangeWarn)
                        if (session.config.downloadExternal) ArchiveModeChip("External", OrangeWarn)
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isRunning) {
                            OutlinedButton(
                                onClick = onStop,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeWarn),
                                border = BorderStroke(1.dp, OrangeWarn.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Stop, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Stop", fontSize = 12.sp)
                            }
                        }
                        if (isDone) {
                            OutlinedButton(
                                onClick = onOpenFolder,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenSuccess),
                                border = BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Outlined.FolderOpen, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Open", fontSize = 12.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError),
                            border = BorderStroke(1.dp, RedError.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Delete, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveInfoChip(value: String, label: String, color: Color) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.07f), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(label, color = TextMuted, fontSize = 9.sp)
    }
}

@Composable
private fun ArchiveModeChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ArchiveStatusBadge(status: String) {
    val (label, color) = when {
        status == "COMPLETED" -> "DONE" to GreenSuccess
        status.startsWith("FAILED") -> "FAILED" to RedError
        status == "RUNNING" -> "RUNNING" to PurpleAccent
        else -> "IDLE" to TextMuted
    }
    val pulse by rememberInfiniteTransition(label = "asb").animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "asbp"
    )
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            label,
            color = color.copy(alpha = if (status == "RUNNING") pulse else 1f),
            fontSize = 9.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, letterSpacing = 1.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewArchiveDialog(
    onStart: (com.had.downloader.service.ArchiveConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var url by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(ArchiveCrawlMode.SINGLE_PAGE) }
    var maxPages by remember { mutableStateOf("100") }
    var concurrency by remember { mutableStateOf("5") }
    var outputDir by remember { mutableStateOf("") }
    var downloadExternal by remember { mutableStateOf(false) }
    var externalDomains by remember { mutableStateOf("") }
    var cookies by remember { mutableStateOf("") }
    var headers by remember { mutableStateOf("") }
    var userAgent by remember { mutableStateOf("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36") }
    var timeoutSec by remember { mutableStateOf("30") }
    var retries by remember { mutableStateOf("3") }
    var minify by remember { mutableStateOf(false) }
    var crawlIframes by remember { mutableStateOf(true) }
    var crawlHashRoutes by remember { mutableStateOf(true) }
    var followMeta by remember { mutableStateOf(true) }
    var maxAssetMb by remember { mutableStateOf("100") }
    var showAdvanced by remember { mutableStateOf(false) }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let { outputDir = it.path ?: outputDir }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Archive, null, tint = PurpleAccent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("New Web Archive", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("Website URL *", color = TextSecondary, fontSize = 12.sp) },
                    placeholder = { Text("https://example.com", color = TextMuted, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Language, null, modifier = Modifier.size(18.dp), tint = CyanDim) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleAccent, unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        unfocusedContainerColor = ElevatedSurf, focusedContainerColor = ElevatedSurf
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        ArchiveCrawlMode.SINGLE_PAGE to "Single Page",
                        ArchiveCrawlMode.FULL_SITE to "Full Site"
                    ).forEach { (m, label) ->
                        val selected = mode == m
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { mode = m }
                                .background(
                                    if (selected) PurpleAccent.copy(alpha = 0.15f) else ElevatedSurf,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(1.dp,
                                    if (selected) PurpleAccent.copy(alpha = 0.6f) else BorderColor,
                                    RoundedCornerShape(8.dp))
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label,
                                color = if (selected) PurpleAccent else TextMuted,
                                fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                AnimatedVisibility(visible = mode == ArchiveCrawlMode.FULL_SITE) {
                    OutlinedTextField(
                        value = maxPages, onValueChange = { maxPages = it },
                        label = { Text("Max pages", color = TextSecondary, fontSize = 12.sp) },
                        placeholder = { Text("100", color = TextMuted, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Outlined.Pages, null, modifier = Modifier.size(18.dp), tint = CyanDim) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleAccent, unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            unfocusedContainerColor = ElevatedSurf, focusedContainerColor = ElevatedSurf
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = outputDir, onValueChange = { outputDir = it },
                    label = { Text("Save folder", color = TextSecondary, fontSize = 12.sp) },
                    placeholder = { Text("/sdcard/Downloads/HAD/archives", color = TextMuted, fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Outlined.FolderOpen, null, modifier = Modifier.size(18.dp), tint = CyanDim) },
                    trailingIcon = {
                        IconButton(onClick = { folderLauncher.launch(null) }) {
                            Icon(Icons.Outlined.Folder, null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                        }
                    },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleAccent, unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        unfocusedContainerColor = ElevatedSurf, focusedContainerColor = ElevatedSurf
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

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
                        null, tint = PurpleAccent, modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Advanced Options", color = PurpleAccent, fontSize = 13.sp)
                }

                AnimatedVisibility(visible = showAdvanced) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = concurrency, onValueChange = { concurrency = it },
                                label = { Text("Workers", color = TextSecondary, fontSize = 11.sp) },
                                placeholder = { Text("5", color = TextMuted, fontSize = 11.sp) },
                                singleLine = true, modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurpleAccent, unfocusedBorderColor = BorderColor,
                                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                    unfocusedContainerColor = ElevatedSurf, focusedContainerColor = ElevatedSurf
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = timeoutSec, onValueChange = { timeoutSec = it },
                                label = { Text("Timeout (s)", color = TextSecondary, fontSize = 11.sp) },
                                placeholder = { Text("30", color = TextMuted, fontSize = 11.sp) },
                                singleLine = true, modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurpleAccent, unfocusedBorderColor = BorderColor,
                                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                    unfocusedContainerColor = ElevatedSurf, focusedContainerColor = ElevatedSurf
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = retries, onValueChange = { retries = it },
                                label = { Text("Retries", color = TextSecondary, fontSize = 11.sp) },
                                placeholder = { Text("3", color = TextMuted, fontSize = 11.sp) },
                                singleLine = true, modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurpleAccent, unfocusedBorderColor = BorderColor,
                                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                    unfocusedContainerColor = ElevatedSurf, focusedContainerColor = ElevatedSurf
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        OutlinedTextField(
                            value = maxAssetMb, onValueChange = { maxAssetMb = it },
                            label = { Text("Max asset size (MB, 0=unlimited)", color = TextSecondary, fontSize = 11.sp) },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleAccent, unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                unfocusedContainerColor = ElevatedSurf, focusedContainerColor = ElevatedSurf
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = cookies, onValueChange = { cookies = it },
                            label = { Text("Cookies (name=value; ...)", color = TextSecondary, fontSize = 11.sp) },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleAccent, unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                unfocusedContainerColor = ElevatedSurf, focusedContainerColor = ElevatedSurf
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = headers, onValueChange = { headers = it },
                            label = { Text("Headers (Key: Value, one per line)", color = TextSecondary, fontSize = 11.sp) },
                            singleLine = false, minLines = 2, modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleAccent, unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                unfocusedContainerColor = ElevatedSurf, focusedContainerColor = ElevatedSurf
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = userAgent, onValueChange = { userAgent = it },
                            label = { Text("User-Agent", color = TextSecondary, fontSize = 11.sp) },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleAccent, unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                unfocusedContainerColor = ElevatedSurf, focusedContainerColor = ElevatedSurf
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        AnimatedVisibility(visible = downloadExternal) {
                            OutlinedTextField(
                                value = externalDomains, onValueChange = { externalDomains = it },
                                label = { Text("Allowed external domains (comma separated)", color = TextSecondary, fontSize = 11.sp) },
                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurpleAccent, unfocusedBorderColor = BorderColor,
                                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                    unfocusedContainerColor = ElevatedSurf, focusedContainerColor = ElevatedSurf
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ArchiveToggle("Download external assets", downloadExternal) { downloadExternal = it }
                            ArchiveToggle("Minify HTML output", minify) { minify = it }
                            ArchiveToggle("Crawl iframes", crawlIframes) { crawlIframes = it }
                            ArchiveToggle("Handle hash routes (SPA)", crawlHashRoutes) { crawlHashRoutes = it }
                            ArchiveToggle("Follow meta-refresh", followMeta) { followMeta = it }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cookieMap = mutableMapOf<String, String>()
                    cookies.split(";").forEach { part ->
                        val kv = part.trim().split("=", limit = 2)
                        if (kv.size == 2) cookieMap[kv[0].trim()] = kv[1].trim()
                    }
                    val headerMap = mutableMapOf<String, String>()
                    headers.lines().forEach { line ->
                        val idx = line.indexOf(':')
                        if (idx > 0) headerMap[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
                    }
                    val extDomList = externalDomains.split(",").map { it.trim() }.filter { it.isNotBlank() }

                    val saveDir = outputDir.trim().ifBlank {
                        android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOWNLOADS
                        ).absolutePath + "/HAD/archives"
                    }

                    onStart(
                        com.had.downloader.service.ArchiveConfig(
                            targetUrl = url.trim(),
                            outputDir = saveDir,
                            mode = mode,
                            maxPages = maxPages.toIntOrNull()?.coerceIn(1, 10000) ?: 100,
                            concurrency = concurrency.toIntOrNull()?.coerceIn(1, 20) ?: 5,
                            downloadExternal = downloadExternal,
                            externalDomains = extDomList,
                            cookies = cookieMap,
                            headers = headerMap,
                            userAgent = userAgent.trim().ifBlank { "Mozilla/5.0" },
                            timeoutMs = (timeoutSec.toIntOrNull() ?: 30) * 1000,
                            retries = retries.toIntOrNull()?.coerceIn(0, 10) ?: 3,
                            minifyOutput = minify,
                            maxAssetSizeBytes = (maxAssetMb.toLongOrNull() ?: 100L) * 1024L * 1024L,
                            crawlIframes = crawlIframes,
                            crawlHashRoutes = crawlHashRoutes,
                            followMetaRefresh = followMeta
                        )
                    )
                },
                enabled = url.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent, contentColor = SpaceBlack),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Outlined.Archive, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Start Archive", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun ArchiveToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Switch(
            checked = checked, onCheckedChange = onToggle,
            modifier = Modifier.scale(0.8f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = SpaceBlack,
                checkedTrackColor = PurpleAccent,
                uncheckedTrackColor = BorderColor
            )
        )
    }
}