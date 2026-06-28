package com.had.downloader.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.had.downloader.data.model.toHumanSize
import com.had.downloader.service.ChunkInfo
import com.had.downloader.service.ChunkStatus
import com.had.downloader.ui.theme.*
import kotlin.math.ceil

enum class ThreadViewMode {
    SEGMENT_BAR,   
    WAVEFORM,      
    GRID           
}

@Composable
fun ThreadVisualizer(
    chunks: List<ChunkInfo>,
    totalBytes: Long,
    mode: ThreadViewMode = ThreadViewMode.SEGMENT_BAR,
    modifier: Modifier = Modifier
) {
    if (chunks.isEmpty() || totalBytes <= 0L) return

    when (mode) {
        ThreadViewMode.SEGMENT_BAR -> SegmentBarView(chunks, totalBytes, modifier)
        ThreadViewMode.WAVEFORM    -> WaveformView(chunks, modifier)
        ThreadViewMode.GRID        -> GridView(chunks, modifier)
    }
}

@Composable
fun SegmentBarView(
    chunks: List<ChunkInfo>,
    totalBytes: Long,
    modifier: Modifier = Modifier
) {
    val glowAnim by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0B", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(totalBytes.toHumanSize(), color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("├", color = BorderColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text("─".repeat(60), color = BorderColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f))
            Text("┤", color = BorderColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(3.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            val w = size.width
            val h = size.height

            drawRoundRect(
                color        = ElevatedSurf,
                cornerRadius = CornerRadius(4f),
                size         = size
            )

            chunks.forEach { chunk ->
                if (chunk.end < 0) return@forEach
                val chunkLen  = (chunk.end - chunk.start + 1).toFloat()
                val x0        = (chunk.start.toFloat() / totalBytes) * w
                val chunkW    = (chunkLen / totalBytes) * w
                val doneW     = (chunk.downloaded.toFloat() / chunkLen) * chunkW

                val chunkColor = when (chunk.status) {
                    ChunkStatus.DONE      -> GreenSuccess
                    ChunkStatus.FAILED    -> RedError
                    ChunkStatus.RETRYING  -> OrangeWarn
                    ChunkStatus.DOWNLOADING, ChunkStatus.PENDING ->
                        CyanPrimary.copy(alpha = if (chunk.status == ChunkStatus.DOWNLOADING) glowAnim else 0.3f)
                    else -> TextMuted
                }

                if (doneW > 0) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(chunkColor.copy(alpha = 0.8f), chunkColor),
                            startX = x0,
                            endX   = x0 + doneW
                        ),
                        topLeft      = Offset(x0, 0f),
                        size         = Size(doneW, h),
                        cornerRadius = CornerRadius(2f)
                    )
                }

                val pendingW = chunkW - doneW
                if (pendingW > 0) {
                    drawRoundRect(
                        color        = chunkColor.copy(alpha = 0.08f),
                        topLeft      = Offset(x0 + doneW, 0f),
                        size         = Size(pendingW, h),
                        cornerRadius = CornerRadius(2f)
                    )
                }

                drawLine(
                    color       = SpaceBlack,
                    start       = Offset(x0, 0f),
                    end         = Offset(x0, h),
                    strokeWidth = 1.5f
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            chunks.take(8).forEachIndexed { i, chunk ->
                ChunkLegendChip(index = i, chunk = chunk)
            }
            if (chunks.size > 8) {
                Text("+${chunks.size - 8}", color = TextMuted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun ChunkLegendChip(index: Int, chunk: ChunkInfo) {
    val color = when (chunk.status) {
        ChunkStatus.DONE      -> GreenSuccess
        ChunkStatus.FAILED    -> RedError
        ChunkStatus.RETRYING  -> OrangeWarn
        ChunkStatus.DOWNLOADING -> CyanPrimary
        else -> TextMuted
    }
    val pct = if (chunk.end > 0)
        (chunk.downloaded.toFloat() / (chunk.end - chunk.start + 1) * 100).toInt()
    else 0

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, RoundedCornerShape(1.dp))
        )
        Spacer(Modifier.width(3.dp))
        Text(
            "T${index + 1} ${pct}%",
            color = color,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun WaveformView(
    chunks: List<ChunkInfo>,
    modifier: Modifier = Modifier
) {
    val infiniteAnim = rememberInfiniteTransition(label = "wave")

    Column(modifier = modifier) {
        Text(
            "THREADS",
            color = TextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            modifier            = Modifier.fillMaxWidth().height(60.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment   = Alignment.Bottom
        ) {
            chunks.forEachIndexed { i, chunk ->
                val pct = if (chunk.end > 0)
                    chunk.downloaded.toFloat() / (chunk.end - chunk.start + 1)
                else 0f

                val animatedHeight by animateFloatAsState(
                    targetValue = pct.coerceIn(0f, 1f),
                    animationSpec = tween(600, easing = FastOutSlowInEasing),
                    label = "waveH_$i"
                )

                val pulse by infiniteAnim.animateFloat(
                    initialValue = 0.7f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(600 + i * 80, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse
                    ),
                    label = "pulse_$i"
                )

                val color = when (chunk.status) {
                    ChunkStatus.DONE        -> GreenSuccess
                    ChunkStatus.FAILED      -> RedError
                    ChunkStatus.RETRYING    -> OrangeWarn
                    ChunkStatus.DOWNLOADING -> CyanPrimary.copy(alpha = pulse)
                    else -> TextMuted.copy(alpha = 0.3f)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedHeight)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(color, color.copy(alpha = 0.4f))
                                )
                            )
                    )
                }
            }
        }

        Row(
            modifier              = Modifier.fillMaxWidth().padding(top = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            chunks.forEachIndexed { i, _ ->
                Text(
                    "${i + 1}",
                    modifier = Modifier.weight(1f),
                    color    = TextMuted,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun GridView(
    chunks: List<ChunkInfo>,
    modifier: Modifier = Modifier
) {
    val cols = when {
        chunks.size <= 4  -> 2
        chunks.size <= 8  -> 4
        else              -> 4
    }
    val rows = ceil(chunks.size.toDouble() / cols).toInt()

    Column(modifier = modifier) {
        Text(
            "THREAD GRID",
            color = TextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(cols) { col ->
                    val idx = row * cols + col
                    if (idx < chunks.size) {
                        GridCell(index = idx, chunk = chunks[idx], modifier = Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GridCell(index: Int, chunk: ChunkInfo, modifier: Modifier = Modifier) {
    val pct = if (chunk.end > 0)
        (chunk.downloaded.toFloat() / (chunk.end - chunk.start + 1)).coerceIn(0f, 1f)
    else 0f

    val animPct by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(500),
        label = "gridPct_$index"
    )

    val pulse by rememberInfiniteTransition(label = "gp$index").animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700 + index * 50), RepeatMode.Reverse),
        label = "gp_$index"
    )

    val baseColor = when (chunk.status) {
        ChunkStatus.DONE        -> GreenSuccess
        ChunkStatus.FAILED      -> RedError
        ChunkStatus.RETRYING    -> OrangeWarn
        ChunkStatus.DOWNLOADING -> CyanPrimary.copy(alpha = pulse)
        ChunkStatus.PENDING     -> TextMuted.copy(alpha = 0.2f)
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ElevatedSurf)
            .border(1.dp, baseColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.BottomStart
    ) {
        
        Box(
            modifier = Modifier
                .fillMaxWidth(animPct)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(baseColor.copy(alpha = 0.2f), baseColor.copy(alpha = 0.4f))
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "T${index + 1}",
                color = baseColor,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${(animPct * 100).toInt()}%",
                color = baseColor.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun ThreadViewModeSelector(
    current: ThreadViewMode,
    onSelect: (ThreadViewMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            ThreadViewMode.SEGMENT_BAR to "━━━",
            ThreadViewMode.WAVEFORM    to "▌▌▌",
            ThreadViewMode.GRID        to "⊞⊞"
        ).forEach { (mode, icon) ->
            val selected = current == mode
            Box(
                modifier = Modifier
                    .clickable { onSelect(mode) }
                    .background(
                        if (selected) CyanPrimary.copy(alpha = 0.15f) else ElevatedSurf,
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        1.dp,
                        if (selected) CyanPrimary.copy(alpha = 0.6f) else BorderColor,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    icon,
                    color = if (selected) CyanPrimary else TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}