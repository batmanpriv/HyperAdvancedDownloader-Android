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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.had.downloader.service.VideoFormat
import com.had.downloader.service.VideoQuality
import com.had.downloader.service.VideoStream
import com.had.downloader.ui.theme.*

@Composable
fun VideoDetectionDialog(
    pageUrl: String,
    streams: List<VideoStream>,
    onDownload: (VideoStream) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStream by remember { mutableStateOf<VideoStream?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(CyanPrimary, PurpleAccent)
                                ),
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
                    Column {
                        Text(
                            "🎬 Video Detected!",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            t("Choose quality to download", "کیفیت مورد نظر برای دانلود را انتخاب کن"),
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = CyanPrimary.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            t("${streams.size} stream${if (streams.size > 1) "s" else ""} found", "${streams.size} استریم پیدا شد"),
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (streams.any { it.durationSeconds > 0 }) {
                            Spacer(Modifier.weight(1f))
                            DurationBadge(streams.maxOfOrNull { it.durationSeconds } ?: 0)
                        }
                    }
                }

                streams.forEach { stream ->
                    VideoStreamCard(
                        stream = stream,
                        isSelected = selectedStream?.url == stream.url,
                        onSelect = { selectedStream = stream },
                        onDownload = { onDownload(stream) }
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
                                t("No video streams detected", "هیچ استریم ویدیویی پیدا نشد"),
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                            Text(
                                t("Try refreshing the page", "صفحه را دوباره بارگذاری کن"),
                                color = TextMuted.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
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
                    Text(t("Close", "بستن"), color = TextSecondary)
                }

                if (selectedStream != null) {
                    Button(
                        onClick = { selectedStream?.let { onDownload(it) } },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = SpaceBlack
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            t("Download ", "دانلود ") + (selectedStream?.quality?.label ?: ""),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun VideoStreamCard(
    stream: VideoStream,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit
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
        VideoFormat.HLS_M3U8 -> "HLS"
        VideoFormat.DASH_MPD -> "DASH"
        VideoFormat.MP4 -> "MP4"
        VideoFormat.MKV -> "MKV"
        VideoFormat.WEBM -> "WEBM"
        VideoFormat.AUDIO_MP3 -> "MP3"
        VideoFormat.AUDIO_FLAC -> "FLAC"
        VideoFormat.AUDIO_AAC -> "AAC"
        VideoFormat.AUDIO_OGG -> "OGG"
        VideoFormat.AUDIO_WAV -> "WAV"
        VideoFormat.AUDIO_M4A -> "M4A"
        else -> stream.format.name.take(4)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
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

                    if (stream.format == VideoFormat.HLS_M3U8 || stream.format == VideoFormat.DASH_MPD) {
                        Box(
                            modifier = Modifier
                                .background(OrangeWarn.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (stream.format == VideoFormat.HLS_M3U8) "HLS" else "DASH",
                                color = OrangeWarn,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

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

            IconButton(
                onClick = onDownload,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isSelected) CyanPrimary else qualityColor.copy(alpha = 0.1f),
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Icon(
                    Icons.Filled.Download,
                    null,
                    tint = if (isSelected) SpaceBlack else CyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DurationBadge(durationSeconds: Int) {
    val durationText = formatDuration(durationSeconds)
    Row(
        modifier = Modifier
            .background(OrangeWarn.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Outlined.Timer,
            null,
            tint = OrangeWarn,
            modifier = Modifier.size(12.dp)
        )
        Text(
            durationText,
            color = OrangeWarn,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
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