package com.mushinlab.launcher.feature.search

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinColors
import kotlin.math.abs

private enum class Anchor { START, END, MOVE }

private class Grip(val inset: Float, val touch: Float) {
    fun anchorAt(p: Offset, frame: KeyboardFrame, height: Float): Anchor? = when {
        abs(p.x - frame.left - inset) < touch -> Anchor.START
        abs(p.x - frame.right + inset) < touch -> Anchor.END
        abs(p.y - height + inset) < touch && p.x in frame.left..frame.right -> Anchor.MOVE
        else -> null
    }
}

@Composable
internal fun ResizeOverlay(
    frame: KeyboardFrame,
    total: Float,
    onDrag: (KeyboardFrame) -> Unit,
    onRelease: (KeyboardFrame) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalMushinColors.current
    val current by rememberUpdatedState(frame)
    val density = LocalDensity.current
    val view = LocalView.current
    val grip = Grip(
        inset = with(density) { SearchDimens.anchorInset.toPx() },
        touch = with(density) { SearchDimens.anchorTouch.toPx() },
    )
    var grabbed by remember { mutableStateOf<Anchor?>(null) }
    Canvas(
        modifier
            .testTag("resize_overlay")
            .pointerInput(total) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val start = current
                    val anchor = grip.anchorAt(down.position, start, size.height.toFloat())
                    if (anchor != null) {
                        grabbed = anchor
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    val moved = trackDrag(down, start, anchor, total, onDrag)
                    grabbed = null
                    if (anchor == null) {
                        onExit()
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                        onRelease(moved)
                    }
                }
            },
    ) {
        drawFrame(frame, grabbed, colors, grip.inset)
    }
}

private suspend fun AwaitPointerEventScope.trackDrag(
    down: PointerInputChange,
    start: KeyboardFrame,
    anchor: Anchor?,
    total: Float,
    onDrag: (KeyboardFrame) -> Unit,
): KeyboardFrame {
    var moved = start
    var change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
    while (change != null && change.pressed) {
        change.consume()
        moved = start.dragged(anchor, change.position.x - down.position.x, total)
        if (anchor != null) onDrag(moved)
        change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
    }
    change?.consume()
    return moved
}

private fun KeyboardFrame.dragged(anchor: Anchor?, dx: Float, total: Float): KeyboardFrame = when (anchor) {
    Anchor.START -> dragStart(dx, total)
    Anchor.END -> dragEnd(dx, total)
    Anchor.MOVE -> move(dx, total)
    null -> this
}

private fun DrawScope.drawFrame(frame: KeyboardFrame, grabbed: Anchor?, colors: MushinColors, inset: Float) {
    val length = SearchDimens.anchorLength.toPx()
    val thickness = SearchDimens.anchorThickness.toPx()
    val radius = SearchDimens.paneRadius.toPx()
    drawRoundRect(
        colors.textSecondary,
        cornerRadius = CornerRadius(radius),
        style = Stroke(
            width = BOUNDS_STROKE.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH.toPx(), DASH.toPx())),
        ),
    )
    drawRect(colors.background.copy(alpha = RESIZE_SCRIM), Offset(frame.left, 0f), Size(frame.width, size.height))
    fun anchorColor(anchor: Anchor) = if (grabbed == anchor) colors.textPrimary else colors.accent
    fun anchorWidth(anchor: Anchor) = if (grabbed == anchor) thickness * GRABBED_SCALE else thickness
    val mid = size.height / 2
    listOf(Anchor.START to frame.left + inset, Anchor.END to frame.right - inset).forEach { (anchor, x) ->
        drawLine(
            anchorColor(anchor),
            Offset(x, mid - length / 2),
            Offset(x, mid + length / 2),
            anchorWidth(anchor),
            StrokeCap.Round,
        )
    }
    val center = frame.left + frame.width / 2
    val bottom = size.height - inset
    drawLine(
        anchorColor(Anchor.MOVE),
        Offset(center - length / 2, bottom),
        Offset(center + length / 2, bottom),
        anchorWidth(Anchor.MOVE),
        StrokeCap.Round,
    )
}

private const val RESIZE_SCRIM = 0.55f
private const val GRABBED_SCALE = 1.6f
private val BOUNDS_STROKE = 1.5.dp
private val DASH = 6.dp
