package com.bluecubelabs.env.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

object PanelDimens {
    val width = 240.dp
    val margin = 8.dp
    val gap = 8.dp
    val radius = 12.dp
    const val MAX_HEIGHT_FRACTION = 0.6f
}

@Composable
fun AnchoredPanel(
    anchor: Offset,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalEnvColors.current
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val insets = WindowInsets.statusBars.union(WindowInsets.navigationBars).union(WindowInsets.displayCutout)
    val insetRect = IntRect(
        insets.getLeft(density, direction),
        insets.getTop(density),
        insets.getRight(density, direction),
        insets.getBottom(density),
    )
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(onDismiss) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    onDismiss()
                }
            },
    ) {
        Layout(
            content = {
                Column(
                    Modifier
                        .widthIn(max = PanelDimens.width)
                        .clip(RoundedCornerShape(PanelDimens.radius))
                        .background(colors.surface)
                        .border(1.dp, colors.divider, RoundedCornerShape(PanelDimens.radius))
                        .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() } }
                        .verticalScroll(rememberScrollState())
                        .testTag("popup"),
                ) { content() }
            },
        ) { measurables, constraints ->
            val window = IntSize(constraints.maxWidth, constraints.maxHeight)
            val safe = AnchoredPosition.safeArea(window, insetRect, PanelDimens.margin.roundToPx())
            val maxHeight = (safe.height * PanelDimens.MAX_HEIGHT_FRACTION).toInt().coerceAtLeast(0)
            val width = PanelDimens.width.roundToPx().coerceAtMost(safe.width.coerceAtLeast(0))
            val placeable = measurables.first().measure(Constraints.fixedWidth(width).copy(maxHeight = maxHeight))
            val position = AnchoredPosition.belowPoint(
                anchorX = anchor.x.toInt(),
                anchorY = anchor.y.toInt(),
                popup = IntSize(placeable.width, placeable.height),
                safe = safe,
                gapPx = PanelDimens.gap.roundToPx(),
            )
            layout(window.width, window.height) { placeable.place(position) }
        }
    }
}
