package com.mushinlab.launcher.feature.home

import androidx.compose.ui.Alignment
import com.mushinlab.launcher.core.model.WidgetCell

internal enum class WidgetEdge(val alignment: Alignment, val horizontal: Boolean) {
    START(Alignment.CenterStart, true),
    END(Alignment.CenterEnd, true),
    TOP(Alignment.TopCenter, false),
    BOTTOM(Alignment.BottomCenter, false),
}

internal fun WidgetCell.moved(columns: Int, rows: Int): WidgetCell = at(
    (column + columns).coerceIn(0, WidgetCell.COLUMNS - columnSpan),
    (row + rows).coerceIn(0, WidgetCell.ROWS - rowSpan),
)

internal fun WidgetCell.resized(edge: WidgetEdge, steps: Int): WidgetCell = when (edge) {
    WidgetEdge.END -> copy(columnSpan = (columnSpan + steps).coerceIn(1, WidgetCell.COLUMNS - column))

    WidgetEdge.BOTTOM -> copy(rowSpan = (rowSpan + steps).coerceIn(1, WidgetCell.ROWS - row))

    WidgetEdge.START -> {
        val start = (column + steps).coerceIn(0, column + columnSpan - 1)
        copy(column = start, columnSpan = column + columnSpan - start)
    }

    WidgetEdge.TOP -> {
        val top = (row + steps).coerceIn(0, row + rowSpan - 1)
        copy(row = top, rowSpan = row + rowSpan - top)
    }
}

internal fun WidgetCell.canResize(edge: WidgetEdge): Boolean = resized(edge, -1) != this || resized(edge, 1) != this

internal fun WidgetCell.grown(): WidgetCell? =
    listOf(WidgetEdge.END to 1, WidgetEdge.BOTTOM to 1, WidgetEdge.START to -1, WidgetEdge.TOP to -1)
        .map { (edge, steps) -> resized(edge, steps) }
        .firstOrNull { it != this }

internal fun WidgetCell.shrunk(): WidgetCell? = listOf(WidgetEdge.END, WidgetEdge.BOTTOM)
    .map { resized(it, -1) }
    .firstOrNull { it != this }

internal fun WidgetCell.stepped(direction: Int): WidgetCell? {
    val start = row * WidgetCell.COLUMNS + column
    return generateSequence(start + direction) { it + direction }
        .takeWhile { it in WidgetCell.singles.indices }
        .map { at(it % WidgetCell.COLUMNS, it / WidgetCell.COLUMNS) }
        .firstOrNull { it.fits }
}
