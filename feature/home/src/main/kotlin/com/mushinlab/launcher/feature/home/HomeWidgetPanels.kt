package com.mushinlab.launcher.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.model.WidgetPanelPosition
import com.mushinlab.launcher.core.model.WidgetPanelSize
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable
import com.mushinlab.launcher.feature.home.gesture.RowRegistry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class HomeWidgetTile(val id: Int, val label: String)

@Immutable
data class HomeWidgetPanel(
    val size: WidgetPanelSize = WidgetPanelSize.MEDIUM,
    val clock: Boolean = false,
    val widgets: ImmutableList<HomeWidgetTile> = persistentListOf(),
) {
    fun hasContent(showClock: Boolean): Boolean = (clock && showClock) || widgets.isNotEmpty()
}

@Immutable
data class HomeWidgetPanels(
    val top: HomeWidgetPanel = HomeWidgetPanel(clock = true),
    val bottom: HomeWidgetPanel = HomeWidgetPanel(),
)

typealias WidgetContent = @Composable (id: Int, height: Dp, maxWidth: Dp) -> Unit

@Composable
internal fun HomeWidgetPanel(
    panel: HomeWidgetPanel,
    position: WidgetPanelPosition,
    height: Dp,
    maxWidth: Dp,
    showClock: Boolean,
    editing: Boolean,
    registry: RowRegistry,
    onAdd: () -> Unit,
    widgets: WidgetContent,
    clock: @Composable () -> Unit,
) {
    val hasContent = panel.hasContent(showClock)
    Column(Modifier.fillMaxWidth().testTag("widget_area_$position")) {
        if (hasContent) {
            Row(
                Modifier.fillMaxWidth().height(height).clipToBounds()
                    .horizontalScroll(
                        rememberScrollState(),
                        enabled = panel.widgets.isNotEmpty(),
                    ).testTag("widget_panel_$position"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (panel.clock && showClock) {
                    val width = if (panel.widgets.isEmpty()) maxWidth else minOf(maxWidth, CLOCK_TILE_WIDTH)
                    Box(Modifier.width(width)) { clock() }
                }
                panel.widgets.forEach { widget ->
                    val key = "widget_${widget.id}"
                    DisposableEffect(key) { onDispose { registry.block(key, null) } }
                    Box(
                        Modifier.fillMaxHeight().onGloballyPositioned { registry.block(key, it.boundsInRoot()) }
                            .testTag(key),
                    ) { widgets(widget.id, height, maxWidth) }
                }
            }
        }
        if (editing) AddWidgetButton(position, registry, onAdd)
    }
}

@Composable
private fun AddWidgetButton(position: WidgetPanelPosition, registry: RowRegistry, onClick: () -> Unit) {
    val key = "add_widget_$position"
    DisposableEffect(key) { onDispose { registry.block(key, null) } }
    Box(
        Modifier.fillMaxWidth().height(ADD_WIDGET_HEIGHT).background(LocalMushinColors.current.surface)
            .onGloballyPositioned { registry.block(key, it.boundsInRoot()) }
            .pressable(onClick = onClick).testTag(key).padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        SingleLineText(
            stringResource(R.string.home_add_widget),
            MushinType.style(MushinType.secondarySize, LocalMushinColors.current.accent),
        )
    }
}

internal fun panelHeights(top: Float, bottom: Float, budget: Float): Pair<Float, Float> {
    val total = top + bottom
    val scale = if (total > 0f) (budget.coerceAtLeast(0f) / total).coerceAtMost(1f) else 0f
    return top * scale to bottom * scale
}

internal val ADD_WIDGET_HEIGHT = 48.dp
private val CLOCK_TILE_WIDTH = 300.dp
