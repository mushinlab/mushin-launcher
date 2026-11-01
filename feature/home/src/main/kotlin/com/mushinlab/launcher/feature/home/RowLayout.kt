package com.mushinlab.launcher.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.model.HomeTextAlign
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.feature.home.gesture.RowRegistry

internal object EditList {
    const val ROWS = 5
    const val PEEK = 0.5f

    fun viewportRows(rows: Int): Float = if (rows <= ROWS) rows.toFloat() else ROWS + PEEK
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
        val labelMax = width - reserve
        val labelPlaceable = measurables[0].measure(Constraints(maxWidth = labelMax.coerceAtLeast(0)))
        val height = if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            maxOf(labelPlaceable.height, iconPlaceable?.height ?: 0)
        }
        val left = when (align) {
            HomeTextAlign.START -> 0
            HomeTextAlign.CENTER -> (width - labelPlaceable.width - reserve) / 2
            HomeTextAlign.END -> width - reserve - labelPlaceable.width
        }
        layout(width, height) {
            labelPlaceable.place(left, (height - labelPlaceable.height) / 2)
            iconPlaceable?.place(left + labelPlaceable.width + gap, (height - iconPlaceable.height) / 2)
        }
    }
}

@Immutable
internal data class ListEditing(val editing: Boolean, val dragging: Boolean) {
    fun rowsInView(rows: Int): Float = if (editing && !dragging) EditList.viewportRows(rows) else rows.toFloat()
}

@Composable
internal fun ScrollingRows(
    rows: Int,
    rowStyle: RowStyle,
    editing: ListEditing,
    registry: RowRegistry,
    content: @Composable () -> Unit,
) {
    val scroll = rememberScrollState()
    val inView by animateFloatAsState(editing.rowsInView(rows), tween(Motion.TRANSITION_MS), label = "rows")
    val scrolls = editing.editing && !editing.dragging && rows > EditList.ROWS
    LaunchedEffect(editing.editing) { if (!editing.editing) scroll.scrollTo(0) }
    DisposableEffect(scrolls) { onDispose { registry.block(EDIT_LIST, null) } }
    val bar = LocalMushinColors.current.textSecondary
    Column(
        Modifier
            .fillMaxWidth()
            .height(rowStyle.height * inView)
            .onGloballyPositioned { if (scrolls) registry.block(EDIT_LIST, it.boundsInRoot()) }
            .scrollBar(scroll, bar.copy(alpha = SCROLL_BAR_ALPHA), visible = scrolls)
            .verticalScroll(scroll, enabled = scrolls)
            .testTag(EDIT_LIST),
    ) {
        content()
    }
}

private fun Modifier.scrollBar(scroll: ScrollState, color: Color, visible: Boolean): Modifier = drawWithContent {
    drawContent()
    val max = scroll.maxValue
    if (!visible || max <= 0 || max == Int.MAX_VALUE) return@drawWithContent
    val width = SCROLL_BAR_WIDTH.toPx()
    val viewport = size.height
    val length = viewport * viewport / (viewport + max)
    drawRoundRect(
        color,
        topLeft = Offset(size.width - width - SCROLL_BAR_INSET.toPx(), (viewport - length) * scroll.value / max),
        size = Size(width, length),
        cornerRadius = CornerRadius(width / 2),
    )
}

private const val EDIT_LIST = "edit_list"
private const val SCROLL_BAR_ALPHA = 0.6f
private val SCROLL_BAR_WIDTH = 4.dp
private val SCROLL_BAR_INSET = 6.dp
