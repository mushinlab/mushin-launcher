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
        const val COLUMNS = 2
        const val ROWS = 2

        /** All single cells, row by row. */
        val singles: List<WidgetCell> = List(COLUMNS * ROWS) { WidgetCell(it % COLUMNS, it / COLUMNS) }
    }
}

/** A panel slot: a bound Android widget, or the built-in clock when [id] is [CLOCK_ID]. */
data class HostedWidget(val id: Int, val label: String, val cell: WidgetCell = WidgetCell(0, 0)) {
    val isClock: Boolean get() = id == CLOCK_ID

    companion object {
        /** Android never allocates widget id 0, so it marks the built-in clock. */
        const val CLOCK_ID = 0

        /** The built-in clock slot in the top-left cell. */
        val clock = HostedWidget(CLOCK_ID, "")
    }
}

/** Per-space top panel: a 2×2 grid where every widget covers a rectangle of cells without overlaps. */
data class WidgetLayout(val widgets: List<HostedWidget> = listOf(HostedWidget.clock)) {
    /** Single cells not covered by any widget, row by row. */
    val freeCells: List<WidgetCell>
        get() = WidgetCell.singles.filter { cell -> widgets.none { it.cell.overlaps(cell) } }

    /** Adds [widget] at [preferred] when that cell is free, otherwise at the first free cell; null when full. */
    fun add(widget: HostedWidget, preferred: WidgetCell? = null): WidgetLayout? {
        if (widgets.any { it.id == widget.id }) return null
        val free = freeCells
        val cell = preferred?.takeIf { it in free } ?: free.firstOrNull() ?: return null
        return copy(widgets = widgets + widget.copy(cell = cell))
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
}

/** Persisted target of a system binding or configuration activity across process recreation. */
data class PendingWidget(val spaceId: Long, val id: Int, val cell: WidgetCell? = null)

/** All space layouts and the optional in-progress system widget request. */
data class WidgetState(val layouts: Map<Long, WidgetLayout> = emptyMap(), val pending: PendingWidget? = null)
