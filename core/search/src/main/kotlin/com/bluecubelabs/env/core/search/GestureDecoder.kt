package com.bluecubelabs.env.core.search

import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.GestureTuning
import com.bluecubelabs.env.core.model.LearnedGesture
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sqrt

data class GesturePoint(val x: Float, val y: Float, val repeat: Boolean = false)

data class GestureWord(val latin: String, val text: String, val key: AppKey)

data class GestureResult(val path: String, val words: List<GestureWord>)

class GestureDecoder(
    private val keys: Map<Char, GesturePoint>,
    private val shiftPenalty: Float = GestureTuning.SHIFT_PENALTY,
    private val suggestMargin: Float = GestureTuning.SUGGEST_MARGIN,
) {
    private val pitch: Float = keys.values.toList().let { points ->
        points.flatMap { a -> points.filter { it != a }.map { b -> distance(a, b) } }.minOrNull() ?: 1f
    }
    private val step = pitch / SAMPLES_PER_KEY
    private val idealPaths = HashMap<String, List<GesturePoint>>()

    fun keysAlong(path: List<GesturePoint>): String {
        val out = StringBuilder()
        fun visit(point: GesturePoint, repeat: Boolean) {
            val key = nearest(point) ?: return
            if (repeat || out.isEmpty() || out.last() != key) out.append(key)
        }
        path.firstOrNull()?.let { visit(it, it.repeat) }
        path.zipWithNext { a, b ->
            val count = ceil(distance(a, b) / step).toInt().coerceAtLeast(1)
            for (i in 1..count) {
                val t = i.toFloat() / count
                visit(GesturePoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t), b.repeat && i == count)
            }
        }
        return out.toString()
    }

    fun decode(
        path: List<GesturePoint>,
        vocabulary: List<GestureWord>,
        learned: List<LearnedGesture> = emptyList(),
    ): GestureResult? {
        val drawn = keysAlong(path)
        if (drawn.length < 2 || drawn.all { it == drawn.first() }) return null
        val sampled = resample(path)
        val repeats = path.count { it.repeat }
        val boosts = boosts(drawn, learned)
        val candidates = vocabulary.asSequence()
            .filter { word -> word.latin.length >= 2 && word.latin.all { it in keys } }
            .map { word -> word to score(sampled, repeats, word.latin) - (boosts[word.key] ?: 0f) }
            .groupBy { it.first.key }
            .map { (_, scored) -> scored.minBy { it.second } }
            .sortedBy { it.second }
        val limit = minOf((candidates.firstOrNull()?.second ?: MAX_SCORE) + suggestMargin, MAX_SCORE)
        return GestureResult(drawn, candidates.takeWhile { it.second <= limit }.map { it.first })
    }

    private fun score(sampled: List<GesturePoint>, repeats: Int, word: String): Float {
        val ideal = idealPaths.getOrPut(word) {
            resample(word.mapIndexed { i, c -> keys.getValue(c).copy(repeat = i > 0 && word[i - 1] == c) })
        }
        val doubles = word.zipWithNext().count { (a, b) -> a == b }
        return minOf(fit(sampled, ideal), aligned(sampled, ideal)) +
            DIRECTION_WEIGHT * direction(sampled, ideal) +
            MISSED_DOUBLE_PENALTY * (doubles - repeats).coerceAtLeast(0) +
            EXTRA_DWELL_PENALTY * (repeats - doubles).coerceAtLeast(0)
    }

    private fun fit(drawn: List<GesturePoint>, ideal: List<GesturePoint>): Float {
        val location = drawn.indices.sumOf { distance(drawn[it], ideal[it]).toDouble() }.toFloat() / drawn.size
        val ends = distance(drawn.first(), ideal.first()) + distance(drawn.last(), ideal.last())
        return (location + ENDS_WEIGHT * ends) / pitch
    }

    private fun aligned(drawn: List<GesturePoint>, ideal: List<GesturePoint>): Float {
        val from = centroid(drawn)
        val to = centroid(ideal)
        val drawnRadius = radius(drawn, from)
        val idealRadius = radius(ideal, to)
        val scale = if (drawnRadius == 0f || idealRadius == 0f) {
            1f
        } else {
            (idealRadius / drawnRadius).coerceIn(1 / MAX_SCALE, MAX_SCALE)
        }
        val moved = drawn.map { GesturePoint(to.x + (it.x - from.x) * scale, to.y + (it.y - from.y) * scale) }
        return fit(moved, ideal) + shiftPenalty * distance(from, to) / pitch + SCALE_WEIGHT * abs(ln(scale))
    }

    private fun direction(drawn: List<GesturePoint>, ideal: List<GesturePoint>): Float {
        var turns = 0.0
        var counted = 0
        for (i in 0 until drawn.lastIndex) {
            val ax = drawn[i + 1].x - drawn[i].x
            val ay = drawn[i + 1].y - drawn[i].y
            val bx = ideal[i + 1].x - ideal[i].x
            val by = ideal[i + 1].y - ideal[i].y
            if ((ax == 0f && ay == 0f) || (bx == 0f && by == 0f)) continue
            turns += abs(atan2(ax * by - ay * bx, ax * bx + ay * by)).toDouble()
            counted++
        }
        return if (counted == 0) 0f else (turns / counted / PI).toFloat()
    }

    private fun centroid(points: List<GesturePoint>): GesturePoint =
        GesturePoint(
            points.sumOf { it.x.toDouble() }.toFloat() / points.size,
            points.sumOf { it.y.toDouble() }.toFloat() / points.size,
        )

    private fun radius(points: List<GesturePoint>, center: GesturePoint): Float =
        sqrt(points.sumOf { distance(it, center).toDouble().let { d -> d * d } } / points.size).toFloat()

    private fun boosts(drawn: String, learned: List<LearnedGesture>): Map<AppKey, Float> {
        if (drawn.length > GestureLearning.MAX_PATH) return emptyMap()
        val support = HashMap<AppKey, Double>()
        learned.forEach { sample ->
            val similarity = GestureLearning.similarity(drawn, sample.path)
            if (similarity >= GestureLearning.MIN_SIMILARITY) {
                support.merge(sample.key, sample.weight * similarity, Double::plus)
            }
        }
        return support.mapValues { (_, s) -> (LEARN_BONUS * s / (s + 1)).toFloat() }
    }

    private fun resample(points: List<GesturePoint>): List<GesturePoint> {
        val lengths = FloatArray(points.size)
        for (i in 1 until points.size) lengths[i] = lengths[i - 1] + distance(points[i - 1], points[i])
        val total = lengths.last()
        if (total == 0f) return List(SAMPLES) { points.first() }
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

    private fun nearest(point: GesturePoint): Char? = keys.minByOrNull { (_, center) -> distance(point, center) }?.key

    private fun distance(a: GesturePoint, b: GesturePoint): Float = hypot(a.x - b.x, a.y - b.y)

    private companion object {
        const val SAMPLES = 32
        const val SAMPLES_PER_KEY = 3f
        const val ENDS_WEIGHT = 0.5f
        const val SCALE_WEIGHT = 0.5f
        const val MAX_SCALE = 2f
        const val DIRECTION_WEIGHT = 1f
        const val MISSED_DOUBLE_PENALTY = 0.1f
        const val EXTRA_DWELL_PENALTY = 0.5f
        const val MAX_SCORE = 2f
        const val LEARN_BONUS = 1.5
    }
}
