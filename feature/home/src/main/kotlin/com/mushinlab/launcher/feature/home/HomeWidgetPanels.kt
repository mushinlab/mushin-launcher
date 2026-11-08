package com.mushinlab.launcher.feature.home

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mushinlab.launcher.core.model.HostedWidget
import com.mushinlab.launcher.core.model.WidgetArrangement
import com.mushinlab.launcher.core.model.WidgetPanelMode
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.GlyphWrapper
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.PopupStyle
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable
import com.mushinlab.launcher.feature.home.gesture.RowRegistry
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class HomeWidgetTile(val id: Int, val label: String) {
    val isClock: Boolean get() = id == HostedWidget.CLOCK_ID
}

@Immutable
data class HomeWidgetPanel(
    val mode: WidgetPanelMode = WidgetPanelMode.STANDARD,
    val arrangement: WidgetArrangement = WidgetArrangement.ROW,
    val widgets: ImmutableList<HomeWidgetTile> = persistentListOf(HomeWidgetTile(HostedWidget.CLOCK_ID, "")),
) {
    val columns: Int get() = if (mode == WidgetPanelMode.STANDARD && arrangement == WidgetArrangement.COLUMN) 1 else 2
}

@Immutable
data class HomeWidgetPanels(val top: HomeWidgetPanel = HomeWidgetPanel())

typealias WidgetContent = @Composable (id: Int, height: Dp, maxWidth: Dp) -> Unit

@Immutable
internal class PanelSlots(
    val widgets: WidgetContent,
    val clock: @Composable (editing: Boolean) -> Unit,
)

@Immutable
internal class PanelEdits(
    val onAdd: () -> Unit,
    val onMove: (Int, Int) -> Unit,
    val onRemove: (Int) -> Unit,
    val onEditClock: () -> Unit,
)

@Composable
internal fun HomeWidgetPanel(
    panel: HomeWidgetPanel,
    maxWidth: Dp,
    editing: Boolean,
    registry: RowRegistry,
    edits: PanelEdits,
    slots: PanelSlots,
) {
    val tiles = panel.widgets.take(panel.mode.capacity)
    val canAdd = editing && panel.widgets.size < panel.mode.capacity
    val count = tiles.size + if (canAdd) 1 else 0
    if (count == 0) return
    val width = (maxWidth - HomeDimens.rowPadding * 2 - WIDGET_GAP) / 2
    var dragging by remember(editing) { mutableStateOf<Int?>(null) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = HomeDimens.rowPadding, vertical = WIDGET_GAP)
            .testTag("widget_panel_TOP"),
        verticalArrangement = Arrangement.spacedBy(WIDGET_GAP),
    ) {
        for (row in (0 until count).chunked(panel.columns)) {
            Row(
                Modifier.zIndex(if (dragging in row) 1f else 0f),
                horizontalArrangement = Arrangement.spacedBy(WIDGET_GAP),
            ) {
                row.forEach { index ->
                    if (index < tiles.size) {
                        WidgetTile(
                            TilePlace(tiles[index], index, tiles.size, panel.columns, width),
                            editing,
                            registry,
                            edits,
                            slots,
                        ) { dragging = it }
                    } else {
                        AddWidgetCell(width, registry, edits.onAdd)
                    }
                }
            }
        }
    }
}

@Immutable
private class TilePlace(val tile: HomeWidgetTile, val index: Int, val count: Int, val columns: Int, val width: Dp)

@Composable
private fun WidgetTile(
    place: TilePlace,
    editing: Boolean,
    registry: RowRegistry,
    edits: PanelEdits,
    slots: PanelSlots,
    onDragging: (Int?) -> Unit,
) {
    val tile = place.tile
    val key = "widget_${tile.id}"
    var drag by remember(tile.id, editing) { mutableStateOf<Offset?>(null) }
    val blocks = editing || !tile.isClock
    DisposableEffect(key, blocks) { onDispose { registry.block(key, null) } }
    Box(
        Modifier.width(place.width).height(WIDGET_HEIGHT).zIndex(if (drag != null) 1f else 0f)
            .offset { drag?.let { IntOffset(it.x.roundToInt(), it.y.roundToInt()) } ?: IntOffset.Zero }
            .onGloballyPositioned { if (blocks) registry.block(key, it.boundsInRoot()) }
            .testTag(key),
    ) {
        Box(Modifier.fillMaxSize().clipToBounds().alpha(if (editing) EDIT_CONTENT_ALPHA else 1f)) {
            if (tile.isClock) slots.clock(editing) else slots.widgets(tile.id, WIDGET_HEIGHT, place.width)
        }
        if (editing) {
            Box(
                Modifier.fillMaxSize()
                    .border(1.dp, LocalMushinColors.current.accent, TILE_SHAPE)
                    .widgetMoveActions(place.index, place.count, tile.id, edits.onMove)
                    .dragToReorder(place, onDragging, { drag = it }, edits.onMove),
            )
            if (tile.isClock) EditClockButton(tile, edits, Modifier.align(Alignment.Center))
            RemoveTileButton(tile, edits, Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun Modifier.dragToReorder(
    place: TilePlace,
    onDragging: (Int?) -> Unit,
    onOffset: (Offset?) -> Unit,
    onMove: (Int, Int) -> Unit,
): Modifier {
    val step = with(LocalDensity.current) {
        Offset((place.width + WIDGET_GAP).toPx(), (WIDGET_HEIGHT + WIDGET_GAP).toPx())
    }
    return pointerInput(place.tile.id, place.index, place.columns, place.count, step) {
        var delta = Offset.Zero
        fun reset() {
            delta = Offset.Zero
            onOffset(null)
            onDragging(null)
        }
        detectDragGesturesAfterLongPress(
            onDragStart = {
                delta = Offset.Zero
                onOffset(delta)
                onDragging(place.index)
            },
            onDragCancel = ::reset,
            onDragEnd = {
                val target =
                    widgetDropIndex(place.index, delta.x / step.x, delta.y / step.y, place.columns, place.count)
                if (target != place.index) onMove(place.tile.id, target)
                reset()
            },
            onDrag = { change, amount ->
                change.consume()
                delta += amount
                onOffset(delta)
            },
        )
    }
}

@Composable
private fun EditClockButton(tile: HomeWidgetTile, edits: PanelEdits, modifier: Modifier) {
    GlyphButton(
        Glyph.EDIT,
        stringResource(R.string.home_edit_clock),
        LocalMushinColors.current.textPrimary,
        tag = "tile_edit_${tile.id}",
        modifier = modifier,
        onClick = edits.onEditClock,
    )
}

@Composable
private fun RemoveTileButton(tile: HomeWidgetTile, edits: PanelEdits, modifier: Modifier) {
    GlyphButton(
        Glyph.DELETE,
        stringResource(R.string.widget_remove),
        LocalMushinColors.current.textPrimary,
        tag = "tile_remove_${tile.id}",
        modifier = modifier.padding(TILE_BUTTON_INSET),
        size = TILE_BUTTON,
        glyphSize = TILE_GLYPH,
    ) { edits.onRemove(tile.id) }
}

@Composable
private fun Modifier.widgetMoveActions(index: Int, count: Int, id: Int, onMove: (Int, Int) -> Unit): Modifier {
    val moveBefore = stringResource(R.string.widget_move_before)
    val moveAfter = stringResource(R.string.widget_move_after)
    return semantics {
        customActions = buildList {
            if (index > 0) add(CustomAccessibilityAction(moveBefore) { onMove(id, index - 1).let { true } })
            if (index + 1 < count) add(CustomAccessibilityAction(moveAfter) { onMove(id, index + 1).let { true } })
        }
    }
}

internal fun widgetDropIndex(index: Int, dx: Float, dy: Float, columns: Int, count: Int): Int {
    val column = (index % columns + dx.roundToInt()).coerceIn(0, columns - 1)
    val row = (index / columns + dy.roundToInt()).coerceIn(0, (count - 1) / columns)
    return (row * columns + column).coerceIn(0, count - 1)
}

@Composable
private fun AddWidgetCell(width: Dp, registry: RowRegistry, onClick: () -> Unit) {
    val key = "add_widget_TOP"
    val colors = LocalMushinColors.current
    DisposableEffect(key) { onDispose { registry.block(key, null) } }
    Column(
        Modifier.width(width).height(WIDGET_HEIGHT)
            .clip(TILE_SHAPE)
            .border(GlyphWrapper.outline, PopupStyle.outline(colors), TILE_SHAPE)
            .onGloballyPositioned { registry.block(key, it.boundsInRoot()) }
            .pressable(onClick = onClick)
            .testTag(key),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GlyphBadge(Glyph.ADD, ADD_BADGE)
        Spacer(Modifier.size(8.dp))
        SingleLineText(
            stringResource(R.string.home_add_widget),
            MushinType.style(MushinType.secondarySize, colors.textPrimary),
        )
    }
}

internal val WIDGET_HEIGHT = 112.dp
private val WIDGET_GAP = 8.dp
private val TILE_SHAPE = RoundedCornerShape(12.dp)
private val TILE_BUTTON = 36.dp
private val TILE_GLYPH = 18.dp
private val TILE_BUTTON_INSET = 4.dp
private val ADD_BADGE = 32.dp
private const val EDIT_CONTENT_ALPHA = 0.5f
