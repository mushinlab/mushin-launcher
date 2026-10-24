package com.mushinlab.launcher.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import com.mushinlab.launcher.core.model.HomeTextAlign
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.ui.Motion

internal object EditList {
    const val ROWS = 5

    fun shown(index: Int, editing: Boolean, dragging: Boolean): Boolean = !editing || dragging || index < ROWS
}

@Composable
internal fun Reveal(visible: Boolean, content: @Composable () -> Unit) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(Motion.TRANSITION_MS), label = "reveal")
    Box(Modifier.graphicsLayer { this.alpha = alpha }) { content() }
}

@Immutable
internal data class RowLayout(val iconMode: IconMode, val align: HomeTextAlign) {
    val icons: Boolean get() = iconMode != IconMode.NONE
}

@Composable
internal fun LabelWithIcon(
    align: HomeTextAlign,
    modifier: Modifier,
    icon: (@Composable () -> Unit)?,
    label: @Composable RowScope.() -> Unit,
) {
    val gap = with(LocalDensity.current) { HomeDimens.iconGap.roundToPx() }
    Layout(
        content = {
            Row(verticalAlignment = Alignment.CenterVertically, content = label)
            icon?.invoke()
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val iconPlaceable = measurables.getOrNull(1)?.measure(Constraints())
        val reserve = iconPlaceable?.let { it.width + gap } ?: 0
        val labelMax = if (align == HomeTextAlign.CENTER) width - reserve * 2 else width - reserve
        val labelPlaceable = measurables[0].measure(Constraints(maxWidth = labelMax.coerceAtLeast(0)))
        val height = if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            maxOf(labelPlaceable.height, iconPlaceable?.height ?: 0)
        }
        val left = when (align) {
            HomeTextAlign.START -> 0
            HomeTextAlign.CENTER -> (width - labelPlaceable.width) / 2
            HomeTextAlign.END -> width - reserve - labelPlaceable.width
        }
        layout(width, height) {
            labelPlaceable.place(left, (height - labelPlaceable.height) / 2)
            iconPlaceable?.place(left + labelPlaceable.width + gap, (height - iconPlaceable.height) / 2)
        }
    }
}
