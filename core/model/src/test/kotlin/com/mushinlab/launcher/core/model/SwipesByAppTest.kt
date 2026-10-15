package com.mushinlab.launcher.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SwipesByAppTest {
    @Test
    fun appsBoundToSwipesGetTheirDirectionsInOrder() {
        val telegram = AppKey("org.telegram", "Main", 0)
        val maps = AppKey("maps", "Main", 0)
        val bindings = mapOf(
            SwipeDirection.RIGHT to GestureAction.LaunchApp(telegram),
            SwipeDirection.UP to GestureAction.LaunchApp(telegram),
            SwipeDirection.LEFT to GestureAction.LaunchApp(maps),
            SwipeDirection.DOWN to GestureAction.Notifications,
        )
        assertThat(bindings.swipesByApp()).containsExactly(
            telegram,
            listOf(SwipeDirection.UP, SwipeDirection.RIGHT),
            maps,
            listOf(SwipeDirection.LEFT),
        )
    }
}
