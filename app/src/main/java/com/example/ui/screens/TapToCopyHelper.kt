package com.example.ui.screens

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ViewConfiguration
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

fun Modifier.tapToCopyGestures(
    viewConfiguration: ViewConfiguration,
    hasActiveSelection: Boolean,
    isTapOnText: (Offset) -> Boolean,
    onExtendSelection: () -> Unit,
    onDismissSelection: () -> Unit,
    onLineCapture: (Offset) -> Unit,
    onParaCapture: (Offset) -> Unit
): Modifier = this.pointerInput(hasActiveSelection) {
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var isCanceled = false

        if (hasActiveSelection) {
            var upEvent = false
            withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.first()
                    if ((change.position - down.position).getDistance() > slop) {
                        isCanceled = true
                        break
                    }
                    if (change.changedToUp()) {
                        upEvent = true
                        change.consume()
                        break
                    }
                }
            }
            if (!isCanceled && upEvent) {
                if (isTapOnText(down.position)) {
                    onExtendSelection()
                } else {
                    onDismissSelection()
                }
            }
            return@awaitEachGesture
        }

        var lineCaptured = false
        try {
            withTimeout(300L) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.first()
                    if ((change.position - down.position).getDistance() > slop) {
                        isCanceled = true
                        break
                    }
                    if (change.changedToUp()) {
                        isCanceled = true
                        break
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            if (!isCanceled && isTapOnText(down.position)) {
                lineCaptured = true
                onLineCapture(down.position)
            }
        }

        if (lineCaptured) {
            var paraCaptured = false
            try {
                withTimeout(400L) {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.first()
                        change.consume()
                        if ((change.position - down.position).getDistance() > slop) {
                            isCanceled = true
                            break
                        }
                        if (change.changedToUp()) {
                            break
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                if (!isCanceled) {
                    paraCaptured = true
                    onParaCapture(down.position)
                }
            }
            
            // Wait for release if not already released
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.first()
                change.consume()
                if (change.changedToUp()) {
                    break
                }
            }
        }
    }
}
