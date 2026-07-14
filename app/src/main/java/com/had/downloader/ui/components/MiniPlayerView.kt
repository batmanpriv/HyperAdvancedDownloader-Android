package com.had.downloader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.had.downloader.data.model.DownloadItem
import com.had.downloader.data.model.DownloadStatus
import com.had.downloader.data.model.toHumanSize
import com.had.downloader.ui.theme.*

@Composable
fun MiniPlayerView(
    download: DownloadItem?,
    onExpand: () -> Unit,
    onMinimize: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (download == null) return

    var isMinimized by remember { mutableStateOf(false) }

    val isActive = download.status == DownloadStatus.DOWNLOADING ||
            download.status == DownloadStatus.CONNECTING ||
            download.status == DownloadStatus.MERGING

    if (!isActive) return

    val progressAnim by animateFloatAsState(
        targetValue = download.progress,
        animationSpec = tween(400),
        label = "miniProgress"
    )

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        if (isMinimized) {
            Box(
                modifier = modifier
                    .size(56.dp)
                    .shadow(12.dp, CircleShape)
                    .background(
                        Brush.linearGradient(listOf(CyanPrimary, PurpleAccent)),
                        CircleShape
                    )
                    .clickable { isMinimized = false }
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { onCancel() })
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(SpaceBlack.copy(alpha = 0.3f), CircleShape)
                ) {
                    CircularProgressIndicator(
                        progress = { progressAnim },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 3.dp
                    )
                }
                Icon(
                    Icons.Filled.Download,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                if (download.speedBps > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .background(GreenSuccess, CircleShape)
                            .size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 8.sp)
                    }
                }
            }
        } else {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .shadow(16.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .clickable { onExpand() },
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = SurfaceDark
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(listOf(CyanPrimary.copy(alpha = 0.2f), PurpleAccent.copy(alpha = 0.2f)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.OndemandVideo,
                            null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        CircularProgressIndicator(
                            progress = { progressAnim },
                            color = CyanPrimary,
                            trackColor = Color.Transparent,
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 2.dp
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            download.filename.ifBlank { "Downloading..." },
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (download.speedBps > 0) {
                                Text(
                                    "⚡ ${download.speedBps.toHumanSize()}/s",
                                    color = CyanPrimary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                "${download.downloadedBytes.toHumanSize()} / ${download.totalBytes.toHumanSize()}",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (download.etaSeconds >= 0) {
                                Text(
                                    "⏱ ${download.etaSeconds.toEtaString()}",
                                    color = OrangeWarn,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = { progressAnim },
                            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                            color = CyanPrimary,
                            trackColor = BorderColor
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        IconButton(
                            onClick = { isMinimized = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Remove,
                                null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = onCancel,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    null,
                                    tint = RedError,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = onExpand,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Filled.OpenInFull,
                                    null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Int.toEtaString(): String = when {
    this < 0 -> "--"
    this < 60 -> "${this}s"
    this < 3600 -> "${this / 60}m ${this % 60}s"
    else -> "${this / 3600}h ${(this % 3600) / 60}m"
}