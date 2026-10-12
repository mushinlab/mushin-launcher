package com.bluecubelabs.env.core.model

enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

sealed interface GestureAction {
    data object None : GestureAction

    data object Notifications : GestureAction

    data object Search : GestureAction

    data object AllApps : GestureAction

    data class LaunchApp(val key: AppKey) : GestureAction
}

data class GestureBinding(
    val direction: SwipeDirection,
    val action: GestureAction,
)

object DefaultGestures {
    val bindings: List<GestureBinding> = listOf(
        GestureBinding(SwipeDirection.UP, GestureAction.Search),
        GestureBinding(SwipeDirection.DOWN, GestureAction.Notifications),
        GestureBinding(SwipeDirection.LEFT, GestureAction.None),
        GestureBinding(SwipeDirection.RIGHT, GestureAction.None),
    )
}

data class LearnedGesture(
    val path: String,
    val key: AppKey,
    val weight: Double,
    val shape: List<ShapeVertex> = emptyList(),
)

data class ShapeVertex(val x: Float, val y: Float, val dwellMs: Long = 0)

fun Map<SwipeDirection, GestureAction>.swipesByApp(): Map<AppKey, List<SwipeDirection>> = SwipeDirection.entries
    .mapNotNull { direction -> (get(direction) as? GestureAction.LaunchApp)?.let { it.key to direction } }
    .groupBy({ it.first }, { it.second })
