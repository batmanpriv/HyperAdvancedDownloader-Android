package com.had.downloader.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.had.downloader.data.model.*
import com.had.downloader.ui.screens.CountdownBadge
import com.had.downloader.ui.theme.*
import androidx.compose.ui.geometry.CornerRadius
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GlowProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = CyanPrimary,
    trackColor: Color = BorderColor
) {
    val animProg by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "progress"
    )
    Canvas(modifier = modifier.height(6.dp)) {
        drawRoundRect(color = trackColor, cornerRadius = CornerRadius(3.dp.toPx()), size = size)
        if (animProg > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(colors = listOf(color.copy(alpha = 0.7f), color)),
                cornerRadius = CornerRadius(3.dp.toPx()),
                size = size.copy(width = size.width * animProg)
            )
            drawRoundRect(
                color = color.copy(alpha = 0.25f),
                cornerRadius = CornerRadius(6.dp.toPx()),
                topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
                size = size.copy(width = size.width * animProg + 4.dp.toPx(), height = size.height + 4.dp.toPx())
            )
        }
    }
}

@Composable
fun StatusBadge(status: DownloadStatus) {
    val (label, color) = when (status) {
        DownloadStatus.DOWNLOADING -> "DOWNLOADING" to CyanPrimary
        DownloadStatus.COMPLETED   -> "DONE"        to GreenSuccess
        DownloadStatus.FAILED      -> "FAILED"      to RedError
        DownloadStatus.PAUSED      -> "PAUSED"      to OrangeWarn
        DownloadStatus.CANCELLED   -> "CANCELLED"   to RedError
        DownloadStatus.CONNECTING  -> "CONNECTING"  to PurpleAccent
        DownloadStatus.QUEUED      -> "QUEUED"      to OrangeWarn
        DownloadStatus.VERIFYING   -> "VERIFYING"   to OrangeWarn
        DownloadStatus.MERGING     -> "MERGING"     to PurpleAccent
    }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse"
    )
    val alpha = if (status == DownloadStatus.DOWNLOADING ||
        status == DownloadStatus.CONNECTING ||
        status == DownloadStatus.MERGING ||
        status == DownloadStatus.QUEUED) pulse else 1f

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = color.copy(alpha = alpha),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun DownloadCard(
    item: DownloadItem,
    chunkState: LiveChunkState?,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onDeleteWithFile: () -> Unit,
    onRetry: () -> Unit,
    onViewMode: (ThreadViewMode) -> Unit,
    onLongPress: () -> Unit = {},
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteMenu by remember { mutableStateOf(false) }

    val schedEpoch = remember(item.scheduleFrom) { item.scheduleFrom.toLongOrNull() }
    val isScheduled = schedEpoch != null && schedEpoch > System.currentTimeMillis()

    val selectionAnim by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(150),
        label = "sel"
    )

    val borderColor = when {
        isSelected -> CyanPrimary.copy(alpha = 0.8f)
        item.status == DownloadStatus.DOWNLOADING -> CyanPrimary.copy(alpha = 0.4f)
        item.status == DownloadStatus.COMPLETED   -> GreenSuccess.copy(alpha = 0.3f)
        item.status == DownloadStatus.FAILED      -> RedError.copy(alpha = 0.3f)
        item.status == DownloadStatus.CANCELLED   -> RedError.copy(alpha = 0.2f)
        item.status == DownloadStatus.QUEUED && isScheduled -> OrangeWarn.copy(alpha = 0.35f)
        else -> BorderColor
    }

    val cardBg = if (isSelected) CyanPrimary.copy(alpha = 0.06f) else SurfaceDark

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(item.id, selectionMode) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = {
                        if (selectionMode) {
                            onTap()
                        } else {
                            expanded = !expanded
                        }
                    }
                )
            }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        tonalElevation = 0.dp
    ) {
        Column(Modifier.padding(16.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (selectionMode) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (isSelected) CyanPrimary else BorderColor,
                                CircleShape
                            )
                            .border(2.dp,
                                if (isSelected) CyanPrimary else TextMuted,
                                CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Filled.Check, null,
                                tint = SpaceBlack,
                                modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                }

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(PurpleAccent.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(item.modeLabel(), color = PurpleAccent, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = item.displayName(),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.url,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextMuted
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusBadge(item.status)
            }

            Spacer(Modifier.height(10.dp))

            if (item.status == DownloadStatus.QUEUED && isScheduled && schedEpoch != null) {
                CountdownBadge(epochMillis = schedEpoch, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
            }

            if (item.status == DownloadStatus.DOWNLOADING &&
                chunkState != null && chunkState.chunks.isNotEmpty()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${chunkState.chunks.size} threads  •  ${item.activeThreads} active",
                        color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                    )
                    ThreadViewModeSelector(current = chunkState.threadViewMode, onSelect = onViewMode)
                }
                ThreadVisualizer(
                    chunks = chunkState.chunks,
                    totalBytes = item.totalBytes,
                    mode = chunkState.threadViewMode,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
            } else {
                val progColor = when (item.status) {
                    DownloadStatus.COMPLETED -> GreenSuccess
                    DownloadStatus.FAILED    -> RedError
                    DownloadStatus.CANCELLED -> RedError
                    DownloadStatus.MERGING   -> PurpleAccent
                    DownloadStatus.VERIFYING -> OrangeWarn
                    DownloadStatus.QUEUED    -> if (isScheduled) OrangeWarn else TextMuted
                    else -> CyanPrimary
                }
                GlowProgressBar(progress = item.progress, color = progColor, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (item.totalBytes > 0)
                        "${item.downloadedBytes.toHumanSize()} / ${item.totalBytes.toHumanSize()}"
                    else if (item.downloadedBytes > 0) item.downloadedBytes.toHumanSize()
                    else "--",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (item.status == DownloadStatus.DOWNLOADING && item.speedBps > 0) {
                        Text("⚡ ${item.speedBps.toSpeedString()}", style = MaterialTheme.typography.bodySmall, color = CyanPrimary)
                    }
                    if (item.status == DownloadStatus.DOWNLOADING && item.etaSeconds >= 0) {
                        Text("⏱ ${item.etaSeconds.toEtaString()}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Text(
                        text = "${(item.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = when (item.status) {
                            DownloadStatus.COMPLETED -> GreenSuccess
                            DownloadStatus.FAILED    -> RedError
                            DownloadStatus.CANCELLED -> RedError
                            else -> CyanPrimary
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!item.errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.status == DownloadStatus.CANCELLED) OrangeWarn else RedError,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!selectionMode) {
                AnimatedVisibility(visible = expanded) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = BorderColor)
                        Spacer(Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            MetaChip(Icons.Outlined.AccountTree, "${item.threads}T")
                            if (!item.proxy.isNullOrBlank()) MetaChip(Icons.Outlined.VpnLock, "Proxy")
                            if (item.cookies.isNotBlank()) MetaChip(Icons.Outlined.Cookie, "Cookies")
                            if (item.checksumAlgo.isNotBlank()) MetaChip(Icons.Outlined.Verified, item.checksumAlgo)
                            if (item.mode == DownloadMode.HLS) MetaChip(Icons.Outlined.OndemandVideo, "HLS")
                            if (item.mirrors.isNotBlank()) MetaChip(Icons.Outlined.Hub, "Mirrors")
                            if (schedEpoch != null) {
                                val fmt = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                                MetaChip(Icons.Outlined.Schedule, fmt.format(Date(schedEpoch)))
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            when (item.status) {
                                DownloadStatus.DOWNLOADING,
                                DownloadStatus.CONNECTING,
                                DownloadStatus.MERGING,
                                DownloadStatus.VERIFYING -> {
                                    ActionButton("Stop", Icons.Filled.Stop, OrangeWarn, onStop)
                                }
                                DownloadStatus.QUEUED -> {
                                    ActionButton("Start Now", Icons.Filled.PlayArrow, CyanPrimary, onRetry)
                                }
                                DownloadStatus.FAILED,
                                DownloadStatus.CANCELLED -> {
                                    ActionButton("Retry", Icons.Filled.Refresh, CyanPrimary, onRetry)
                                }
                                else -> {}
                            }

                            Box {
                                ActionButton("Delete", Icons.Filled.Delete, RedError) {
                                    showDeleteMenu = true
                                }
                                DropdownMenu(
                                    expanded = showDeleteMenu,
                                    onDismissRequest = { showDeleteMenu = false },
                                    modifier = Modifier.background(SurfaceDark).border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("Remove from list", color = TextPrimary, fontSize = 13.sp)
                                                Text("Keep file on disk", color = TextMuted, fontSize = 10.sp)
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = OrangeWarn, modifier = Modifier.size(18.dp)) },
                                        onClick = { showDeleteMenu = false; onDelete() }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("Delete with file", color = RedError, fontSize = 13.sp)
                                                Text("Permanently remove file", color = TextMuted, fontSize = 10.sp)
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Filled.DeleteForever, null, tint = RedError, modifier = Modifier.size(18.dp)) },
                                        onClick = { showDeleteMenu = false; onDeleteWithFile() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(ElevatedSurf, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = TextSecondary)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StatCard(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
fun SelectionActionBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDelete: () -> Unit,
    onDeleteWithFiles: () -> Unit
) {
    var showDeleteMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ElevatedSurf,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onClearSelection, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Close, null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
            }

            Text(
                "$selectedCount selected",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = onSelectAll,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    if (selectedCount == totalCount) "None" else "All",
                    color = CyanPrimary,
                    fontSize = 12.sp
                )
            }

            Box {
                Button(
                    onClick = { showDeleteMenu = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                DropdownMenu(
                    expanded = showDeleteMenu,
                    onDismissRequest = { showDeleteMenu = false },
                    modifier = Modifier
                        .background(SurfaceDark)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("Remove $selectedCount from list", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("Files remain on disk", color = TextMuted, fontSize = 10.sp)
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, null, tint = OrangeWarn, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            showDeleteMenu = false
                            onDelete()
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("Delete $selectedCount with files", color = RedError, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("Permanently removes downloaded files", color = TextMuted, fontSize = 10.sp)
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.DeleteForever, null, tint = RedError, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            showDeleteMenu = false
                            onDeleteWithFiles()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LogConsole(logs: List<LogEntry>, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    LaunchedEffect(logs.size) { scrollState.animateScrollTo(scrollState.maxValue) }

    Box(
        modifier = modifier
            .background(Color(0xFF050810), RoundedCornerShape(12.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        if (logs.isEmpty()) {
            Text("> Waiting for output...", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        } else {
            Column(Modifier.verticalScroll(scrollState)) {
                logs.forEach { entry ->
                    val color = when (entry.tag) {
                        "ERR"  -> RedError
                        "SYS"  -> PurpleAccent
                        "INFO" -> CyanPrimary
                        else   -> GreenSuccess
                    }
                    Text(
                        "[${entry.tag}] ${entry.message}",
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}