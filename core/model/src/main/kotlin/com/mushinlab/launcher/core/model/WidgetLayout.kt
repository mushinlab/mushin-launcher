package com.mushinlab.launcher.core.model

/** Rectangle of the top widget grid, in cells from the top-left corner. */
data class WidgetCell(val column: Int, val row: Int, val columnSpan: Int = 1, val rowSpan: Int = 1) {
    val fits: Boolean
        get() = column >= 0 && row >= 0 && columnSpan >= 1 && rowSpan >= 1 &&
            column + columnSpan <= COLUMNS && row + rowSpan <= ROWS

    /** Whether this rectangle shares at least one cell with [other]. */
    fun overlaps(other: WidgetCell): Boolean = column < other.column + other.columnSpan &&
        other.column < column + columnSpan && row < other.row + other.rowSpan && other.row < row + rowSpan

    /** The same span moved to [column] and [row]. */
    fun at(column: Int, row: Int): WidgetCell = copy(column = column, row = row)

    companion object {
        const val COLUMNS = 8
        const val ROWS = 8
        const val QUARTER = COLUMNS / 2

        /** All single cells, row by row. */
        val singles: List<WidgetCell> = List(COLUMNS * ROWS) { WidgetCell(it % COLUMNS, it / COLUMNS) }

        /** The four quarter-panel rectangles, row by row. */
        val quarters: List<WidgetCell> = List(2 * 2) {
            WidgetCell((it % 2) * QUARTER, (it / 2) * QUARTER, QUARTER, QUARTER)
        }
    }
}

/** A panel slot: a bound Android widget, or the built-in clock when [id] is [CLOCK_ID]. */
data class HostedWidget(val id: Int, val label: String, val cell: WidgetCell = WidgetCell(0, 0)) {
    val isClock: Boolean get() = id == CLOCK_ID

    companion object {
        /** Android never allocates widget id 0, so it marks the built-in clock. */
        const val CLOCK_ID = 0

        /** The built-in clock slot in the top-left quarter. */
        val clock = HostedWidget(CLOCK_ID, "", WidgetCell(0, 0, WidgetCell.QUARTER, WidgetCell.QUARTER))
    }
}

/** Per-space top panel: an 8×8 grid where every widget covers a rectangle of cells without overlaps. */
data class WidgetLayout(val widgets: List<HostedWidget> = listOf(HostedWidget.clock)) {
    /** Single cells not covered by any widget, row by row. */
    val freeCells: List<WidgetCell>
        get() = WidgetCell.singles.filter { cell -> widgets.none { it.cell.overlaps(cell) } }

    /**
     * Adds [widget] at [preferred] when that cell is free, otherwise at the first free cell; null when full.
     * A widget that needs [columnSpan] by [rowSpan] cells takes a free rectangle of that size, first around its
     * cell, or pushes the widgets it covers to free space. Without room it keeps its width in one row, then its
     * height in one column, then one cell.
     */
    fun add(
        widget: HostedWidget,
        preferred: WidgetCell? = null,
        columnSpan: Int = 1,
        rowSpan: Int = 1,
    ): WidgetLayout? {
        if (widgets.size >= MAX_WIDGETS || widgets.any { it.id == widget.id }) return null
        val free = freeCells
        val cell = preferred?.takeIf { it in free } ?: free.firstOrNull() ?: return null
        val added = copy(widgets = widgets + widget.copy(cell = cell))
        return listOf(WidgetCell(0, 0, columnSpan, rowSpan), WidgetCell(0, 0, columnSpan), WidgetCell(0, 0, 1, rowSpan))
            .firstNotNullOfOrNull { added.spread(widget.id, cell, it) } ?: added
    }

    /**
     * The largest free rectangle for a new widget: at least [columnSpan] by [rowSpan] cells and at most a quarter
     * of the panel, the topmost then leftmost among equals; null when no such rectangle is free.
     */
    fun roomFor(columnSpan: Int = 1, rowSpan: Int = 1): WidgetCell? {
        val columns = columnSpan.coerceAtMost(WidgetCell.QUARTER)..WidgetCell.QUARTER
        val rows = rowSpan.coerceAtMost(WidgetCell.QUARTER)..WidgetCell.QUARTER
        return WidgetCell.singles
            .flatMap { origin ->
                columns.flatMap { c -> rows.map { r -> WidgetCell(origin.column, origin.row, c, r) } }
            }
            .filter { candidate -> candidate.fits && widgets.none { it.cell.overlaps(candidate) } }
            .maxWithOrNull(
                compareBy<WidgetCell> { it.columnSpan * it.rowSpan }.thenByDescending {
                    it.row * WidgetCell.COLUMNS +
                        it.column
                },
            )
    }

    private fun spread(id: Int, cell: WidgetCell, span: WidgetCell): WidgetLayout? {
        val around = span.at(
            minOf(cell.column, WidgetCell.COLUMNS - span.columnSpan),
            minOf(cell.row, WidgetCell.ROWS - span.rowSpan),
        )
        val others = widgets.filter { it.id != id }.map { it.cell }
        return place(id, spot(span, listOf(around) + WidgetCell.singles, others) ?: around)
    }

    /**
     * Moves or resizes widget [id] to [target]. Widgets it covers move to free space, first to where the moved
     * widget was, keeping their span when possible and shrinking to one cell otherwise. Returns null when [target]
     * is outside the grid or there is no room.
     */
    fun place(id: Int, target: WidgetCell): WidgetLayout? {
        val moved = widgets.firstOrNull { it.id == id } ?: return null
        if (!target.fits) return null
        val stay = widgets.filter { it.id != id && !it.cell.overlaps(target) }
        val displaced = widgets.filter { it.id != id && it.cell.overlaps(target) }
        val placed = stay.map { it.cell } + target
        val origins = listOf(moved.cell) + WidgetCell.singles
        val relocated = mutableListOf<HostedWidget>()
        for (widget in displaced) {
            val taken = placed + relocated.map { it.cell }
            val cell = spot(widget.cell, origins, taken) ?: spot(WidgetCell(0, 0), origins, taken) ?: return null
            relocated += widget.copy(cell = cell)
        }
        val result = (stay + relocated).associateBy { it.id } + (id to moved.copy(cell = target))
        return copy(widgets = widgets.map { result.getValue(it.id) })
    }

    private fun spot(span: WidgetCell, origins: List<WidgetCell>, taken: List<WidgetCell>): WidgetCell? = origins
        .map { span.at(it.column, it.row) }
        .firstOrNull { candidate -> candidate.fits && taken.none { it.overlaps(candidate) } }

    companion object {
        /** Widgets (the clock included) a panel holds at most. */
        const val MAX_WIDGETS = 4
    }
}

/** Persisted target of a system binding or configuration activity across process recreation. */
data class PendingWidget(val spaceId: Long, val id: Int, val cell: WidgetCell? = null)

/** All space layouts and the optional in-progress system widget request. */
data class WidgetState(val layouts: Map<Long, WidgetLayout> = emptyMap(), val pending: PendingWidget? = null)
