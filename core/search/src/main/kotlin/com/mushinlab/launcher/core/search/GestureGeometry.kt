package com.mushinlab.launcher.core.search

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

internal object GestureGeometry {
    fun direction(drawn: List<GesturePoint>, ideal: List<GesturePoint>): Float {
        var turns = 0.0
        var counted = 0
        for (i in 0 until drawn.lastIndex) {
            val ax = drawn[i + 1].x - drawn[i].x
            val ay = drawn[i + 1].y - drawn[i].y
            val bx = ideal[i + 1].x - ideal[i].x
            val by = ideal[i + 1].y - ideal[i].y
            if (isStill(ax, ay) || isStill(bx, by)) continue
            turns += abs(atan2(ax * by - ay * bx, ax * bx + ay * by)).toDouble()
            counted++
        }
        return if (counted == 0) 0f else (turns / counted / PI).toFloat()
    }

    fun centroid(points: List<GesturePoint>): GesturePoint =
        GesturePoint(
            points.sumOf { it.x.toDouble() }.toFloat() / points.size,
            points.sumOf { it.y.toDouble() }.toFloat() / points.size,
        )

    fun radius(points: List<GesturePoint>, center: GesturePoint): Float =
        sqrt(points.sumOf { Shapes.distance(it, center).toDouble().let { d -> d * d } } / points.size).toFloat()

    fun resample(points: List<GesturePoint>, samples: Int): List<GesturePoint> {
        val lengths = FloatArray(points.size)
        for (i in 1 until points.size) lengths[i] = lengths[i - 1] + Shapes.distance(points[i - 1], points[i])
        val total = lengths.last()
        if (total == 0f) return List(samples) { points.first() }
        var segment = 1
        return List(samples) { i ->
            val target = total * i / (samples - 1)
            while (segment < points.lastIndex && lengths[segment] < target) segment++
            val a = points[segment - 1]
            val b = points[segment]
            val span = lengths[segment] - lengths[segment - 1]
            val t = if (span == 0f) 0f else ((target - lengths[segment - 1]) / span).coerceIn(0f, 1f)
            GesturePoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        }
    }

    private fun isStill(dx: Float, dy: Float): Boolean = dx == 0f && dy == 0f
}
