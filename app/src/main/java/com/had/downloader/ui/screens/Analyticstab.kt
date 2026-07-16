package com.had.downloader.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.had.downloader.data.model.toHumanSize
import com.had.downloader.data.model.toSpeedString
import com.had.downloader.data.repository.HourlyStats
import com.had.downloader.data.repository.MonthlyStats
import com.had.downloader.data.repository.OverallStats
import com.had.downloader.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AnalyticsTab(vm: MainViewModel) {
    val overallStats   by vm.overallStats.collectAsState(initial = OverallStats())
    val monthlyStats   by vm.monthlyStats.collectAsState(initial = emptyList())
    val hourlyStats    by vm.hourlyStats.collectAsState(initial = emptyList())
    val recentEvents   by vm.recentAnalyticsEvents.collectAsState(initial = emptyList())
    val scrollState    = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "ANALYTICS",
            color = TextMuted, fontSize = 10.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 3.sp,
            fontFamily = FontFamily.Monospace
        )
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

        OverallStatsSection(stats = overallStats)

        if (monthlyStats.isNotEmpty()) {
            MonthlyBarChart(data = monthlyStats)
        }

        if (hourlyStats.isNotEmpty()) {
            HourlyHeatmap(data = hourlyStats)
        }

        SpeedHistorySection(vm = vm)

        if (recentEvents.isNotEmpty()) {
            RecentDownloadsTable(events = recentEvents)
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun OverallStatsSection(stats: OverallStats) {
    val successRate = if (stats.total > 0) (stats.successful.toFloat() / stats.total * 100).toInt() else 0

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "OVERVIEW",
            color = TextMuted, fontSize = 9.sp,
            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard2(
                label = "Total",
                value = "${stats.total}",
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            StatCard2(
                label = "Done",
                value = "${stats.successful}",
                color = GreenSuccess,
                modifier = Modifier.weight(1f)
            )
            StatCard2(
                label = "Success",
                value = "$successRate%",
                color = if (successRate >= 80) GreenSuccess else OrangeWarn,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard2(
                label = "Total Saved",
                value = stats.totalBytes.toHumanSize(),
                color = CyanPrimary,
                modifier = Modifier.weight(1f)
            )
            StatCard2(
                label = "Avg Speed",
                value = stats.avgSpeed.toSpeedString(),
                color = PurpleAccent,
                modifier = Modifier.weight(1f)
            )
            StatCard2(
                label = "Peak Speed",
                value = stats.peakSpeed.toSpeedString(),
                color = OrangeWarn,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard2(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        color    = SurfaceDark,
        border   = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier            = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            Text(label, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun MonthlyBarChart(data: List<MonthlyStats>) {
    val maxBytes = data.maxOfOrNull { it.bytes } ?: 1L
    val anim by animateFloatAsState(targetValue = 1f, animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "barAnim")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = SurfaceDark,
        border   = BorderStroke(1.dp, BorderColor)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.BarChart, null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                Text("MONTHLY DOWNLOADS", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            }

            val reversed = data.reversed()

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val w         = size.width
                val h         = size.height
                val barCount  = reversed.size
                val gap       = 6f
                val barWidth  = (w - gap * (barCount - 1)) / barCount

                reversed.forEachIndexed { i, stat ->
                    val x   = i * (barWidth + gap)
                    val pct = (stat.bytes.toFloat() / maxBytes) * anim
                    val bh  = pct * h

                    drawRoundRect(
                        color        = ElevatedSurf,
                        topLeft      = Offset(x, 0f),
                        size         = Size(barWidth, h),
                        cornerRadius = CornerRadius(4f)
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors   = listOf(CyanPrimary, CyanPrimary.copy(alpha = 0.5f)),
                            startY   = h - bh,
                            endY     = h
                        ),
                        topLeft      = Offset(x, h - bh),
                        size         = Size(barWidth, bh),
                        cornerRadius = CornerRadius(4f)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                reversed.forEach { stat ->
                    Text(
                        stat.monthYear.takeLast(5),
                        color = TextMuted, fontSize = 7.sp, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun HourlyHeatmap(data: List<HourlyStats>) {
    val maxCount = data.maxOfOrNull { it.count } ?: 1

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = SurfaceDark,
        border   = BorderStroke(1.dp, BorderColor)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.AccessTime, null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
                Text("DOWNLOADS BY HOUR", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            }

            val hourMap = data.associate { it.hourOfDay to it.count }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                (0..23).forEach { hour ->
                    val count = hourMap[hour] ?: 0
                    val pct   = count.toFloat() / maxCount
                    Column(
                        modifier            = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(
                                    PurpleAccent.copy(alpha = 0.08f + pct * 0.7f),
                                    RoundedCornerShape(3.dp)
                                )
                        )
                        if (hour % 6 == 0) {
                            Text(
                                "%02d".format(hour),
                                color = TextMuted, fontSize = 6.sp, fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("00:00", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Low", color = TextMuted, fontSize = 8.sp)
                    Box(Modifier.width(40.dp).height(8.dp).background(
                        Brush.horizontalGradient(listOf(PurpleAccent.copy(alpha = 0.08f), PurpleAccent)),
                        RoundedCornerShape(4.dp)
                    ))
                    Text("High", color = TextMuted, fontSize = 8.sp)
                }
                Text("23:00", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun SpeedHistorySection(vm: MainViewModel) {
    val speedHistory by vm.currentSpeedHistory.collectAsState(initial = emptyList())
    if (speedHistory.isEmpty()) return

    val maxSpeed = speedHistory.maxOfOrNull { it.speedBps } ?: 1L
    val anim by animateFloatAsState(targetValue = 1f, animationSpec = tween(800), label = "speedAnim")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = SurfaceDark,
        border   = BorderStroke(1.dp, BorderColor)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Speed, null, tint = GreenSuccess, modifier = Modifier.size(16.dp))
                Text("SPEED HISTORY", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                Spacer(Modifier.weight(1f))
                Text("Peak: ${maxSpeed.toSpeedString()}", color = GreenSuccess, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val w     = size.width
                val h     = size.height
                val pts   = speedHistory.takeLast(100)
                if (pts.size < 2) return@Canvas

                val step  = w / (pts.size - 1).toFloat()

                drawRoundRect(color = ElevatedSurf, cornerRadius = CornerRadius(8f), size = size)

                val path = Path().apply {
                    pts.forEachIndexed { i, sample ->
                        val x = i * step
                        val y = h - (sample.speedBps.toFloat() / maxSpeed) * h * anim
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }

                drawPath(
                    path  = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(GreenSuccess.copy(alpha = 0.3f), GreenSuccess.copy(alpha = 0f))
                    )
                )

                drawPath(
                    path        = path,
                    color       = GreenSuccess,
                    style       = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                val last = pts.last()
                val lx   = (pts.size - 1) * step
                val ly   = h - (last.speedBps.toFloat() / maxSpeed) * h * anim
                drawCircle(color = GreenSuccess, radius = 4f, center = Offset(lx, ly))
                drawCircle(color = SpaceBlack, radius = 2f, center = Offset(lx, ly))
            }
        }
    }
}

@Composable
private fun RecentDownloadsTable(events: List<com.had.downloader.data.repository.AnalyticsEvent>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = SurfaceDark,
        border   = BorderStroke(1.dp, BorderColor)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.History, null, tint = OrangeWarn, modifier = Modifier.size(16.dp))
                Text("RECENT HISTORY", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("File", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(2f))
                Text("Size", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text("Speed", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text("Status", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            events.take(20).forEach { event ->
                val statusColor = if (event.success) GreenSuccess else RedError
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        event.filename.ifBlank { event.url.substringAfterLast('/').take(20) },
                        color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(2f)
                    )
                    Text(
                        event.totalBytes.toHumanSize(),
                        color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        event.avgSpeedBps.toSpeedString(),
                        color = CyanPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (event.success) "OK" else "FAIL",
                            color = statusColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}