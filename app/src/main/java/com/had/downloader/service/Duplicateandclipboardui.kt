package com.had.downloader.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.had.downloader.data.model.toHumanSize
import com.had.downloader.service.DuplicateAction
import com.had.downloader.service.DuplicateResult
import com.had.downloader.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DuplicateDialog(
    filename: String,
    url: String,
    result: DuplicateResult,
    onAction: (DuplicateAction) -> Unit,
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(OrangeWarn.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ContentCopy, null, tint = OrangeWarn, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("File Already Exists", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Duplicate detected", color = OrangeWarn, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElevatedSurf,
                    border = BorderStroke(1.dp, OrangeWarn.copy(alpha = 0.25f))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("EXISTING FILE", color = TextMuted, fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.InsertDriveFile, null, tint = OrangeWarn, modifier = Modifier.size(18.dp))
                            Text(filename, color = TextPrimary, fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f))
                        }

                        if (result.existingFileSize > 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                InfoPill("Size", result.existingFileSize.toHumanSize(), CyanPrimary)
                                result.existingCompletedAt?.let { ts ->
                                    InfoPill("Downloaded", sdf.format(Date(ts)), GreenSuccess)
                                }
                            }
                        }

                        if (!result.existingFilePath.isNullOrBlank()) {
                            Text(result.existingFilePath, color = TextMuted, fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                if (!result.suggestedNewName.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CyanPrimary.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.2f))
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.DriveFileRenameOutline, null, tint = CyanPrimary, modifier = Modifier.size(15.dp))
                            Column {
                                Text("Will be saved as:", color = TextMuted, fontSize = 10.sp)
                                Text(result.suggestedNewName, color = CyanPrimary, fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Text("CHOOSE ACTION", color = TextMuted, fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DuplicateActionButton(
                        icon = Icons.Outlined.DriveFileRenameOutline,
                        title = "Save with new name",
                        subtitle = result.suggestedNewName ?: "auto-rename",
                        color = CyanPrimary,
                        onClick = { onAction(DuplicateAction.RENAME) }
                    )
                    DuplicateActionButton(
                        icon = Icons.Outlined.Cached,
                        title = "Overwrite existing file",
                        subtitle = "Delete old file and re-download",
                        color = OrangeWarn,
                        onClick = { onAction(DuplicateAction.OVERWRITE) }
                    )
                    DuplicateActionButton(
                        icon = Icons.Outlined.Block,
                        title = "Skip this download",
                        subtitle = "Keep existing file, don't download",
                        color = TextMuted,
                        onClick = { onAction(DuplicateAction.SKIP) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun DuplicateActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextMuted, fontSize = 10.sp)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = color.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun InfoPill(label: String, value: String, color: Color) {
    Column {
        Text(label, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = color, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ClipboardDownloadDialog(
    urls: List<String>,
    onDownload: (List<String>) -> Unit,        
    onQueue: (List<String>) -> Unit,            
    onDismiss: () -> Unit
) {
    val isSingle = urls.size == 1

    val selected = remember(urls) { mutableStateMapOf<String, Boolean>().also { m -> urls.forEach { m[it] = true } } }
    val selectedCount = selected.values.count { it }
    val allSelected = urls.all { selected[it] == true }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).background(CyanPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ContentPaste, null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(
                        if (isSingle) "Download Detected" else "${urls.size} Links Detected",
                        color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                    Text("From clipboard", color = CyanPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                if (isSingle) {
                    
                    val url = urls.first()
                    val filename = url.substringAfterLast('/').substringBefore('?').ifBlank { url.take(40) }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ElevatedSurf,
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(filename, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(url, color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                } else {
                    
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Select links to download", color = TextSecondary, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                            urls.forEach { selected[it] = !allSelected }
                        }) {
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = { v -> urls.forEach { selected[it] = v } },
                                colors = CheckboxDefaults.colors(checkedColor = CyanPrimary, checkmarkColor = SpaceBlack, uncheckedColor = BorderColor),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("All", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                    Column(
                        modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        urls.forEach { url ->
                            val isSelected = selected[url] == true
                            val filename = url.substringAfterLast('/').substringBefore('?')
                                .ifBlank { url.substringAfter("://").take(30) }
                            val ext = filename.substringAfterLast('.').lowercase().take(5)
                            val extColor = extColor(ext)

                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { selected[url] = !isSelected },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) CyanPrimary.copy(alpha = 0.06f) else ElevatedSurf,
                                border = BorderStroke(1.dp, if (isSelected) CyanPrimary.copy(alpha = 0.3f) else BorderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { selected[url] = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = CyanPrimary, checkmarkColor = SpaceBlack, uncheckedColor = BorderColor
                                        ),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(filename, color = if (isSelected) TextPrimary else TextSecondary,
                                            fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            fontFamily = FontFamily.Monospace)
                                        Text(url, color = TextMuted, fontSize = 9.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (ext.isNotBlank() && !ext.contains('/') && !ext.contains('.')) {
                                        Box(Modifier.background(extColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)) {
                                            Text(".$ext", color = extColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedCount > 0) {
                        Text("$selectedCount link${if (selectedCount > 1) "s" else ""} selected",
                            color = CyanPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val toProcess = if (isSingle) urls else urls.filter { selected[it] == true }

                OutlinedButton(
                    onClick = { onQueue(toProcess); onDismiss() },
                    enabled = toProcess.isNotEmpty(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent),
                    border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Outlined.AddCircleOutline, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isSingle) "Queue" else "Queue ${toProcess.size}", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }

                Button(
                    onClick = { onDownload(toProcess); onDismiss() },
                    enabled = toProcess.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SpaceBlack),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isSingle) "Download" else "Download ${toProcess.size}",
                        fontWeight = FontWeight.Bold, fontSize = 12.sp
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Dismiss", color = TextSecondary) }
        }
    )
}

private fun extColor(ext: String): Color = when (ext) {
    "mp4", "mkv", "avi", "mov", "webm", "m3u8", "mpd" -> GreenSuccess
    "mp3", "flac", "aac", "m4a", "ogg", "opus"         -> PurpleAccent
    "zip", "rar", "7z", "tar", "gz"                    -> OrangeWarn
    "pdf", "epub", "mobi", "doc", "docx"               -> CyanPrimary
    "torrent"                                           -> RedError
    "apk", "exe", "dmg"                                -> RedError
    else                                               -> TextSecondary
}

@Composable
fun RemoteServerPanel(
    isRunning: Boolean,
    ipAddress: String,
    port: Int,
    requestCount: Int,
    lastRequest: String,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, if (isRunning) GreenSuccess.copy(alpha = 0.35f) else BorderColor)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Wifi, null, tint = if (isRunning) GreenSuccess else TextMuted, modifier = Modifier.size(18.dp))
                    Text("Remote Download Server", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Switch(
                    checked = isRunning,
                    onCheckedChange = { if (it) onStart() else onStop() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SpaceBlack,
                        checkedTrackColor = GreenSuccess,
                        uncheckedTrackColor = BorderColor
                    )
                )
            }

            if (isRunning) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GreenSuccess.copy(alpha = 0.07f),
                    border = BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.2f))
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("OPEN FROM PC BROWSER:", color = TextMuted, fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                        Text(
                            "http://$ipAddress:$port",
                            color = GreenSuccess, fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        if (requestCount > 0) {
                            HorizontalDivider(color = GreenSuccess.copy(alpha = 0.15f), thickness = 0.5.dp)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                    Text("Requests", color = TextMuted, fontSize = 9.sp)
                                    Text("$requestCount", color = GreenSuccess, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                if (lastRequest.isNotBlank()) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Last request", color = TextMuted, fontSize = 9.sp)
                                        Text(lastRequest.substringAfterLast('/').take(30),
                                            color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    "Make sure your PC and phone are on the same Wi-Fi network. Open the URL in any browser to send downloads to your phone.",
                    color = TextMuted, fontSize = 10.sp, lineHeight = 15.sp
                )
            } else {
                Text(
                    "Enable to control downloads from your PC browser at http://[phone-ip]:8080",
                    color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp
                )
            }
        }
    }
}