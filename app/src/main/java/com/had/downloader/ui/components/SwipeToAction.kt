package com.had.downloader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.had.downloader.ui.theme.GreenSuccess
import com.had.downloader.ui.theme.RedError

@Composable
fun <T> SwipeToAction(
    item: T,
    onSwipeLeft: (T) -> Unit,
    onSwipeRight: (T) -> Unit,
    onTap: (T) -> Unit,
    leftActionIcon: ImageVector = Icons.Filled.Delete,
    leftActionColor: Color = RedError,
    leftActionLabel: String = "Delete",
    rightActionIcon: ImageVector = Icons.Filled.PlayArrow,
    rightActionColor: Color = GreenSuccess,
    rightActionLabel: String = "Resume",
    threshold: Float = 0.3f,
    content: @Composable (T) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxSwipe = 200f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    offsetX < 0 -> leftActionColor.copy(alpha = 0.15f)
                    offsetX > 0 -> rightActionColor.copy(alpha = 0.15f)
                    else -> Color.Transparent
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    leftActionIcon,
                    null,
                    tint = leftActionColor.copy(alpha = if (offsetX < 0) 0.8f else 0f),
                    modifier = Modifier.size(24.dp)
                )
                AnimatedVisibility(
                    visible = offsetX < -50f,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        leftActionLabel,
                        color = leftActionColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AnimatedVisibility(
                    visible = offsetX > 50f,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        rightActionLabel,
                        color = rightActionColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    rightActionIcon,
                    null,
                    tint = rightActionColor.copy(alpha = if (offsetX > 0) 0.8f else 0f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.toInt(), 0) }
                .pointerInput(item) {
                    detectDragGestures(
                        onDragStart = { },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(-maxSwipe, maxSwipe)
                        },
                        onDragEnd = {
                            val thresholdPx = threshold * maxSwipe
                            when {
                                offsetX < -thresholdPx -> {
                                    onSwipeLeft(item)
                                    offsetX = 0f
                                }
                                offsetX > thresholdPx -> {
                                    onSwipeRight(item)
                                    offsetX = 0f
                                }
                                else -> {
                                    offsetX = 0f
                                }
                            }
                        }
                    )
                }
                .clickable { onTap(item) }
        ) {
            content(item)
        }
    }
}