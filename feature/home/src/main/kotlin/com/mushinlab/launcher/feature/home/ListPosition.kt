package com.mushinlab.launcher.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

internal fun autoListOffset(naturalTop: Float, listHeight: Float, rowHeight: Float, maxOffset: Float): Float =
    minOf(naturalTop * (AUTO_DEPTH - 1), listHeight - rowHeight * MIN_VISIBLE_ROWS, maxOffset).coerceAtLeast(0f)

@Immutable
internal data class ListPlacement(
    val savedOffsetPx: Float?,
    val rowHeightPx: Float,
    val maxOffsetPx: Float,
    val handleHeightPx: Int,
)

internal fun homeColumnPolicy(placement: ListPlacement, handle: ListHandleDrag): MeasurePolicy =
    MeasurePolicy { measurables, constraints ->
        val loose = constraints.copy(minHeight = 0)
        val header = measurables[0].measure(loose)
        val bar = measurables[2].measure(loose)
        val space = (constraints.maxHeight - header.height - bar.height).coerceAtLeast(0)
        val listSpace = (space - placement.handleHeightPx).coerceAtLeast(0)
        val resting = placement.savedOffsetPx ?: with(placement) {
            autoListOffset(header.height.toFloat(), listSpace.toFloat(), rowHeightPx, maxOffsetPx)
        }
        val offset = handle.offsetPx(resting).roundToInt().coerceIn(0, listSpace)
        val list = measurables[1].measure(Constraints.fixed(constraints.maxWidth, space - offset))
        layout(constraints.maxWidth, constraints.maxHeight) {
            header.place(0, 0)
            list.place(0, header.height + offset)
            bar.place(0, constraints.maxHeight - bar.height)
        }
    }

@Composable
internal fun rememberRowHeight(textStyle: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(textStyle, density) {
        val textHeight = with(density) { measurer.measure("Ag", textStyle).size.height.toDp() }
        max(HomeDimens.rowHeight.value, (textHeight + 16.dp).value).dp
    }
}

private const val AUTO_DEPTH = 3
private const val MIN_VISIBLE_ROWS = 4
