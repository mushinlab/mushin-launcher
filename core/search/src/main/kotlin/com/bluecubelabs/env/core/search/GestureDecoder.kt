package com.bluecubelabs.env.core.search

import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.LearnedGesture
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot

data class GesturePoint(val x: Float, val y: Float, val repeat: Boolean = false)

data class GestureWord(val latin: String, val text: String, val key: AppKey)

data class GestureResult(val path: String, val word: GestureWord?)

class GestureDecoder(private val keys: Map<Char, GesturePoint>) {
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
        val best = vocabulary.asSequence()
            .filter { word -> word.latin.length >= 2 && word.latin.all { it in keys } }
            .map { word -> word to score(sampled, repeats, word.latin) - (boosts[word.key] ?: 0f) }
            .minByOrNull { it.second }
        return GestureResult(drawn, best?.takeIf { it.second <= MAX_SCORE }?.first)
    }

    private fun score(sampled: List<GesturePoint>, repeats: Int, word: String): Float {
        val ideal = idealPaths.getOrPut(word) {
            resample(word.mapIndexed { i, c -> keys.getValue(c).copy(repeat = i > 0 && word[i - 1] == c) })
        }
        val location = sampled.indices.sumOf { distance(sampled[it], ideal[it]).toDouble() }.toFloat() /
            sampled.size / pitch
        val ends = (distance(sampled.first(), ideal.first()) + distance(sampled.last(), ideal.last())) / pitch
        val doubles = word.zipWithNext().count { (a, b) -> a == b }
        return location + ENDS_WEIGHT * ends + DOUBLE_PENALTY * abs(repeats - doubles)
    }

    private fun boosts(drawn: String, learned: List<LearnedGesture>): Map<AppKey, Float> {
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
        const val DOUBLE_PENALTY = 0.1f
        const val MAX_SCORE = 2f
        const val LEARN_BONUS = 1.5
    }
}
