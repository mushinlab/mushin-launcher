package com.mushinlab.launcher.core.search

import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.GestureTuning
import com.mushinlab.launcher.core.model.LearnedGesture
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.ln

/**
 * A point of a drawing or a key center, in keyboard pixels. [repeat] marks a pause on a key, which stands for
 * a double letter.
 */
data class GesturePoint(val x: Float, val y: Float, val repeat: Boolean = false)

/** A drawable name: [latin] is its a-z key sequence, [text] the name as shown, [key] the app it opens. */
data class GestureWord(val latin: String, val text: String, val key: AppKey)

/** Decoding result: [path] is the keys the drawing passed over, [words] the candidates, best first. */
data class GestureResult(val path: String, val words: List<GestureWord>)

/**
 * Decodes a drawing on the keyboard into app names (gesture typing). [keys] maps each letter to its key center.
 *
 * Every word becomes an ideal polyline through its keys; the drawing and the polyline are resampled to the same
 * number of points and compared by position, by a fit tolerant to offset and scale (offset costs [shiftPenalty]
 * per key pitch) and by direction of travel. Scores are roughly in key pitches; lower is better.
 * Not thread-safe: ideal polylines are cached in a plain map.
 */
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

    /**
     * Letters of the keys the drawing passes over, sampled several times per key pitch. Consecutive duplicates
     * collapse unless a point is marked [GesturePoint.repeat].
     */
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

    /**
     * Candidate words for a drawing, at most one per app, best first.
     *
     * Words within [suggestMargin] of the best score (and under an absolute cap) are kept. [learned] drawings
     * similar to this one improve the score of their app. Returns null when fewer than two distinct keys are touched.
     */
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

    /** Score of [path] against one [word], or null when either is too short or a letter has no key. */
    fun distance(path: List<GesturePoint>, word: String): Float? {
        if (path.size < 2 || word.length < 2 || !word.all { it in keys }) return null
        return score(resample(path), path.count { it.repeat }, word)
    }

    private fun score(sampled: List<GesturePoint>, repeats: Int, word: String): Float {
        val ideal = idealPaths.getOrPut(word) {
            resample(word.mapIndexed { i, c -> keys.getValue(c).copy(repeat = i > 0 && word[i - 1] == c) })
        }
        val doubles = word.zipWithNext().count { (a, b) -> a == b }
        return minOf(fit(sampled, ideal), aligned(sampled, ideal)) +
            DIRECTION_WEIGHT * GestureGeometry.direction(sampled, ideal) +
            MISSED_DOUBLE_PENALTY * (doubles - repeats).coerceAtLeast(0) +
            EXTRA_DWELL_PENALTY * (repeats - doubles).coerceAtLeast(0)
    }

    private fun fit(drawn: List<GesturePoint>, ideal: List<GesturePoint>): Float {
        val location = drawn.indices.sumOf { distance(drawn[it], ideal[it]).toDouble() }.toFloat() / drawn.size
        val ends = distance(drawn.first(), ideal.first()) + distance(drawn.last(), ideal.last())
        return (location + ENDS_WEIGHT * ends) / pitch
    }

    private fun aligned(drawn: List<GesturePoint>, ideal: List<GesturePoint>): Float {
        val from = GestureGeometry.centroid(drawn)
        val to = GestureGeometry.centroid(ideal)
        val drawnRadius = GestureGeometry.radius(drawn, from)
        val idealRadius = GestureGeometry.radius(ideal, to)
        val scale = if (drawnRadius == 0f || idealRadius == 0f) {
            1f
        } else {
            (idealRadius / drawnRadius).coerceIn(1 / MAX_SCALE, MAX_SCALE)
        }
        val moved = drawn.map { GesturePoint(to.x + (it.x - from.x) * scale, to.y + (it.y - from.y) * scale) }
        return fit(moved, ideal) + shiftPenalty * distance(from, to) / pitch + SCALE_WEIGHT * abs(ln(scale))
    }

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

    private fun resample(points: List<GesturePoint>): List<GesturePoint> = GestureGeometry.resample(points, SAMPLES)

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
