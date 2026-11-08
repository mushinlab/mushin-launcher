package com.mushinlab.launcher.core.model

/** Maximum number of small widgets in the top panel. */
enum class WidgetPanelMode(val capacity: Int) { STANDARD(capacity = 2), EXPANDED(capacity = 4) }

/** Arrangement of the standard panel; expanded panels always use two columns. */
enum class WidgetArrangement { ROW, COLUMN }

/** A panel slot: a bound Android widget, or the built-in clock when [id] is [CLOCK_ID]. */
data class HostedWidget(val id: Int, val label: String) {
    val isClock: Boolean get() = id == CLOCK_ID

    companion object {
        /** Android never allocates widget id 0, so it marks the built-in clock. */
        const val CLOCK_ID = 0

        /** The built-in clock slot. */
        val clock = HostedWidget(CLOCK_ID, "")
    }
}

/** Per-space top panel; slots beyond the mode's capacity stay listed in panel management until removed. */
data class WidgetLayout(
    val mode: WidgetPanelMode = WidgetPanelMode.STANDARD,
    val arrangement: WidgetArrangement = WidgetArrangement.ROW,
    val widgets: List<HostedWidget> = listOf(HostedWidget.clock),
)

/** Persisted target of a system binding or configuration activity across process recreation. */
data class PendingWidget(val spaceId: Long, val id: Int)

/** All space layouts and the optional in-progress system widget request. */
data class WidgetState(val layouts: Map<Long, WidgetLayout> = emptyMap(), val pending: PendingWidget? = null)
