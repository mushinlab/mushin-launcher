package com.bluecubelabs.env.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bluecubelabs.env.core.search.GestureDecoder
import com.bluecubelabs.env.core.search.GesturePoint
import com.bluecubelabs.env.core.search.KeyboardLayouts
import com.bluecubelabs.env.core.ui.LocalEnvColors

@Composable
internal fun GesturePanel(vocabulary: () -> List<String>, onWord: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEnvColors.current
    val density = LocalDensity.current
    val layout = KeyboardLayouts.EN
    var size by remember { mutableStateOf(IntSize.Zero) }
    val trail = remember { mutableStateListOf<Offset>() }
    val currentOnWord by rememberUpdatedState(onWord)
    val currentVocabulary by rememberUpdatedState(vocabulary)
    val paddingPx = with(density) { KeyboardDimens.verticalPadding.toPx() }
    val decoder = remember(size) {
        val keys = KeyboardGeometry.compute(
            layout,
            size.width.toFloat(),
            with(density) { KeyboardDimens.rowHeight.toPx() },
            paddingPx,
            with(density) { KeyboardDimens.gap.toPx() },
        )
        GestureDecoder(
            keys.mapNotNull { key -> key.spec.char?.let { it to GesturePoint(key.rect.center.x, key.rect.center.y) } }
                .toMap(),
        )
    }
    val radiusPx = with(density) { KeyboardDimens.radius.toPx() }
    val strokePx = with(density) { TRAIL_WIDTH.toPx() }
    Box(
        modifier
            .fillMaxWidth()
            .height(KeyboardDimens.rowHeight * layout.rows.size + KeyboardDimens.verticalPadding * 2)
            .background(colors.background)
            .onSizeChanged { size = it }
            .testTag("gesture_panel")
            .pointerInput(decoder) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    trail.clear()
                    trail += down.position
                    var change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    while (change != null && change.pressed) {
                        change.consume()
                        trail += change.position
                        change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    }
                    change?.consume()
                    decoder.decode(trail.map { GesturePoint(it.x, it.y) }, currentVocabulary())?.let(currentOnWord)
                }
            }
            .drawBehind {
                drawRoundRect(
                    color = colors.keyBackground,
                    topLeft = Offset(paddingPx, paddingPx),
                    size = Size(size.width - paddingPx * 2, size.height - paddingPx * 2),
                    cornerRadius = CornerRadius(radiusPx),
                )
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
            },
    )
}

private val TRAIL_WIDTH = 6.dp
