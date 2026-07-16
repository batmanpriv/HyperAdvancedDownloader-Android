package com.had.downloader.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.had.downloader.data.model.DownloadItem
import com.had.downloader.data.model.DownloadMode
import com.had.downloader.data.model.DownloadStatus
import com.had.downloader.data.model.toHumanSize
import com.had.downloader.ui.theme.*

@Composable
fun HlsProgressSection(
    item: DownloadItem,
    modifier: Modifier = Modifier
) {
    if (item.mode != DownloadMode.HLS) return

    val isActive = item.status == DownloadStatus.DOWNLOADING ||
            item.status == DownloadStatus.CONNECTING ||
            item.status == DownloadStatus.MERGING

    val segDone = item.hlsSegmentsDone
    val segTotal = item.hlsSegmentCount

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {

        GlowProgressBar(
            progress = item.progress,
            color = when (item.status) {
                DownloadStatus.MERGING -> PurpleAccent
                DownloadStatus.COMPLETED -> GreenSuccess
                DownloadStatus.FAILED, DownloadStatus.CANCELLED -> RedError
                else -> CyanPrimary
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (segTotal > 0) {
                    
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = CyanPrimary.copy(alpha = 0.09f),
                        border = BorderStroke(0.5.dp, CyanPrimary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = when (item.status) {
                                DownloadStatus.MERGING -> "Merging $segTotal segs"
                                DownloadStatus.COMPLETED -> "$segTotal segs"
                                else -> "$segDone / $segTotal segs"
                            },
                            color = CyanPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                if (item.downloadedBytes > 0) {
                    Text(
                        text = item.downloadedBytes.toHumanSize(),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (item.totalBytes > 0) {
                        Text("/ ${item.totalBytes.toHumanSize()}", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (item.speedBps > 0 && isActive) {
                    Text(
                        "⚡ ${item.speedBps.toHumanSize()}/s",
                        color = CyanPrimary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    "${(item.progress * 100).toInt()}%",
                    color = when (item.status) {
                        DownloadStatus.COMPLETED -> GreenSuccess
                        DownloadStatus.FAILED, DownloadStatus.CANCELLED -> RedError
                        else -> CyanPrimary
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
