package com.had.downloader.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.had.downloader.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun CountdownBadge(epochMillis: Long, modifier: Modifier = Modifier) {
    var remaining by remember { mutableStateOf("") }
    LaunchedEffect(epochMillis) {
        while (true) {
            val ms = epochMillis - System.currentTimeMillis()
            remaining = if (ms <= 0) "Starting..." else {
                val s = ms / 1000; val m = s / 60; val h = m / 60; val d = h / 24
                when {
                    d > 0 -> "${d}d ${h % 24}h ${m % 60}m"
                    h > 0 -> "${h}h ${m % 60}m ${s % 60}s"
                    m > 0 -> "${m}m ${s % 60}s"
                    else  -> "${s}s"
                }
            }
            delay(1000L)
        }
    }
    Row(
        modifier = modifier
            .background(OrangeWarn.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .border(1.dp, OrangeWarn.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val pulse by rememberInfiniteTransition(label = "dot").animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "dotAlpha"
        )
        Box(modifier = Modifier.size(7.dp).background(OrangeWarn.copy(alpha = pulse), CircleShape))
        Icon(Icons.Outlined.Schedule, null, tint = OrangeWarn, modifier = Modifier.size(13.dp))
        Text(
            "Starts in $remaining",
            color = OrangeWarn,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}