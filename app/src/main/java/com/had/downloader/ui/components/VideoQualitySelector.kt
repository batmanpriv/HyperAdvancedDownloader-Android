package com.had.downloader.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.had.downloader.service.VideoQuality
import com.had.downloader.service.VideoStream
import com.had.downloader.ui.theme.*

@Composable
fun VideoQualitySelectorDialog(
    streams: List<VideoStream>,
    pageUrl: String,
    loading: Boolean = false,
    onDownload: (VideoStream) -> Unit,
    onQueue: (VideoStream) -> Unit = {},
    onDismiss: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var isQueuing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
                        Icon(
                            Icons.Filled.OndemandVideo,
                            null,
                            tint = SpaceBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Select Quality",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            if (loading && streams.isEmpty()) "Looking for available qualities…"
                            else "${streams.size} streams available",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
                if (isDownloading || isQueuing || (loading && streams.isEmpty())) {
                    CircularProgressIndicator(
                        color = CyanPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (loading && streams.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = CyanPrimary, strokeWidth = 3.dp)
                        Text(
                            "Reading available qualities from the page…",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            "This can take a bit longer on a slow connection",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    return@Column
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = ElevatedSurf,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Choose a quality to download. Higher quality = larger file.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                streams.forEachIndexed { index, stream ->
                    QualityStreamCard(
                        stream = stream,
                        isSelected = selectedIndex == index,
                        onClick = {
                            selectedIndex = if (selectedIndex == index) null else index
                        }
                    )
                }

                if (streams.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(ElevatedSurf, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.SearchOff,
                                null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "No quality options available",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                selectedIndex?.let { index ->
                    val stream = streams[index]
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = CyanPrimary.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    null,
                                    tint = GreenSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Selected: ${stream.quality.label} · ${formatDuration(stream.durationSeconds)}",
                                    color = CyanPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }

                OutlinedButton(
                    onClick = {
                        selectedIndex?.let { index ->
                            isQueuing = true
                            onQueue(streams[index])
                            isQueuing = false
                            onDismiss()
                        }
                    },
                    enabled = selectedIndex != null && !isDownloading && !isQueuing,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent),
                    border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isQueuing) {
                        CircularProgressIndicator(
                            color = PurpleAccent,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Queuing...", fontWeight = FontWeight.Medium)
                    } else {
                        Icon(Icons.Outlined.AddCircleOutline, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Queue", fontWeight = FontWeight.Medium)
                    }
                }

                Button(
                    onClick = {
                        selectedIndex?.let { index ->
                            isDownloading = true
                            onDownload(streams[index])
                            isDownloading = false
                            onDismiss()
                        }
                    },
                    enabled = selectedIndex != null && !isDownloading && !isQueuing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = SpaceBlack
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            color = SpaceBlack,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Starting...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    )
}

@Composable
private fun QualityStreamCard(
    stream: VideoStream,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val qualityColor = when (stream.quality) {
        VideoQuality.UHD_4K -> CyanPrimary
        VideoQuality.QHD_2K -> CyanPrimary.copy(alpha = 0.7f)
        VideoQuality.FHD_1080 -> GreenSuccess
        VideoQuality.HD_720 -> OrangeWarn
        VideoQuality.SD_480 -> OrangeWarn.copy(alpha = 0.7f)
        VideoQuality.SD_360 -> TextSecondary
        VideoQuality.LOW_240 -> TextSecondary.copy(alpha = 0.5f)
        VideoQuality.AUDIO_ONLY -> PurpleAccent
        else -> TextMuted
    }

    val formatLabel = when (stream.format) {
        com.had.downloader.service.VideoFormat.HLS_M3U8 -> "HLS"
        com.had.downloader.service.VideoFormat.DASH_MPD -> "DASH"
        com.had.downloader.service.VideoFormat.MP4 -> "MP4"
        com.had.downloader.service.VideoFormat.MKV -> "MKV"
        com.had.downloader.service.VideoFormat.WEBM -> "WEBM"
        com.had.downloader.service.VideoFormat.AUDIO_MP3 -> "MP3"
        com.had.downloader.service.VideoFormat.AUDIO_FLAC -> "FLAC"
        com.had.downloader.service.VideoFormat.AUDIO_AAC -> "AAC"
        else -> stream.format.name.take(4)
    }

    val isHls = stream.format == com.had.downloader.service.VideoFormat.HLS_M3U8 ||
            stream.format == com.had.downloader.service.VideoFormat.DASH_MPD

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                2.dp,
                if (isSelected) CyanPrimary else Color.Transparent,
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) CyanPrimary.copy(alpha = 0.06f) else ElevatedSurf,
        border = BorderStroke(1.dp, qualityColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(qualityColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (stream.audioOnly) Icons.Outlined.MusicNote else Icons.Outlined.OndemandVideo,
                    null,
                    tint = qualityColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        if (stream.audioOnly) "🎵 Audio" else stream.quality.label,
                        color = qualityColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (isHls) {
                        Box(
                            modifier = Modifier
                                .background(OrangeWarn.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                formatLabel,
                                color = OrangeWarn,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(TextMuted.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                formatLabel,
                                color = TextMuted,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (stream.resolution.isNotBlank()) {
                        Text(
                            stream.resolution,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (stream.sizeMb > 0) {
                        Text(
                            String.format("%.1f MB", stream.sizeMb),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (stream.durationSeconds > 0) {
                        Text(
                            formatDuration(stream.durationSeconds),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (stream.bitrate > 0) {
                        Text(
                            String.format("%d kbps", stream.bitrate / 1000),
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(CyanPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        null,
                        tint = SpaceBlack,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> String.format("%dh %02dm", hours, minutes)
        minutes > 0 -> String.format("%dm %02ds", minutes, secs)
        else -> String.format("%ds", secs)
    }
}