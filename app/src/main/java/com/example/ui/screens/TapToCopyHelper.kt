package com.example.ui.screens

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ViewConfiguration
import kotlinx.coroutines.withTimeoutOrNull

private enum class HoldResult {
    TIMEOUT,
    RELEASED,
    MOVED
}

fun Modifier.tapToCopyGestures(
    viewConfiguration: ViewConfiguration,
    hasActiveSelection: () -> Boolean,
    isTapOnText: (Offset) -> Boolean,
    onExtendSelection: () -> Unit,
    onDismissSelection: () -> Unit,
    onLineCapture: (Offset) -> Unit,
    onParaCapture: (Offset) -> Unit,
    onCaptureGestureFinished: () -> Unit
): Modifier = pointerInput(Unit) {
    val slop = viewConfiguration.touchSlop

    suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.waitForStage(
        pointerId: androidx.compose.ui.input.pointer.PointerId,
        startPosition: Offset,
        timeoutMillis: Long,
        consumeChanges: Boolean
    ): HoldResult {
        val result: HoldResult? = withTimeoutOrNull(timeoutMillis) {
            var outcome: HoldResult? = null

            while (outcome == null) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == pointerId }

                if (change == null) {
                    outcome = HoldResult.RELEASED
                    continue
                }

                if ((change.position - startPosition).getDistance() > slop) {
                    outcome = HoldResult.MOVED
                    continue
                }

                if (consumeChanges) change.consume()

                if (change.changedToUp()) {
                    outcome = HoldResult.RELEASED
                }
            }

            outcome ?: HoldResult.RELEASED
        }

        return result ?: HoldResult.TIMEOUT
    }

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val pointerId = down.id
        val startPosition = down.position

        if (hasActiveSelection()) {
            // While a copied range is active, a quick stationary tap extends/dismisses.
            // Movement is deliberately left unconsumed so vertical drag scrolling wins.
            val quickTap: Boolean = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                var outcome: Boolean? = null

                while (outcome == null) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull { it.id == pointerId }

                    if (change == null) {
                        outcome = false
                        continue
                    }

                    if ((change.position - startPosition).getDistance() > slop) {
                        outcome = false
                        continue
                    }

                    if (change.changedToUp()) {
                        change.consume()
                        outcome = true
                    }
                }

                outcome ?: false
            } ?: false

            if (quickTap) {
                if (isTapOnText(startPosition)) onExtendSelection()
                else onDismissSelection()
            }
            return@awaitEachGesture
        }

        when (
            waitForStage(
                pointerId = pointerId,
                startPosition = startPosition,
                timeoutMillis = 300L,
                consumeChanges = false
            )
        ) {
            HoldResult.RELEASED -> {
                // Ordinary quick tap: leave it untouched for BasicTextField cursor placement.
                return@awaitEachGesture
            }
            HoldResult.MOVED -> {
                // Ordinary drag/scroll: leave it untouched.
                return@awaitEachGesture
            }
            HoldResult.TIMEOUT -> Unit
        }

        if (!isTapOnText(startPosition)) return@awaitEachGesture

        onLineCapture(startPosition)

        when (
            waitForStage(
                pointerId = pointerId,
                startPosition = startPosition,
                timeoutMillis = 400L,
                consumeChanges = true
            )
        ) {
            HoldResult.RELEASED -> {
                // The short-hold gesture is complete. Do not wait for another release.
                onCaptureGestureFinished()
                return@awaitEachGesture
            }
            HoldResult.MOVED -> {
                onCaptureGestureFinished()
                return@awaitEachGesture
            }
            HoldResult.TIMEOUT -> onParaCapture(startPosition)
        }

        // Paragraph capture occurred while the same finger is still down.
        // Consume only the remainder of this physical gesture, then finish.
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
            change.consume()
            if (change.changedToUp()) break
        }
        onCaptureGestureFinished()
    }
}
