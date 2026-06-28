package com.had.downloader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.had.downloader.data.model.AppSettings
import com.had.downloader.data.model.toHumanSize
import com.had.downloader.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onDismiss: () -> Unit,
    onFolderSelect: () -> Unit
) {
    var threads       by remember { mutableStateOf(settings.defaultThreads.toString()) }
    var outputDir     by remember { mutableStateOf(settings.defaultOutputDir) }
    var maxParallel   by remember { mutableStateOf(settings.maxConcurrent.toString()) }
    var proxy         by remember { mutableStateOf(settings.defaultProxy) }
    var maxSpeedBps   by remember { mutableStateOf(settings.defaultMaxSpeedBps.toString()) }
    var retries       by remember { mutableStateOf(settings.defaultRetries.toString()) }
    var timeoutSec    by remember { mutableStateOf(settings.defaultTimeoutSec.toString()) }
    var enableGzip    by remember { mutableStateOf(settings.enableGzip) }
    var saveSession   by remember { mutableStateOf(settings.saveSession) }
    var notifications by remember { mutableStateOf(settings.showNotifications) }

    var scheduleEnabled by remember { mutableStateOf(settings.defaultScheduleFrom.isNotBlank()) }
    var schedStartHour  by remember { mutableStateOf(parseTimePart(settings.defaultScheduleFrom, 0, 22)) }
    var schedStartMin   by remember { mutableStateOf(parseTimePart(settings.defaultScheduleFrom, 1, 0)) }
    var schedEndHour    by remember { mutableStateOf(parseTimePart(settings.defaultScheduleTo, 0, 6)) }
    var schedEndMin     by remember { mutableStateOf(parseTimePart(settings.defaultScheduleTo, 1, 0)) }

    var openSection by remember { mutableStateOf<String?>("download") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceDark,
        shape            = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, null, tint = CyanPrimary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Settings", fontWeight = FontWeight.Black, color = TextPrimary, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier            = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsSection(
                    title    = "Download Defaults",
                    icon     = Icons.Outlined.CloudDownload,
                    color    = CyanPrimary,
                    summary  = "${threads}T  •  ${maxParallel} parallel",
                    expanded = openSection == "download",
                    onToggle = { openSection = if (openSection == "download") null else "download" }
                ) {
                    SettingsTextField(
                        value         = outputDir,
                        onValueChange = { outputDir = it },
                        label         = "Default Save Folder",
                        placeholder   = "/sdcard/Downloads/HAD",
                        icon          = Icons.Outlined.FolderOpen,
                        trailingIcon  = {
                            IconButton(onClick = onFolderSelect) {
                                Icon(Icons.Outlined.Folder, null, tint = CyanPrimary)
                            }
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsTextField(
                            value         = threads,
                            onValueChange = { threads = it },
                            label         = "Threads / file",
                            placeholder   = "4",
                            icon          = Icons.Outlined.Speed,
                            keyboardType  = KeyboardType.Number,
                            modifier      = Modifier.weight(1f)
                        )
                        SettingsTextField(
                            value         = maxParallel,
                            onValueChange = { maxParallel = it },
                            label         = "Max parallel",
                            placeholder   = "2",
                            icon          = Icons.Outlined.Layers,
                            keyboardType  = KeyboardType.Number,
                            modifier      = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsTextField(
                            value         = retries,
                            onValueChange = { retries = it },
                            label         = "Retries",
                            placeholder   = "5",
                            icon          = Icons.Outlined.Refresh,
                            keyboardType  = KeyboardType.Number,
                            modifier      = Modifier.weight(1f)
                        )
                        SettingsTextField(
                            value         = timeoutSec,
                            onValueChange = { timeoutSec = it },
                            label         = "Timeout (s)",
                            placeholder   = "30",
                            icon          = Icons.Outlined.Timer,
                            keyboardType  = KeyboardType.Number,
                            modifier      = Modifier.weight(1f)
                        )
                    }
                }

                SettingsSection(
                    title    = "Network",
                    icon     = Icons.Outlined.VpnLock,
                    color    = PurpleAccent,
                    summary  = if (proxy.isBlank()) "No proxy" else proxy.take(22),
                    expanded = openSection == "network",
                    onToggle = { openSection = if (openSection == "network") null else "network" }
                ) {
                    SettingsTextField(
                        value         = proxy,
                        onValueChange = { proxy = it },
                        label         = "Default Proxy",
                        placeholder   = "socks5://127.0.0.1:1080",
                        icon          = Icons.Outlined.VpnLock
                    )
                    SpeedField(value = maxSpeedBps, onChange = { maxSpeedBps = it })
                }

                SettingsSection(
                    title    = "Schedule Window",
                    icon     = Icons.Outlined.Schedule,
                    color    = OrangeWarn,
                    summary  = if (scheduleEnabled)
                        "%02d:%02d → %02d:%02d".format(schedStartHour, schedStartMin, schedEndHour, schedEndMin)
                    else "Disabled — all downloads run anytime",
                    expanded = openSection == "schedule",
                    onToggle = { openSection = if (openSection == "schedule") null else "schedule" }
                ) {
                    ScheduleWindowSection(
                        enabled        = scheduleEnabled,
                        startHour      = schedStartHour,
                        startMin       = schedStartMin,
                        endHour        = schedEndHour,
                        endMin         = schedEndMin,
                        onEnabledChange = { scheduleEnabled = it },
                        onStartChanged  = { h, m -> schedStartHour = h; schedStartMin = m },
                        onEndChanged    = { h, m -> schedEndHour = h; schedEndMin = m }
                    )
                }

                SettingsSection(
                    title    = "Options",
                    icon     = Icons.Outlined.Tune,
                    color    = GreenSuccess,
                    summary  = buildString {
                        if (saveSession)   append("Resume  ")
                        if (enableGzip)    append("Gzip  ")
                        if (notifications) append("Notif")
                    }.trim().ifBlank { "All off" },
                    expanded = openSection == "options",
                    onToggle = { openSection = if (openSection == "options") null else "options" }
                ) {
                    SettingsToggle("Save Session (Resume)", saveSession)   { saveSession = it }
                    SettingsToggle("Enable Gzip",           enableGzip)    { enableGzip = it }
                    SettingsToggle("Show Notifications",    notifications) { notifications = it }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(AppSettings(
                        defaultThreads      = threads.toIntOrNull()?.coerceIn(1, 32) ?: 4,
                        defaultOutputDir    = outputDir.trim(),
                        maxConcurrent       = maxParallel.toIntOrNull()?.coerceIn(1, 10) ?: 2,
                        defaultProxy        = proxy.trim(),
                        defaultMaxSpeedBps  = maxSpeedBps.toLongOrNull() ?: 0L,
                        defaultRetries      = retries.toIntOrNull()?.coerceIn(1, 20) ?: 5,
                        defaultTimeoutSec   = timeoutSec.toIntOrNull()?.coerceIn(5, 300) ?: 30,
                        defaultScheduleFrom = if (scheduleEnabled)
                            "%02d:%02d".format(schedStartHour, schedStartMin) else "",
                        defaultScheduleTo   = if (scheduleEnabled)
                            "%02d:%02d".format(schedEndHour, schedEndMin) else "",
                        enableGzip          = enableGzip,
                        saveSession         = saveSession,
                        showNotifications   = notifications
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SpaceBlack),
                shape  = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun ScheduleWindowSection(
    enabled: Boolean,
    startHour: Int,
    startMin: Int,
    endHour: Int,
    endMin: Int,
    onEnabledChange: (Boolean) -> Unit,
    onStartChanged: (Int, Int) -> Unit,
    onEndChanged: (Int, Int) -> Unit
) {
    val isOvernight = (startHour * 60 + startMin) > (endHour * 60 + endMin)
    val durationLabel = remember(startHour, startMin, endHour, endMin) {
        val s = startHour * 60 + startMin
        val e = endHour * 60 + endMin
        val diff = if (e >= s) e - s else (24 * 60 - s) + e
        val h = diff / 60; val m = diff % 60
        if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Time Window", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (enabled) "Downloads only run within this time range"
                    else "Downloads run at any time",
                    color = TextMuted, fontSize = 10.sp
                )
            }
            Switch(
                checked         = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor   = SpaceBlack,
                    checkedTrackColor   = OrangeWarn,
                    uncheckedTrackColor = BorderColor
                )
            )
        }

        AnimatedVisibility(visible = enabled) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                WindowSummaryBanner(
                    startHour     = startHour,
                    startMin      = startMin,
                    endHour       = endHour,
                    endMin        = endMin,
                    isOvernight   = isOvernight,
                    durationLabel = durationLabel
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimeSpinnerCard(
                        label       = "START",
                        accentColor = GreenSuccess,
                        hour        = startHour,
                        minute      = startMin,
                        onChanged   = onStartChanged,
                        modifier    = Modifier.weight(1f)
                    )
                    TimeSpinnerCard(
                        label       = "END",
                        accentColor = RedError,
                        hour        = endHour,
                        minute      = endMin,
                        onChanged   = onEndChanged,
                        modifier    = Modifier.weight(1f)
                    )
                }

                if (isOvernight) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OrangeWarn.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                            .border(1.dp, OrangeWarn.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🌙", fontSize = 13.sp)
                        Text(
                            "Overnight window · crosses midnight · $durationLabel total",
                            color    = OrangeWarn,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        if (!enabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TextMuted.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Info, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                Text(
                    "Enable to restrict downloads to specific hours only",
                    color = TextMuted, fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun TimeSpinnerCard(
    label: String,
    accentColor: Color,
    hour: Int,
    minute: Int,
    onChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        color    = ElevatedSurf,
        border   = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(7.dp).background(accentColor, CircleShape))
                Text(
                    label,
                    color         = accentColor,
                    fontSize      = 9.sp,
                    fontFamily    = FontFamily.Monospace,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            }

            Text(
                "%02d:%02d".format(hour, minute),
                color      = accentColor,
                fontSize   = 26.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            )

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NumberSpinner(
                    value      = hour,
                    min        = 0,
                    max        = 23,
                    label      = "HH",
                    accentColor = accentColor,
                    onChanged  = { onChanged(it, minute) },
                    modifier   = Modifier.weight(1f)
                )
                NumberSpinner(
                    value      = minute,
                    min        = 0,
                    max        = 59,
                    label      = "MM",
                    accentColor = accentColor,
                    onChanged  = { onChanged(hour, it) },
                    modifier   = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun NumberSpinner(
    value: Int,
    min: Int,
    max: Int,
    label: String,
    accentColor: Color,
    onChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label,
            color      = TextMuted,
            fontSize   = 8.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceBlack, RoundedCornerShape(10.dp))
                .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .clickable { onChanged(if (value >= max) min else value + 1) }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                null,
                tint     = accentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(accentColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "%02d".format(value),
                color      = accentColor,
                fontSize   = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceBlack, RoundedCornerShape(10.dp))
                .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .clickable { onChanged(if (value <= min) max else value - 1) }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                null,
                tint     = accentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun WindowSummaryBanner(
    startHour: Int,
    startMin: Int,
    endHour: Int,
    endMin: Int,
    isOvernight: Boolean,
    durationLabel: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        color    = SpaceBlack,
        border   = BorderStroke(1.dp, OrangeWarn.copy(alpha = 0.25f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "%02d:%02d".format(startHour, startMin),
                        color = GreenSuccess, fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black
                    )
                    Text("START", color = GreenSuccess.copy(alpha = 0.6f), fontSize = 8.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isOvernight) Text("🌙", fontSize = 12.sp)
                    Text(
                        durationLabel,
                        color = OrangeWarn, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                    )
                    Icon(Icons.Filled.ArrowForward, null, tint = OrangeWarn.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "%02d:%02d".format(endHour, endMin),
                        color = RedError, fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black
                    )
                    Text("END", color = RedError.copy(alpha = 0.6f), fontSize = 8.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                }
            }

            TimelineBar(
                startFrac   = (startHour * 60 + startMin) / (24f * 60f),
                endFrac     = (endHour * 60 + endMin) / (24f * 60f),
                isOvernight = isOvernight
            )
        }
    }
}

@Composable
private fun TimelineBar(startFrac: Float, endFrac: Float, isOvernight: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0", "6", "12", "18", "24").forEach { t ->
                Text(t, color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            }
        }
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxWidth().height(10.dp)
        ) {
            val w = size.width
            val h = size.height
            val r = androidx.compose.ui.geometry.CornerRadius(5f)
            drawRoundRect(color = SpaceBlack, cornerRadius = r, size = size)
            if (isOvernight) {
                drawRoundRect(
                    color = OrangeWarn.copy(alpha = 0.5f), cornerRadius = r,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(endFrac * w, h)
                )
                drawRoundRect(
                    color = OrangeWarn.copy(alpha = 0.5f), cornerRadius = r,
                    topLeft = androidx.compose.ui.geometry.Offset(startFrac * w, 0f),
                    size = androidx.compose.ui.geometry.Size((1f - startFrac) * w, h)
                )
            } else {
                drawRoundRect(
                    color = OrangeWarn.copy(alpha = 0.5f), cornerRadius = r,
                    topLeft = androidx.compose.ui.geometry.Offset(startFrac * w, 0f),
                    size = androidx.compose.ui.geometry.Size((endFrac - startFrac) * w, h)
                )
            }
            drawLine(color = GreenSuccess, start = androidx.compose.ui.geometry.Offset(startFrac * w, 0f), end = androidx.compose.ui.geometry.Offset(startFrac * w, h), strokeWidth = 2.5f)
            drawLine(color = RedError, start = androidx.compose.ui.geometry.Offset(endFrac * w, 0f), end = androidx.compose.ui.geometry.Offset(endFrac * w, h), strokeWidth = 2.5f)
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = if (expanded) ElevatedSurf else SurfaceDark,
        border   = BorderStroke(1.dp, if (expanded) color.copy(alpha = 0.35f) else BorderColor)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title,   color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(summary, color = TextMuted,   fontSize = 10.sp)
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null,
                    tint     = if (expanded) color else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier            = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content             = content
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onValueChange,
        label           = { Text(label, color = TextSecondary, fontSize = 12.sp) },
        placeholder     = { Text(placeholder, color = TextMuted, fontSize = 12.sp) },
        leadingIcon     = { Icon(icon, null, modifier = Modifier.size(18.dp), tint = CyanDim) },
        trailingIcon    = trailingIcon,
        singleLine      = true,
        modifier        = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = CyanPrimary,
            unfocusedBorderColor    = BorderColor,
            focusedLabelColor       = CyanPrimary,
            cursorColor             = CyanPrimary,
            focusedTextColor        = TextPrimary,
            unfocusedTextColor      = TextPrimary,
            unfocusedContainerColor = ElevatedSurf,
            focusedContainerColor   = ElevatedSurf
        ),
        shape     = RoundedCornerShape(10.dp),
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
    )
}

@Composable
private fun SpeedField(value: String, onChange: (String) -> Unit) {
    val speedLabel = remember(value) {
        val bps = value.toLongOrNull() ?: 0L
        if (bps <= 0) "Unlimited" else bps.toHumanSize() + "/s"
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SettingsTextField(
            value         = value,
            onValueChange = onChange,
            label         = "Max Speed (bytes/s, 0 = unlimited)",
            placeholder   = "0",
            icon          = Icons.Outlined.Speed,
            keyboardType  = KeyboardType.Number
        )
        Text(speedLabel, color = CyanPrimary, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextPrimary, fontSize = 14.sp)
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = SpaceBlack,
                checkedTrackColor   = CyanPrimary,
                uncheckedTrackColor = BorderColor
            )
        )
    }
}

private fun parseTimePart(hhmm: String, part: Int, default: Int): Int =
    hhmm.split(":").getOrNull(part)?.toIntOrNull() ?: default

fun formatSettingsTime(h: Int, m: Int, use24h: Boolean): String = if (use24h) {
    "%02d:%02d".format(h, m)
} else {
    val h12 = (h % 12).let { if (it == 0) 12 else it }
    "%02d:%02d %s".format(h12, m, if (h < 12) "AM" else "PM")
}