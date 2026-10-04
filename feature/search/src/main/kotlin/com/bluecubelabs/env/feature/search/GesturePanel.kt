package com.bluecubelabs.env.feature.search

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bluecubelabs.env.core.search.GestureDecoder
import com.bluecubelabs.env.core.search.GesturePoint
import com.bluecubelabs.env.core.search.KeyboardLayouts
import com.bluecubelabs.env.core.ui.EnvType
import com.bluecubelabs.env.core.ui.LocalEnvColors
import kotlinx.coroutines.withTimeoutOrNull

object GestureDimens {
    val height = (KeyboardDimens.rowHeight * KeyboardLayouts.LETTER_ROWS + KeyboardDimens.verticalPadding * 2) * 2
    val trailWidth = 6.dp
    const val DWELL_MS = 1000L
}

@Composable
internal fun GesturePanel(
    controller: SearchController,
    haptics: Boolean,
    showLetters: Boolean,
    shiftPenalty: Float,
    suggestMargin: Float,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEnvColors.current
    val density = LocalDensity.current
    val view = LocalView.current
    val letters = remember {
        KeyboardLayouts.EN.let { it.copy(rows = it.rows.take(KeyboardLayouts.LETTER_ROWS)) }
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val trail = remember { mutableStateListOf<Offset>() }
    val dwells = remember { mutableStateListOf<Offset>() }
    val currentHaptics by rememberUpdatedState(haptics)
    LaunchedEffect(controller.session) {
        trail.clear()
        dwells.clear()
    }
    val paddingPx = with(density) { KeyboardDimens.verticalPadding.toPx() }
    val centers = remember(size) {
        KeyboardGeometry.compute(
            letters,
            size.width.toFloat(),
            (size.height - paddingPx * 2) / KeyboardLayouts.LETTER_ROWS,
            paddingPx,
            with(density) { KeyboardDimens.gap.toPx() },
        ).mapNotNull { key -> key.spec.char?.let { it to key.rect.center } }.toMap()
    }
    val decoder = remember(centers, shiftPenalty, suggestMargin) {
        GestureDecoder(
            centers.mapValues { (_, center) ->
                GesturePoint(center.x, center.y)
            },
            shiftPenalty,
            suggestMargin,
        )
    }
    val measurer = rememberTextMeasurer()
    val labels = remember(measurer, colors) {
        val style = EnvType.style(EnvType.keySize, colors.textSecondary.copy(alpha = LETTERS_ALPHA))
        letters.rows.flatten().mapNotNull { it.char }.associateWith { measurer.measure(it.toString(), style) }
    }
    val radiusPx = with(density) { KeyboardDimens.radius.toPx() }
    val strokePx = with(density) { GestureDimens.trailWidth.toPx() }
    Box(
        modifier
            .fillMaxWidth()
            .height(GestureDimens.height)
            .onSizeChanged { size = it }
            .systemGestureExclusion()
            .testTag("gesture_panel")
            .pointerInput(decoder) {
                val dwellRadius = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    trail.clear()
                    dwells.clear()
                    val points = mutableListOf(GesturePoint(down.position.x, down.position.y))
                    trail += down.position
                    var anchor = down.position
                    var anchorTime = down.uptimeMillis
                    var lastTime = down.uptimeMillis
                    fun dwell() {
                        points += GesturePoint(anchor.x, anchor.y, repeat = true)
                        dwells += anchor
                        if (currentHaptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    var lifted = false
                    while (!lifted) {
                        val wait = (anchorTime + GestureDimens.DWELL_MS - lastTime).coerceAtLeast(1L)
                        val event = withTimeoutOrNull(wait) { awaitPointerEvent() }
                        val change = event?.changes?.firstOrNull { it.id == down.id }
                        when {
                            event == null -> {
                                dwell()
                                anchorTime += GestureDimens.DWELL_MS
                                lastTime = anchorTime
                            }

                            change == null || !change.pressed -> {
                                change?.consume()
                                lifted = true
                            }

                            else -> {
                                change.consume()
                                lastTime = change.uptimeMillis
                                points += GesturePoint(change.position.x, change.position.y)
                                trail += change.position
                                if ((change.position - anchor).getDistance() > dwellRadius) {
                                    anchor = change.position
                                    anchorTime = change.uptimeMillis
                                } else if (change.uptimeMillis - anchorTime >= GestureDimens.DWELL_MS) {
                                    dwell()
                                    anchorTime = change.uptimeMillis
                                }
                            }
                        }
                    }
                    if (!controller.applyGesture(decoder, points) && currentHaptics) {
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    }
                }
            }
            .drawBehind {
                drawRoundRect(
                    color = colors.keyBackground,
                    topLeft = Offset(paddingPx, paddingPx),
                    size = Size(size.width - paddingPx * 2, size.height - paddingPx * 2),
                    cornerRadius = CornerRadius(radiusPx),
                )
                if (showLetters) drawLetters(centers, labels)
                if (trail.size > 1) {
                    val path = Path().apply {
                        moveTo(trail.first().x, trail.first().y)
                        trail.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path,
                        colors.accent,
                        style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
                dwells.forEach { drawCircle(colors.accent, radius = strokePx * 2, center = it) }
            },
    )
}

private fun DrawScope.drawLetters(centers: Map<Char, Offset>, labels: Map<Char, TextLayoutResult>) {
    centers.forEach { (char, center) ->
        val label = labels.getValue(char)
        drawText(label, topLeft = center - Offset(label.size.width / 2f, label.size.height / 2f))
    }
}

private const val LETTERS_ALPHA = 0.6f
