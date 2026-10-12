package com.bluecubelabs.env.core.search

import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.LearnedGesture
import com.bluecubelabs.env.core.model.ShapeVertex
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShapeMatcherTest {
    private val keys: Map<Char, GesturePoint> = buildMap {
        listOf("qwertyuiop" to 0f, "asdfghjkl" to 0.5f, "zxcvbnm" to 1.5f).forEachIndexed { row, (letters, shift) ->
            letters.forEachIndexed { i, c -> put(c, GesturePoint((i + shift) * KEY, row * KEY)) }
        }
    }
    private val decoder = GestureDecoder(keys)
    private val matcher = ShapeMatcher(keys, 10 * KEY, 3 * KEY)
    private val maps = AppKey("maps", "A", 0)
    private val slack = AppKey("slack", "A", 0)
    private val learned = listOf(
        LearnedGesture(decoder.keysAlong(word("maps")), maps, 1.0),
        LearnedGesture(decoder.keysAlong(word("slack")), slack, 1.0),
    )

    private fun word(text: String) = text.map { keys.getValue(it) }

    private fun drawn(text: String, scale: Float, dx: Float = 0f, dy: Float = 0f): List<GesturePoint> {
        val path = word(text)
        val cx = path.map { it.x }.average().toFloat()
        val cy = path.map { it.y }.average().toFloat()
        return path.map { GesturePoint(cx + (it.x - cx) * scale + dx, cy + (it.y - cy) * scale + dy) }
    }

    @Test
    fun findsTheShapeAnywhereOnTheScreen() {
        assertThat(matcher.match(drawn("maps", 1f, dx = 900f, dy = 1500f), learned)).containsExactly(maps)
    }

    @Test
    fun findsTheShapeDrawnUpToTwiceLargerOrSmaller() {
        assertThat(matcher.match(drawn("maps", 1.9f), learned)).containsExactly(maps)
        assertThat(matcher.match(drawn("maps", 0.55f), learned)).containsExactly(maps)
        assertThat(matcher.match(drawn("slack", 1.5f, dy = 700f), learned)).containsExactly(slack)
    }

    @Test
    fun ignoresShapesDrawnMoreThanTwiceLargerOrSmaller() {
        assertThat(matcher.match(drawn("maps", 2.5f), learned)).isEmpty()
        assertThat(matcher.match(drawn("maps", 0.35f), learned)).isEmpty()
    }

    @Test
    fun ignoresUnknownShapes() {
        assertThat(matcher.match(drawn("qp", 1f), learned)).isEmpty()
        assertThat(matcher.match(drawn("zqm", 1f), learned)).isEmpty()
        assertThat(matcher.match(drawn("maps", 1f), emptyList())).isEmpty()
    }

    @Test
    fun anyFigureStoredAsVectorsIsFound() {
        val ring = (0..12).map { i ->
            val a = i * 2 * Math.PI / 12
            ShapeVertex((0.5 + 0.1 * kotlin.math.cos(a)).toFloat(), (0.5 + 0.3 * kotlin.math.sin(a)).toFloat())
        }
        val circle = LearnedGesture("ghj", slack, 1.0, ring)
        val drawn = ring.map { GesturePoint(it.x * 10 * KEY * 1.4f + 300f, it.y * 3 * KEY * 1.4f + 900f) }
        assertThat(matcher.match(drawn, listOf(circle) + learned)).containsExactly(slack)
    }

    @Test
    fun aPointIsNotAShape() {
        assertThat(matcher.match(listOf(GesturePoint(1f, 1f), GesturePoint(1f, 1f)), learned)).isEmpty()
    }

    private companion object {
        const val KEY = 100f
    }
}
