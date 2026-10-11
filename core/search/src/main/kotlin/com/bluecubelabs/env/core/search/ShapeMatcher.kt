package com.bluecubelabs.env.core.search

import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.LearnedGesture
import kotlin.math.hypot
import kotlin.math.sqrt

class ShapeMatcher(
    private val keys: Map<Char, GesturePoint>,
    private val maxScale: Float = MAX_SCALE,
    private val threshold: Float = THRESHOLD,
) {
    fun match(drawn: List<GesturePoint>, learned: List<LearnedGesture>): List<AppKey> {
        val shape = Shape.of(drawn) ?: return emptyList()
        val best = HashMap<AppKey, Float>()
        learned.forEach { sample ->
            val ideal = Shape.of(sample.path.mapNotNull { keys[it] }) ?: return@forEach
            val scale = shape.radius / ideal.radius
            if (scale > maxScale * SLACK || scale * maxScale * SLACK < 1) return@forEach
            val distance = shape.distanceTo(ideal)
            if (distance <= threshold) best.merge(sample.key, distance, ::minOf)
        }
        val top = best.values.minOrNull() ?: return emptyList()
        return best.entries.filter { it.value <= top + MARGIN }.sortedBy { it.value }.map { it.key }
    }

    private class Shape(val points: List<GesturePoint>, val radius: Float) {
        fun distanceTo(other: Shape): Float =
            points.indices.sumOf { i -> distance(points[i], other.points[i]).toDouble() }.toFloat() / points.size

        companion object {
            fun of(path: List<GesturePoint>): Shape? {
                val distinct = path.filterIndexed { i, p -> i == 0 || p.x != path[i - 1].x || p.y != path[i - 1].y }
                if (distinct.size < 2) return null
                val sampled = resample(distinct)
                val cx = sampled.sumOf { it.x.toDouble() }.toFloat() / sampled.size
                val cy = sampled.sumOf { it.y.toDouble() }.toFloat() / sampled.size
                val radius = sqrt(
                    sampled.sumOf { p -> ((p.x - cx) * (p.x - cx) + (p.y - cy) * (p.y - cy)).toDouble() } /
                        sampled.size,
                ).toFloat()
                if (radius == 0f) return null
                return Shape(sampled.map { GesturePoint((it.x - cx) / radius, (it.y - cy) / radius) }, radius)
            }

            private fun resample(points: List<GesturePoint>): List<GesturePoint> {
                val lengths = FloatArray(points.size)
                for (i in 1 until points.size) lengths[i] = lengths[i - 1] + distance(points[i - 1], points[i])
                val total = lengths.last()
                var segment = 1
                return List(SAMPLES) { i ->
                    val target = total * i / (SAMPLES - 1)
                    while (segment < points.lastIndex && lengths[segment] < target) segment++
                    val a = points[segment - 1]
                    val b = points[segment]
                    val span = lengths[segment] - lengths[segment - 1]
                    val t = if (span == 0f) 0f else ((target - lengths[segment - 1]) / span).coerceIn(0f, 1f)
                    GesturePoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
                }
            }
        }
    }

    companion object {
        const val MAX_SCALE = 2f
        const val THRESHOLD = 0.35f
        private const val SLACK = 1.15f
        private const val MARGIN = 0.05f
        private const val SAMPLES = 32

        private fun distance(a: GesturePoint, b: GesturePoint): Float = hypot(a.x - b.x, a.y - b.y)
    }
}
