package com.mushinlab.launcher.core.search

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShapesTest {
    private fun line(from: Pair<Float, Float>, to: Pair<Float, Float>, startMs: Long, steps: Int = 10) =
        (0..steps).map { i ->
            val t = i.toFloat() / steps
            TimedPoint(
                from.first + (to.first - from.first) * t,
                from.second + (to.second - from.second) * t,
                startMs + i * 10L,
            )
        }

    @Test
    fun vectorsKeepCornersAndRecordHowLongTheFingerStayed() {
        val down = line(0f to 0f, 0f to 100f, 0)
        val hold = List(20) { TimedPoint(0f, 100f + (it % 2), 110L + it * 50L) }
        val right = line(0f to 100f, 100f to 100f, 1110)
        val vertices = Shapes.vectorize(down + hold + right, tolerance = 5f, dwellRadius = 4f)
        assertThat(vertices).hasSize(3)
        assertThat(vertices[1].x).isWithin(1f).of(0f)
        assertThat(vertices[1].y).isWithin(1f).of(100f)
        assertThat(vertices.last().x).isEqualTo(100f)
        assertThat(vertices[1].dwellMs).isAtLeast(900L)
        assertThat(vertices[0].dwellMs).isLessThan(50L)
    }

    @Test
    fun averageFollowsTheRepeatedPatternAndIgnoresAnOutlier() {
        val v = listOf(GesturePoint(0f, 0f), GesturePoint(50f, 100f), GesturePoint(100f, 0f))
        val drawings = listOf(
            v,
            v.map { GesturePoint(it.x * 2 + 500, it.y * 2 + 30) },
            v.map { GesturePoint(it.x + 3, it.y - 4) },
            listOf(GesturePoint(0f, 0f), GesturePoint(100f, 0f), GesturePoint(100f, 100f), GesturePoint(0f, 100f)),
        )
        val average = Shapes.average(drawings)
        val reference = Shapes.normalize(v)
        assertThat(average).isNotNull()
        assertThat(Shapes.normalize(average!!)!!.distanceTo(reference!!)).isLessThan(0.1f)
    }

    @Test
    fun nothingToAverageGivesNull() {
        assertThat(Shapes.average(emptyList())).isNull()
        assertThat(Shapes.average(listOf(listOf(GesturePoint(1f, 1f))))).isNull()
    }
}
