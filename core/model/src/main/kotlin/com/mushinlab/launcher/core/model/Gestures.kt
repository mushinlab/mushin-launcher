package com.mushinlab.launcher.core.model

enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

/** What a home-screen swipe does; [None] disables the direction. */
sealed interface GestureAction {
    data object None : GestureAction

    data object Notifications : GestureAction

    data object Search : GestureAction

    data object AllApps : GestureAction

    data class LaunchApp(val key: AppKey) : GestureAction
}

/** Assignment of a [GestureAction] to a home-screen swipe direction. */
data class GestureBinding(
    val direction: SwipeDirection,
    val action: GestureAction,
)

/** Bindings used until the user changes them: up opens search, down opens notifications. */
object DefaultGestures {
    val bindings: List<GestureBinding> = listOf(
        GestureBinding(SwipeDirection.UP, GestureAction.Search),
        GestureBinding(SwipeDirection.DOWN, GestureAction.Notifications),
        GestureBinding(SwipeDirection.LEFT, GestureAction.None),
        GestureBinding(SwipeDirection.RIGHT, GestureAction.None),
    )
}

/**
 * A keyboard drawing the user has confirmed for an app.
 *
 * [path] is the sequence of keys the drawing passed over. [weight] grows with each confirmation and shrinks
 * when a similar drawing is confirmed for another app. [shape] is the simplified outline in fractions (0..1)
 * of the keyboard letter area; it is empty for samples learned before outlines were stored.
 */
data class LearnedGesture(
    val path: String,
    val key: AppKey,
    val weight: Double,
    val shape: List<ShapeVertex> = emptyList(),
)

/**
 * A vertex of a simplified drawing. Coordinates are in the caller's space: pixels while drawing, fractions
 * of the keyboard area inside [LearnedGesture]. [dwellMs] is how long the finger stayed near the vertex.
 */
data class ShapeVertex(val x: Float, val y: Float, val dwellMs: Long = 0)

/** Inverts swipe bindings: for every app bound to a swipe, the directions that launch it. */
fun Map<SwipeDirection, GestureAction>.swipesByApp(): Map<AppKey, List<SwipeDirection>> = SwipeDirection.entries
    .mapNotNull { direction -> (get(direction) as? GestureAction.LaunchApp)?.let { it.key to direction } }
    .groupBy({ it.first }, { it.second })
