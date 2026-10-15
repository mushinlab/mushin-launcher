package com.mushinlab.launcher.core.search

import com.mushinlab.launcher.core.model.ShapeVertex
import kotlin.math.hypot
import kotlin.math.sqrt

/** A raw touch sample: position in pixels and event time in milliseconds. */
data class TimedPoint(val x: Float, val y: Float, val timeMs: Long)

/** An outline resampled to [Shapes.SAMPLES] points, centered on its centroid and divided by its RMS [radius]. */
class NormalizedShape(val points: List<GesturePoint>, val radius: Float) {
    /** Mean distance between corresponding points, in units of radius. */
    fun distanceTo(other: NormalizedShape): Float =
        points.indices.sumOf { i -> Shapes.distance(points[i], other.points[i]).toDouble() }.toFloat() / points.size
}

/** Geometry of drawings: simplification into vertices, normalization and averaging. */
object Shapes {
    const val SAMPLES = 32

    /**
     * Simplifies a touch trace into vertices (Ramer-Douglas-Peucker with [tolerance] px) and records at each vertex
     * how long the finger stayed within [dwellRadius] px of it.
     */
    fun vectorize(points: List<TimedPoint>, tolerance: Float, dwellRadius: Float): List<ShapeVertex> {
        val distinct = points.filterIndexed { i, p -> i == 0 || p.x != points[i - 1].x || p.y != points[i - 1].y }
        if (distinct.isEmpty()) return emptyList()
        val kept = sortedSetOf(0, distinct.lastIndex)
        simplify(distinct, 0, distinct.lastIndex, tolerance, kept)
        return kept.map { i -> ShapeVertex(distinct[i].x, distinct[i].y, dwell(points, distinct[i], dwellRadius)) }
    }

    /** Normalized outline of [path], or null when it has fewer than two distinct points or zero size. */
    fun normalize(path: List<GesturePoint>): NormalizedShape? {
        val distinct = path.filterIndexed { i, p -> i == 0 || p.x != path[i - 1].x || p.y != path[i - 1].y }
        return if (distinct.size < 2) null else centered(resample(distinct))
    }

    private fun centered(sampled: List<GesturePoint>): NormalizedShape? {
        val cx = sampled.sumOf { it.x.toDouble() }.toFloat() / sampled.size
        val cy = sampled.sumOf { it.y.toDouble() }.toFloat() / sampled.size
        val radius = sqrt(
            sampled.sumOf { p -> ((p.x - cx) * (p.x - cx) + (p.y - cy) * (p.y - cy)).toDouble() } / sampled.size,
        ).toFloat()
        return if (radius == 0f) {
            null
        } else {
            NormalizedShape(sampled.map { GesturePoint((it.x - cx) / radius, (it.y - cy) / radius) }, radius)
        }
    }

    /**
     * Mean normalized outline of the [drawings] within [threshold] of their medoid, so outliers are ignored;
     * null when no drawing can be normalized.
     */
    fun average(drawings: List<List<GesturePoint>>, threshold: Float = ShapeMatcher.THRESHOLD): List<GesturePoint>? {
        val shapes = drawings.mapNotNull(::normalize)
        if (shapes.isEmpty()) return null
        val medoid = shapes.minBy { a -> shapes.sumOf { b -> a.distanceTo(b).toDouble() } }
        val cluster = shapes.filter { it.distanceTo(medoid) <= threshold }
        return List(SAMPLES) { i ->
            GesturePoint(
                cluster.sumOf { it.points[i].x.toDouble() }.toFloat() / cluster.size,
                cluster.sumOf { it.points[i].y.toDouble() }.toFloat() / cluster.size,
            )
        }
    }

    fun distance(a: GesturePoint, b: GesturePoint): Float = hypot(a.x - b.x, a.y - b.y)

    private fun simplify(points: List<TimedPoint>, from: Int, to: Int, tolerance: Float, kept: MutableSet<Int>) {
        if (to - from < 2) return
        var farthest = from
        var max = 0f
        for (i in from + 1 until to) {
            val d = toSegment(points[i], points[from], points[to])
            if (d > max) {
                max = d
                farthest = i
            }
        }
        if (max <= tolerance) return
        kept += farthest
        simplify(points, from, farthest, tolerance, kept)
        simplify(points, farthest, to, tolerance, kept)
    }

    private fun dwell(points: List<TimedPoint>, at: TimedPoint, radius: Float): Long {
        val index = points.indexOf(at)
        var start = index
        var end = index
        while (start > 0 && hypot(points[start - 1].x - at.x, points[start - 1].y - at.y) <= radius) start--
        while (end < points.lastIndex && hypot(points[end + 1].x - at.x, points[end + 1].y - at.y) <= radius) end++
        return points[end].timeMs - points[start].timeMs
    }

    private fun toSegment(p: TimedPoint, a: TimedPoint, b: TimedPoint): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0f) return hypot(p.x - a.x, p.y - a.y)
        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lengthSquared).coerceIn(0f, 1f)
        return hypot(p.x - (a.x + dx * t), p.y - (a.y + dy * t))
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
