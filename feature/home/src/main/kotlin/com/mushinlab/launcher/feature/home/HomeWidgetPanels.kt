package com.mushinlab.launcher.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mushinlab.launcher.core.model.HostedWidget
import com.mushinlab.launcher.core.model.WidgetCell
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
data class HomeWidgetTile(val id: Int, val label: String, val cell: WidgetCell = WidgetCell(0, 0)) {
    val isClock: Boolean get() = id == HostedWidget.CLOCK_ID
}

@Immutable
data class HomeWidgetPanel(
    val widgets: ImmutableList<HomeWidgetTile> = persistentListOf(HomeWidgetTile(HostedWidget.CLOCK_ID, "")),
) {
    val freeCells: List<WidgetCell>
        get() = WidgetCell.singles.filter { cell -> widgets.none { it.cell.overlaps(cell) } }

    fun visibleRows(editing: Boolean): List<Int> = (0 until WidgetCell.ROWS).filter { row ->
        editing || widgets.any { row in it.cell.row until it.cell.row + it.cell.rowSpan }
    }
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
    val onAdd: (WidgetCell) -> Unit,
    val onPlace: (Int, WidgetCell) -> Unit,
    val onRemove: (Int) -> Unit,
    val onEditClock: () -> Unit,
)

@Immutable
private class PanelGrid(val cellWidth: Dp, val rows: List<Int>) {
    val height: Dp get() = WIDGET_HEIGHT * rows.size + WIDGET_GAP * (rows.size - 1)

    fun offset(cell: WidgetCell): DpSize =
        DpSize((cellWidth + WIDGET_GAP) * cell.column, (WIDGET_HEIGHT + WIDGET_GAP) * rows.indexOf(cell.row))

    fun size(cell: WidgetCell): DpSize = DpSize(
        cellWidth * cell.columnSpan + WIDGET_GAP * (cell.columnSpan - 1),
        WIDGET_HEIGHT * cell.rowSpan + WIDGET_GAP * (cell.rowSpan - 1),
    )
}

@Composable
internal fun HomeWidgetPanel(
    panel: HomeWidgetPanel,
    maxWidth: Dp,
    editing: Boolean,
    registry: RowRegistry,
    edits: PanelEdits,
    slots: PanelSlots,
) {
    val rows = panel.visibleRows(editing)
    if (rows.isEmpty()) return
    val grid = PanelGrid((maxWidth - HomeDimens.rowPadding * 2 - WIDGET_GAP) / WidgetCell.COLUMNS, rows)
    var preview by remember(editing) { mutableStateOf<WidgetCell?>(null) }
    Box(
        Modifier.fillMaxWidth().padding(horizontal = HomeDimens.rowPadding, vertical = WIDGET_GAP)
            .height(grid.height)
            .testTag("widget_panel_TOP"),
    ) {
        if (editing) panel.freeCells.forEach { key(it) { AddWidgetCell(it, grid, registry, edits.onAdd) } }
        panel.widgets.forEach { tile ->
            key(tile.id) { WidgetTile(tile, grid, editing, registry, edits, slots) { preview = it } }
        }
        preview?.let { PreviewFrame(it, grid) }
    }
}

@Composable
private fun WidgetTile(
    tile: HomeWidgetTile,
    grid: PanelGrid,
    editing: Boolean,
    registry: RowRegistry,
    edits: PanelEdits,
    slots: PanelSlots,
    onPreview: (WidgetCell?) -> Unit,
) {
    val key = "widget_${tile.id}"
    var drag by remember(tile.cell, editing) { mutableStateOf<Offset?>(null) }
    val blocks = editing || !tile.isClock
    val origin = grid.offset(tile.cell)
    val size = grid.size(tile.cell)
    DisposableEffect(key, blocks) { onDispose { registry.block(key, null) } }
    Box(
        Modifier.offset(origin.width, origin.height).size(size)
            .zIndex(if (drag != null) 1f else 0f)
            .offset { drag?.let { IntOffset(it.x.roundToInt(), it.y.roundToInt()) } ?: IntOffset.Zero }
            .onGloballyPositioned { if (blocks) registry.block(key, it.boundsInRoot()) }
            .testTag(key),
    ) {
        Box(Modifier.fillMaxSize().clipToBounds().alpha(if (editing) EDIT_CONTENT_ALPHA else 1f)) {
            if (tile.isClock) slots.clock(editing) else slots.widgets(tile.id, size.height, size.width)
        }
        if (editing) {
            val step = with(LocalDensity.current) {
                Offset((grid.cellWidth + WIDGET_GAP).toPx(), (WIDGET_HEIGHT + WIDGET_GAP).toPx())
            }
            val place: (WidgetCell) -> Unit = { if (it != tile.cell) edits.onPlace(tile.id, it) }
            Box(
                Modifier.fillMaxSize()
                    .border(1.dp, LocalMushinColors.current.accent, TILE_SHAPE)
                    .widgetMoveActions(tile.cell, place)
                    .dragToMove(tile.cell, step, { drag = it }, onPreview, place),
            )
            WidgetEdge.entries.filter { tile.cell.canResize(it) }.forEach { edge ->
                ResizeHandle(tile, edge, step, onPreview, place)
            }
            if (tile.isClock) EditClockButton(tile, edits, Modifier.align(Alignment.Center))
            RemoveTileButton(tile, edits, Modifier.align(Alignment.TopEnd))
        }
    }
}

private fun Modifier.dragToMove(
    cell: WidgetCell,
    step: Offset,
    onOffset: (Offset?) -> Unit,
    onPreview: (WidgetCell?) -> Unit,
    onDrop: (WidgetCell) -> Unit,
): Modifier = pointerInput(cell, step) {
    var delta = Offset.Zero
    fun target() = cell.moved((delta.x / step.x).roundToInt(), (delta.y / step.y).roundToInt())
    fun reset() {
        delta = Offset.Zero
        onOffset(null)
        onPreview(null)
    }
    detectDragGesturesAfterLongPress(
        onDragStart = {
            delta = Offset.Zero
            onOffset(delta)
            onPreview(cell)
        },
        onDragCancel = ::reset,
        onDragEnd = {
            val target = target()
            reset()
            onDrop(target)
        },
        onDrag = { change, amount ->
            change.consume()
            delta += amount
            onOffset(delta)
            onPreview(target())
        },
    )
}

@Composable
private fun BoxScope.ResizeHandle(
    tile: HomeWidgetTile,
    edge: WidgetEdge,
    step: Offset,
    onPreview: (WidgetCell?) -> Unit,
    onResize: (WidgetCell) -> Unit,
) {
    fun along(across: Dp, length: Dp) = if (edge.horizontal) DpSize(across, length) else DpSize(length, across)
    val touch = along(HANDLE_TOUCH, HANDLE_LENGTH + HANDLE_BACKING)
    val backing = along(HANDLE_BACKING, HANDLE_LENGTH + HANDLE_BACKING)
    val bar = along(HANDLE_BAR, HANDLE_LENGTH)
    Box(
        Modifier.align(edge.alignment).size(touch)
            .pointerInput(tile.cell, edge, step) {
                var total = 0f
                var target = tile.cell
                detectDragGestures(
                    onDragStart = {
                        total = 0f
                        target = tile.cell
                        onPreview(target)
                    },
                    onDragCancel = { onPreview(null) },
                    onDragEnd = {
                        onPreview(null)
                        onResize(target)
                    },
                ) { change, amount ->
                    change.consume()
                    total += if (edge.horizontal) amount.x else amount.y
                    target = tile.cell.resized(edge, (total / if (edge.horizontal) step.x else step.y).roundToInt())
                    onPreview(target)
                }
            }
            .testTag("tile_resize_${edge.name.lowercase()}_${tile.id}"),
        contentAlignment = edge.alignment,
    ) {
        val colors = LocalMushinColors.current
        Box(
            Modifier.size(backing).background(colors.keyBackground, CircleShape)
                .border(PopupStyle.outlineWidth, PopupStyle.outline(colors), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(bar).background(colors.accent, CircleShape))
        }
    }
}

@Composable
private fun PreviewFrame(cell: WidgetCell, grid: PanelGrid) {
    val origin = grid.offset(cell)
    val accent = LocalMushinColors.current.accent
    Box(
        Modifier.offset(origin.width, origin.height).size(grid.size(cell)).zIndex(2f)
            .background(accent.copy(alpha = PREVIEW_ALPHA), TILE_SHAPE)
            .border(2.dp, accent, TILE_SHAPE)
            .testTag("widget_preview"),
    )
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
private fun Modifier.widgetMoveActions(cell: WidgetCell, onMove: (WidgetCell) -> Unit): Modifier {
    val actions = listOf(
        stringResource(R.string.widget_move_before) to cell.stepped(-1),
        stringResource(R.string.widget_move_after) to cell.stepped(1),
        stringResource(R.string.widget_grow) to cell.grown(),
        stringResource(R.string.widget_shrink) to cell.shrunk(),
    )
    return semantics {
        customActions = actions.mapNotNull { (label, target) ->
            target?.let { CustomAccessibilityAction(label) { onMove(it).let { true } } }
        }
    }
}

@Composable
private fun AddWidgetCell(cell: WidgetCell, grid: PanelGrid, registry: RowRegistry, onAdd: (WidgetCell) -> Unit) {
    val key = "add_widget_${cell.column}_${cell.row}"
    val colors = LocalMushinColors.current
    val origin = grid.offset(cell)
    DisposableEffect(key) { onDispose { registry.block(key, null) } }
    Column(
        Modifier.offset(origin.width, origin.height).size(grid.size(cell))
            .clip(TILE_SHAPE)
            .border(GlyphWrapper.outline, PopupStyle.outline(colors), TILE_SHAPE)
            .onGloballyPositioned { registry.block(key, it.boundsInRoot()) }
            .pressable(onClick = { onAdd(cell) })
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
private val HANDLE_TOUCH = 24.dp
private val HANDLE_LENGTH = 32.dp
private val HANDLE_BAR = 4.dp
private val HANDLE_BACKING = 12.dp
private const val EDIT_CONTENT_ALPHA = 0.5f
private const val PREVIEW_ALPHA = 0.15f
