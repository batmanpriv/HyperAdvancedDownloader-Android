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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.had.downloader.data.model.toHumanSize
import com.had.downloader.data.model.toSpeedString
import com.had.downloader.service.*
import com.had.downloader.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun TorrentTab(vm: MainViewModel) {
    val torrentState by vm.torrentState.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showMagnetDialog by remember { mutableStateOf(false) }
    var selectedTorrent by remember { mutableStateOf<TorrentProgress?>(null) }

    val torrentFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            if (bytes != null) vm.parseTorrentFile(bytes)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(SpaceBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TorrentStatsHeader(torrentState)

            if (torrentState.active.isEmpty() && torrentState.completed.isEmpty() && torrentState.failed.isEmpty()) {
                TorrentEmptyState(
                    onAddTorrent = { torrentFilePicker.launch("application/x-bittorrent") },
                    onAddMagnet = { showMagnetDialog = true }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 14.dp, end = 14.dp, top = 8.dp, bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (torrentState.active.isNotEmpty()) {
                        item {
                            SectionLabel("ACTIVE  •  ${torrentState.active.size}", CyanPrimary)
                        }
                        items(torrentState.active, key = { it.infoHashHex }) { progress ->
                            TorrentCard(
                                progress = progress,
                                onStop   = { vm.stopTorrent(progress.infoHashHex) },
                                onDetails = { selectedTorrent = progress }
                            )
                        }
                    }
                    if (torrentState.completed.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            SectionLabel("COMPLETED  •  ${torrentState.completed.size}", GreenSuccess)
                        }
                        items(torrentState.completed, key = { it.infoHashHex }) { progress ->
                            TorrentCard(
                                progress = progress,
                                onStop   = {},
                                onDetails = { selectedTorrent = progress }
                            )
                        }
                    }
                    if (torrentState.failed.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            SectionLabel("FAILED  •  ${torrentState.failed.size}", RedError)
                        }
                        items(torrentState.failed, key = { it.infoHashHex }) { progress ->
                            TorrentCard(
                                progress = progress,
                                onStop   = {},
                                onDetails = { selectedTorrent = progress }
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            SmallFloatingActionButton(
                onClick = { showMagnetDialog = true },
                containerColor = PurpleAccent,
                contentColor   = SpaceBlack,
                shape = CircleShape
            ) {
                Icon(Icons.Outlined.Link, "Magnet", modifier = Modifier.size(20.dp))
            }
            FloatingActionButton(
                onClick = { torrentFilePicker.launch("application/x-bittorrent") },
                containerColor = CyanPrimary,
                contentColor   = SpaceBlack,
                shape = CircleShape,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(Icons.Filled.Add, "Add Torrent", modifier = Modifier.size(28.dp))
            }
        }
    }

    if (showMagnetDialog) {
        MagnetInputDialog(
            onConfirm = { magnet ->
                vm.startMagnetDownload(magnet)
                showMagnetDialog = false
            },
            onDismiss = { showMagnetDialog = false }
        )
    }

    selectedTorrent?.let { prog ->
        TorrentDetailSheet(
            progress = prog,
            onDismiss = { selectedTorrent = null },
            onFileSelectionChanged = { idx, sel ->
                vm.updateTorrentFileSelection(prog.infoHashHex, idx, sel)
            },
            onStop = {
                vm.stopTorrent(prog.infoHashHex)
                selectedTorrent = null
            }
        )
    }

    val parsedInfo by vm.parsedTorrentInfo.collectAsState()
    parsedInfo?.let { info ->
        TorrentStartDialog(
            info = info,
            onStart = { selectedFiles ->
                vm.startTorrentDownload(info, selectedFiles)
            },
            onDismiss = { vm.clearParsedTorrent() }
        )
    }
}

@Composable
private fun TorrentStatsHeader(state: TorrentUiState) {
    val totalDown = state.active.sumOf { it.downloadedBytes } +
            state.completed.sumOf { it.downloadedBytes }
    val totalSpeed = state.active.sumOf { it.speedBps }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TorrentStatPill("${state.active.size}", "Active", CyanPrimary, Modifier.weight(1f))
        TorrentStatPill("${state.completed.size}", "Done", GreenSuccess, Modifier.weight(1f))
        TorrentStatPill(totalDown.toHumanSize(), "Total", PurpleAccent, Modifier.weight(1f))
        TorrentStatPill(totalSpeed.toSpeedString(), "Speed", OrangeWarn, Modifier.weight(1f))
    }
}

@Composable
private fun TorrentStatPill(value: String, label: String, color: Color, modifier: Modifier) {
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
private fun SectionLabel(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
    )
}

@Composable
fun TorrentCard(
    progress: TorrentProgress,
    onStop: () -> Unit,
    onDetails: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when {
        progress.status == "COMPLETED" -> GreenSuccess
        progress.status.startsWith("FAILED") -> RedError
        progress.status == "FETCHING_METADATA" -> PurpleAccent
        else -> CyanPrimary
    }

    val borderColor = when {
        progress.status == "COMPLETED" -> GreenSuccess.copy(alpha = 0.3f)
        progress.status.startsWith("FAILED") -> RedError.copy(alpha = 0.3f)
        else -> CyanPrimary.copy(alpha = 0.3f)
    }

    val glowAlpha by rememberInfiniteTransition(label = "tglow").animateFloat(
        initialValue = 0.3f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "tg"
    )
    val isActive = progress.status == "DOWNLOADING" || progress.status == "CONNECTING"

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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                progress.status == "COMPLETED" -> Icons.Filled.CheckCircle
                                progress.status.startsWith("FAILED") -> Icons.Filled.ErrorOutline
                                else -> Icons.Outlined.CloudDownload
                            },
                            null,
                            tint = statusColor.copy(alpha = if (isActive) glowAlpha else 1f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            progress.name,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            progress.infoHashHex.take(16) + "...",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                TorrentStatusBadge(progress.status)
            }

            Spacer(Modifier.height(10.dp))

            TorrentProgressBar(progress)

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${progress.downloadedBytes.toHumanSize()} / ${progress.totalBytes.toHumanSize()}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (progress.speedBps > 0) {
                        Text(
                            "↓ ${progress.speedBps.toSpeedString()}",
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (progress.connectedPeers > 0) {
                        Text(
                            "⇄ ${progress.connectedPeers}",
                            color = PurpleAccent,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        "${(progress.percent * 100).toInt()}%",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip("${progress.pieces} pieces", Icons.Outlined.Layers, TextSecondary)
                        InfoChip("${progress.completedPieces} done", Icons.Outlined.CheckCircle, GreenSuccess)
                        if (progress.eta > 0) {
                            InfoChip(progress.eta.toEtaStr(), Icons.Outlined.Timer, OrangeWarn)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    if (progress.files.isNotEmpty()) {
                        Text(
                            "FILES (${progress.files.size})",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        progress.files.take(5).forEach { file ->
                            FileRowCompact(file)
                        }
                        if (progress.files.size > 5) {
                            Text(
                                "+${progress.files.size - 5} more files",
                                color = TextMuted,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isActive) {
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
                        OutlinedButton(
                            onClick = onDetails,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
                            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Outlined.Info, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Details", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TorrentProgressBar(progress: TorrentProgress) {
    val animPct by animateFloatAsState(
        targetValue = progress.percent.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "tpct"
    )
    val glowAlpha by rememberInfiniteTransition(label = "tpb").animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "tpbg"
    )
    val isActive = progress.status == "DOWNLOADING"
    val barColor = when {
        progress.status == "COMPLETED" -> GreenSuccess
        progress.status.startsWith("FAILED") -> RedError
        else -> CyanPrimary
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
        val h = size.height
        val w = size.width
        drawRoundRect(color = ElevatedSurf, cornerRadius = CornerRadius(4f), size = size)
        if (animPct > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        barColor.copy(alpha = if (isActive) glowAlpha * 0.7f else 0.7f),
                        barColor.copy(alpha = if (isActive) glowAlpha else 1f)
                    )
                ),
                cornerRadius = CornerRadius(4f),
                size = size.copy(width = w * animPct)
            )
        }
        if (progress.pieces > 0 && progress.completedPieces > 0) {
            val pieceW = w / progress.pieces
            repeat(minOf(progress.completedPieces, progress.pieces)) { i ->
                drawRect(
                    color = Color.White.copy(alpha = 0.07f),
                    topLeft = Offset(i * pieceW, 0f),
                    size = Size(pieceW - 0.5f, h)
                )
            }
        }
    }
}

@Composable
private fun TorrentStatusBadge(status: String) {
    val (label, color) = when {
        status == "COMPLETED"          -> "DONE"       to GreenSuccess
        status.startsWith("FAILED")    -> "FAILED"     to RedError
        status == "FETCHING_METADATA"  -> "METADATA"   to PurpleAccent
        status == "CONNECTING"         -> "CONNECTING" to PurpleAccent
        status == "DOWNLOADING"        -> "DL"         to CyanPrimary
        else -> status.take(8) to TextMuted
    }
    val pulse by rememberInfiniteTransition(label = "tsb").animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "tsbp"
    )
    val isAnimated = status == "DOWNLOADING" || status == "CONNECTING" || status == "FETCHING_METADATA"
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            label,
            color = color.copy(alpha = if (isAnimated) pulse else 1f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun FileRowCompact(file: TorrentFile) {
    val ext = file.path.substringAfterLast('.').lowercase().take(4)
    val extColor = when (ext) {
        "mp4", "mkv", "avi", "mov" -> GreenSuccess
        "mp3", "flac", "aac"       -> PurpleAccent
        "zip", "rar", "7z"         -> OrangeWarn
        "pdf", "doc", "docx"       -> CyanPrimary
        else -> TextSecondary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(extColor.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(ext.uppercase(), color = extColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
        }
        Text(
            file.path.substringAfterLast('/'),
            color = if (file.selected) TextSecondary else TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            file.length.toHumanSize(),
            color = TextMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        if (!file.selected) {
            Icon(Icons.Outlined.PauseCircle, null,
                tint = TextMuted, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun InfoChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(11.dp))
        Text(label, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun TorrentEmptyState(onAddTorrent: () -> Unit, onAddMagnet: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Outlined.CloudDownload,
                null,
                modifier = Modifier.size(64.dp),
                tint = TextMuted
            )
            Text("No torrents yet", color = TextMuted, fontSize = 16.sp)
            Text(
                "Add a .torrent file or paste a magnet link",
                color = TextMuted.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onAddTorrent,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary, contentColor = SpaceBlack
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Torrent File", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onAddMagnet,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent),
                    border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Outlined.Link, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Magnet")
                }
            }
        }
    }
}

@Composable
fun MagnetInputDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Link, null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Magnet Link", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("magnet:?xt=urn:btih:...", color = TextSecondary, fontSize = 12.sp) },
                singleLine = false,
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleAccent,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    unfocusedContainerColor = ElevatedSurf,
                    focusedContainerColor = ElevatedSurf
                ),
                shape = RoundedCornerShape(10.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.startsWith("magnet:")) onConfirm(text.trim()) },
                enabled = text.startsWith("magnet:"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurpleAccent, contentColor = SpaceBlack
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Start Download", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentStartDialog(
    info: com.had.downloader.service.TorrentInfo,
    onStart: (List<Boolean>) -> Unit,
    onDismiss: () -> Unit
) {
    val fileSelection = remember { mutableStateListOf(*Array(info.files.size) { true }) }
    val selectedSize = info.files.indices
        .filter { fileSelection[it] }
        .sumOf { info.files[it].length }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CloudDownload, null, tint = CyanPrimary,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(info.name, color = TextPrimary, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoBadge("${info.files.size} files", CyanPrimary)
                    InfoBadge(info.totalSize.toHumanSize(), PurpleAccent)
                    InfoBadge("${info.pieces.size} pieces", OrangeWarn)
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Select files to download",
                        color = TextSecondary, fontSize = 12.sp
                    )
                    Text(
                        selectedSize.toHumanSize(),
                        color = CyanPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { fileSelection.indices.forEach { fileSelection[it] = true } },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Select All", color = CyanPrimary, fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = { fileSelection.indices.forEach { fileSelection[it] = false } },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Deselect All", color = TextMuted, fontSize = 11.sp)
                    }
                }
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                Spacer(Modifier.height(4.dp))
                info.files.forEachIndexed { idx, file ->
                    TorrentFileRow(
                        file = file,
                        selected = fileSelection[idx],
                        onToggle = { fileSelection[idx] = it }
                    )
                    if (idx < info.files.size - 1) {
                        HorizontalDivider(
                            color = BorderColor.copy(alpha = 0.4f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
                if (info.comment.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElevatedSurf,
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text("Comment", color = TextMuted, fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace)
                            Text(info.comment, color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStart(fileSelection.toList()) },
                enabled = fileSelection.any { it },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary, contentColor = SpaceBlack
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Start (${selectedSize.toHumanSize()})", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun TorrentFileRow(file: TorrentFile, selected: Boolean, onToggle: (Boolean) -> Unit) {
    val ext = file.path.substringAfterLast('.').lowercase().take(5)
    val extColor = when (ext) {
        "mp4", "mkv", "avi", "mov", "webm" -> GreenSuccess
        "mp3", "flac", "aac", "opus"       -> PurpleAccent
        "zip", "rar", "7z", "tar"          -> OrangeWarn
        "pdf", "epub", "mobi"              -> CyanPrimary
        else -> TextSecondary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!selected) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(
                checkedColor = CyanPrimary,
                checkmarkColor = SpaceBlack,
                uncheckedColor = BorderColor
            ),
            modifier = Modifier.size(20.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                file.path.substringAfterLast('/').ifBlank { file.path },
                color = if (selected) TextPrimary else TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (file.path.contains('/')) {
                Text(
                    file.path.substringBeforeLast('/'),
                    color = TextMuted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .background(extColor.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(".${ext}", color = extColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            }
            Text(
                file.length.toHumanSize(),
                color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun InfoBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentDetailSheet(
    progress: TorrentProgress,
    onDismiss: () -> Unit,
    onFileSelectionChanged: (Int, Boolean) -> Unit,
    onStop: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(progress.name, color = TextPrimary, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElevatedSurf,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DetailRow("Hash", progress.infoHashHex, TextMuted)
                        DetailRow("Status", progress.status, CyanPrimary)
                        DetailRow("Downloaded", progress.downloadedBytes.toHumanSize(), GreenSuccess)
                        DetailRow("Total", progress.totalBytes.toHumanSize(), TextSecondary)
                        DetailRow("Speed", progress.speedBps.toSpeedString(), CyanPrimary)
                        DetailRow("Peers", "${progress.connectedPeers} connected", PurpleAccent)
                        DetailRow("Pieces", "${progress.completedPieces}/${progress.pieces}", OrangeWarn)
                        if (progress.eta > 0) {
                            DetailRow("ETA", progress.eta.toEtaStr(), OrangeWarn)
                        }
                    }
                }

                if (progress.peers.isNotEmpty()) {
                    Text("PEERS (${progress.peers.size})", color = TextMuted,
                        fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 150.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(progress.peers.take(20)) { peer ->
                            PeerRow(peer)
                        }
                    }
                }

                if (progress.files.isNotEmpty()) {
                    Text("FILES (${progress.files.size})", color = TextMuted,
                        fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                    progress.files.forEachIndexed { idx, file ->
                        TorrentFileRow(
                            file = file,
                            selected = file.selected,
                            onToggle = { onFileSelectionChanged(idx, it) }
                        )
                        if (idx < progress.files.size - 1) {
                            HorizontalDivider(color = BorderColor.copy(alpha = 0.4f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (progress.status == "DOWNLOADING" || progress.status == "CONNECTING") {
                    OutlinedButton(
                        onClick = onStop,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError),
                        border = BorderStroke(1.dp, RedError.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Stop, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Stop", fontSize = 12.sp)
                    }
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary, contentColor = SpaceBlack
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextMuted, fontSize = 11.sp)
        Text(
            value, color = valueColor, fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 200.dp)
        )
    }
}

@Composable
private fun PeerRow(peer: PeerInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ElevatedSurf, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (!peer.choking) GreenSuccess else TextMuted,
                        CircleShape
                    )
            )
            Text(
                "${peer.ip}:${peer.port}",
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            if (peer.downloadSpeed > 0) peer.downloadSpeed.toSpeedString() else "idle",
            color = if (peer.downloadSpeed > 0) CyanPrimary else TextMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun Int.toEtaStr(): String = when {
    this < 0    -> "--"
    this < 60   -> "${this}s"
    this < 3600 -> "${this / 60}m ${this % 60}s"
    else        -> "${this / 3600}h ${(this % 3600) / 60}m"
}