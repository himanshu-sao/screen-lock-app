package com.touchfreeze.app.ui.unlock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * Enhanced unlock bottom sheet with:
 * - PIN digit circles that fill as user types
 * - Shake animation on incorrect PIN
 * - Haptic feedback on digit press and error
 * - Smooth animations
 */
@OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun UnlockBottomSheet(
    onPinEntered: (String) -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null,
    showError: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    var pin by remember { mutableStateOf("") }
    var isShaking by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }

    val pinLength = 4
    val digitSize = 56.dp

    // Shake animation on error
    LaunchedEffect(showError) {
        if (showError) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            coroutineScope {
                launch {
                    shakeOffset.animateTo(10f, tween(50))
                    shakeOffset.animateTo(-10f, tween(50))
                    shakeOffset.animateTo(10f, tween(50))
                    shakeOffset.animateTo(-10f, tween(50))
                    shakeOffset.animateTo(0f, tween(100))
                }
            }
            // Clear error after delay
            kotlinx.coroutines.delay(2000)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp)
                .offset { IntOffset(shakeOffset.value.toInt(), 0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Enter PIN",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // PIN digit circles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pinLength) { index ->
                        PINDigitCircle(
                            filled = index < pin.length,
                            digit = if (index < pin.length) pin[index].toString() else null,
                            size = digitSize,
                            isError = showError && index == pin.length - 1
                        )
                    }
                }

                // Error message
                if (errorMessage != null && showError) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // PIN pad
                PINPad(
                    onDigitClick = { digit ->
                        if (pin.length < pinLength) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            pin += digit
                        }
                    },
                    onBackspaceClick = {
                        if (pin.isNotEmpty()) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            pin = pin.dropLast(1)
                        }
                    },
                    onSubmit = {
                        if (pin.length == pinLength) {
                            onPinEntered(pin)
                            if (!showError) pin = ""
                        }
                    },
                    pinLength = pin.length,
                    showSubmit = pin.length == pinLength
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Cancel", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun PINDigitCircle(
    filled: Boolean,
    digit: String?,
    size: androidx.compose.ui.unit.Dp,
    isError: Boolean = false
) {
    val borderColor = if (isError) Color(0xFFE53935) else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) borderColor else Color.Transparent)
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (digit != null) {
            Text(
                text = digit,
                fontSize = 24.sp,
                color = if (filled) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PINPad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onSubmit: () -> Unit,
    pinLength: Int,
    showSubmit: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PINRow(listOf("1", "2", "3"), onDigitClick)
        PINRow(listOf("4", "5", "6"), onDigitClick)
        PINRow(listOf("7", "8", "9"), onDigitClick)
        PINRow(
            listOf("", "0", ""),
            onDigitClick,
            onBackspaceClick,
            onSubmit,
            pinLength,
            showSubmit
        )
    }
}

@Composable
fun PINRow(
    digits: List<String>,
    onClick: (String) -> Unit,
    onBackspace: (() -> Unit)? = null,
    onSubmit: (() -> Unit)? = null,
    pinLength: Int = 0,
    showSubmit: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        digits.forEachIndexed { index, digit ->
            when {
                digit.isEmpty() -> Spacer(modifier = Modifier.weight(1f))
                index == 1 && onBackspace != null && onSubmit != null -> {
                    if (showSubmit) SubmitButton(onSubmit)
                    else BackspaceButton(onBackspace)
                }
                else -> PINButton(digit, { onClick(digit) })
            }
        }
    }
}

@Composable
fun PINButton(digit: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(64.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center
    ) {
        Text(text = digit, fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun BackspaceButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(64.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center
    ) {
        Text("⌫", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SubmitButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(64.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center
    ) {
        Text("→", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
    }
}