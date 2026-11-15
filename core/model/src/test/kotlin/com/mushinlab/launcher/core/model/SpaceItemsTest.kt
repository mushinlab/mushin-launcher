package com.mushinlab.launcher.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpaceItemsTest {
    private val a = AppKey("a", "A", 0)
    private val b = AppKey("b", "B", 0)
    private val c = AppKey("c", "C", 0)

    @Test
    fun movingTakesThePlaceOfTheTargetInBothDirections() {
        assertThat(listOf(a, b, c).moving(a, c)).containsExactly(b, c, a).inOrder()
        assertThat(listOf(b, c, a).moving(a, b)).containsExactly(a, b, c).inOrder()
        assertThat(listOf(a, b).moving(a, c)).containsExactly(a, b).inOrder()
        assertThat(listOf(a, b).moving(c, a)).containsExactly(a, b).inOrder()
    }
}
