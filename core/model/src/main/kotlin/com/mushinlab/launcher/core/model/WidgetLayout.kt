package com.mushinlab.launcher.core.model

/** The independent widget areas above and below the home app list. */
enum class WidgetPanelPosition { TOP, BOTTOM }

/** Preferred panel height in dp; empty panels always occupy zero height. */
enum class WidgetPanelSize(val heightDp: Int) { SMALL(heightDp = 120), MEDIUM(heightDp = 200), LARGE(heightDp = 280) }

/** A bound Android widget instance belonging to one panel of one space. */
data class HostedWidget(val id: Int, val panel: WidgetPanelPosition, val label: String)

/** Per-space widget layout; the built-in clock reuses the existing clock appearance preferences. */
data class WidgetLayout(
    val clockPanel: WidgetPanelPosition? = WidgetPanelPosition.TOP,
    val topSize: WidgetPanelSize = WidgetPanelSize.MEDIUM,
    val bottomSize: WidgetPanelSize = WidgetPanelSize.MEDIUM,
    val widgets: List<HostedWidget> = emptyList(),
)

/** Persisted target of a system picker or configuration activity, including across process recreation. */
data class PendingWidget(val spaceId: Long, val panel: WidgetPanelPosition, val id: Int)

/** All space layouts and the optional in-progress system widget request. */
data class WidgetState(val layouts: Map<Long, WidgetLayout> = emptyMap(), val pending: PendingWidget? = null)
