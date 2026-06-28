package com.touchfreeze.app.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen touch-blocking overlay for frozen state.
 *
 * Features:
 * - Blocks all touch from reaching underlying video player
 * - Detects hidden long-press gesture in top-right corner (80dp zone, 2.5s)
 * - Shows animated progress indicator during long-press
 */
@Composable
fun TouchBlockOverlay(
    isFrozen: Boolean,
    onUnlockGestureDetected: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isFrozen) return

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var isGestureInProgress by remember { mutableStateOf(false) }
    val gestureDurationMs = 2500L
    val progress = remember { Animatable(0f) }

    // Reset progress when gesture ends
    LaunchedEffect(isGestureInProgress) {
        if (!isGestureInProgress) {
            progress.animateTo(0f, tween(200))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (isGestureInProgress) 0.3f else 0.2f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        // Check if long press is in top-right corner (80dp zone)
                        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
                        val gestureZoneSizePx = with(density) { 80.dp.toPx() }
                        val zoneLeft = screenWidthPx - gestureZoneSizePx
                        val zoneTop = 0f
                        val zoneRight = screenWidthPx
                        val zoneBottom = gestureZoneSizePx

                        if (offset.x >= zoneLeft && offset.x <= zoneRight &&
                            offset.y >= zoneTop && offset.y <= zoneBottom) {
                            // Long press detected in gesture zone
                            isGestureInProgress = true
                            scope.launch {
                                // Progress animation loop
                                val gestureStartTime = System.currentTimeMillis()
                                while (isGestureInProgress) {
                                    val elapsed = System.currentTimeMillis() - gestureStartTime
                                    val normalizedProgress = (elapsed / gestureDurationMs.toFloat()).coerceIn(0f, 1f)

                                    if (normalizedProgress >= 1f) {
                                        onUnlockGestureDetected()
                                        isGestureInProgress = false
                                    } else {
                                        progress.snapTo(normalizedProgress)
                                    }

                                    delay(50)
                                }
                            }
                        }
                    }
                )
            }
    ) {
        // Animated progress indicator in top-right corner
        if (isGestureInProgress) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            MaterialTheme.shapes.medium
                        )
                ) {
                    CircularProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier.size(44.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}
