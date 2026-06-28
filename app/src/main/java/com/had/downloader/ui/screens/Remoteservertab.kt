package com.had.downloader.ui.screens

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.had.downloader.service.RemoteServerState
import com.had.downloader.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RemoteServerTab(vm: MainViewModel) {
    val serverState by vm.remoteServerState.collectAsState()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        
        if (serverState.isRunning) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GreenSuccess.copy(alpha = 0.04f), Color.Transparent),
                        center = Offset(size.width * 0.5f, 0f),
                        radius = size.width * 0.9f
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (serverState.isRunning) GreenSuccess.copy(alpha = 0.12f)
                            else BorderColor,
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Wifi,
                        null,
                        tint = if (serverState.isRunning) GreenSuccess else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        "Remote Download Server",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Control HAD from your PC browser",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            ServerControlCard(serverState = serverState, vm = vm)

            AnimatedVisibility(visible = !serverState.isRunning) {
                HowItWorksCard()
            }

            AnimatedVisibility(visible = serverState.isRunning) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LiveAddressCard(serverState = serverState)
                    StatsRow2(serverState = serverState)
                }
            }

            AnimatedVisibility(visible = serverState.requestCount > 0) {
                RequestHistorySection(vm = vm, serverState = serverState)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ServerControlCard(serverState: RemoteServerState, vm: MainViewModel) {
    val pulseAlpha by rememberInfiniteTransition(label = "srv").animateFloat(
        initialValue = 0.5f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "srvp"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        color    = SurfaceDark,
        border   = BorderStroke(
            1.dp,
            if (serverState.isRunning) GreenSuccess.copy(alpha = 0.4f) else BorderColor
        )
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (serverState.isRunning)
                                    GreenSuccess.copy(alpha = pulseAlpha)
                                else
                                    TextMuted.copy(alpha = 0.4f),
                                CircleShape
                            )
                    )
                    Column {
                        Text(
                            if (serverState.isRunning) "Server Online" else "Server Offline",
                            color = if (serverState.isRunning) GreenSuccess else TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (serverState.isRunning)
                                "Listening on port ${serverState.port}"
                            else
                                "Tap to start accepting downloads",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked         = serverState.isRunning,
                    onCheckedChange = { on ->
                        if (on) vm.startRemoteServer() else vm.stopRemoteServer()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor   = SpaceBlack,
                        checkedTrackColor   = GreenSuccess,
                        uncheckedTrackColor = BorderColor,
                        uncheckedThumbColor = TextMuted
                    )
                )
            }

            Button(
                onClick = {
                    if (serverState.isRunning) vm.stopRemoteServer()
                    else vm.startRemoteServer()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (serverState.isRunning) RedError else GreenSuccess,
                    contentColor   = SpaceBlack
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    if (serverState.isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (serverState.isRunning) "Stop Server" else "Start Server",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun LiveAddressCard(serverState: RemoteServerState) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val url = "http://${serverState.ipAddress}:${serverState.port}"

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000L)
            copied = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(18.dp),
        color    = GreenSuccess.copy(alpha = 0.06f),
        border   = BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.25f))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Text(
                "OPEN IN PC BROWSER",
                color         = GreenSuccess.copy(alpha = 0.7f),
                fontSize      = 9.sp,
                fontFamily    = FontFamily.Monospace,
                letterSpacing = 3.sp,
                fontWeight    = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpaceBlack, RoundedCornerShape(12.dp))
                    .border(1.dp, GreenSuccess.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    url,
                    color      = GreenSuccess,
                    fontSize   = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    modifier   = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(url))
                        copied = true
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (copied) GreenSuccess.copy(alpha = 0.15f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        if (copied) Icons.Filled.Check else Icons.Outlined.ContentCopy,
                        null,
                        tint     = if (copied) GreenSuccess else GreenSuccess.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                "Make sure your PC and phone are on the same Wi-Fi network",
                color      = GreenSuccess.copy(alpha = 0.6f),
                fontSize   = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun StatsRow2(serverState: RemoteServerState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatPill2(
            value    = "${serverState.requestCount}",
            label    = "Requests",
            color    = CyanPrimary,
            modifier = Modifier.weight(1f)
        )
        StatPill2(
            value    = "8080",
            label    = "Port",
            color    = PurpleAccent,
            modifier = Modifier.weight(1f)
        )
        StatPill2(
            value    = serverState.ipAddress.ifBlank { "--" },
            label    = "IP",
            color    = GreenSuccess,
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
private fun StatPill2(value: String, label: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        color    = SurfaceDark,
        border   = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(label, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun RequestHistorySection(vm: MainViewModel, serverState: RemoteServerState) {

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.History,
                    null,
                    tint     = OrangeWarn,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "INCOMING REQUESTS",
                    color         = TextMuted,
                    fontSize      = 9.sp,
                    fontFamily    = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    fontWeight    = FontWeight.Bold
                )
            }
            Text(
                "${serverState.requestCount} total",
                color      = OrangeWarn,
                fontSize   = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

        if (serverState.lastRequest.isNotBlank()) {
            LastRequestCard(url = serverState.lastRequest, vm = vm)
        }
    }
}

@Composable
private fun LastRequestCard(url: String, vm: MainViewModel) {
    val filename = url.substringAfterLast('/').substringBefore('?')
        .ifBlank { url.substringAfter("://").take(30) }
    val ext = filename.substringAfterLast('.').lowercase().take(5)
    val extColor = when (ext) {
        "mp4", "mkv", "webm", "m3u8", "mpd", "avi", "mov" -> GreenSuccess
        "mp3", "flac", "aac", "m4a", "ogg"                 -> PurpleAccent
        "zip", "rar", "7z", "tar", "gz"                    -> OrangeWarn
        "pdf", "epub", "mobi", "doc", "docx"               -> CyanPrimary
        "apk", "exe", "dmg"                                -> RedError
        else                                               -> TextSecondary
    }
    val isHls = url.contains(".m3u8", ignoreCase = true)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        color    = ElevatedSurf,
        border   = BorderStroke(1.dp, extColor.copy(alpha = 0.25f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(extColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.CloudDownload,
                        null,
                        tint     = extColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        filename.ifBlank { "Remote download" },
                        color      = TextPrimary,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        url,
                        color    = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (ext.isNotBlank() && !ext.contains('/')) {
                    Box(
                        modifier = Modifier
                            .background(extColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            ".$ext",
                            color      = extColor,
                            fontSize   = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                
                OutlinedButton(
                    onClick = {
                        vm.queueClipboardUrls(listOf(url))
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent),
                    border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Outlined.AddCircleOutline,
                        null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Queue",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        vm.downloadClipboardUrls(listOf(url))
                    },
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHls) GreenSuccess else CyanPrimary,
                        contentColor   = SpaceBlack
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (isHls) "Download HLS" else "Download Now",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HowItWorksCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = ElevatedSurf,
        border   = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.Info,
                    null,
                    tint     = CyanPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "How it works",
                    color      = CyanPrimary,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val steps = listOf(
                "Start the server — your phone starts listening on port 8080",
                "Connect your PC to the same Wi-Fi network as your phone",
                "Open the displayed IP:port URL in any PC browser",
                "Paste any download link in the web UI — it lands on your phone instantly",
                "Choose Download Now to start immediately, or Queue to schedule"
            )

            steps.forEachIndexed { i, step ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(CyanPrimary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${i + 1}",
                            color      = CyanPrimary,
                            fontSize   = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        step,
                        color      = TextSecondary,
                        fontSize   = 12.sp,
                        lineHeight = 17.sp,
                        modifier   = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}