package com.mushinlab.launcher.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

internal fun homeColumnPolicy(): MeasurePolicy = MeasurePolicy { measurables, constraints ->
    val loose = constraints.copy(minHeight = 0)
    val bar = measurables[BAR_INDEX].measure(loose)
    val header = measurables[0].measure(loose)
    val space = (constraints.maxHeight - header.height - bar.height).coerceAtLeast(0)
    val list = measurables[1].measure(Constraints.fixed(constraints.maxWidth, space))
    layout(constraints.maxWidth, constraints.maxHeight) {
        header.place(0, 0)
        list.place(0, header.height)
        bar.place(0, constraints.maxHeight - bar.height)
    }
}

@Composable
internal fun rememberRowHeight(textStyle: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(textStyle, density) {
        val textHeight = with(density) { measurer.measure("Ag", textStyle).size.height.toDp() }
        max(HomeDimens.minRowHeight.value, textHeight.value * ROW_TO_TEXT).dp
    }
}

private const val ROW_TO_TEXT = 2.1f

private const val BAR_INDEX = 2
