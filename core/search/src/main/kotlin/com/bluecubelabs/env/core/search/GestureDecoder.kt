package com.bluecubelabs.env.core.search

import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max

data class GesturePoint(val x: Float, val y: Float)

class GestureDecoder(private val keys: Map<Char, GesturePoint>) {
    private val step: Float = keys.values.toList().let { points ->
        points.flatMap { a -> points.filter { it != a }.map { b -> hypot(a.x - b.x, a.y - b.y) } }
            .minOrNull()
            ?.div(SAMPLES_PER_KEY)
            ?: 1f
    }
    private val idealPaths = HashMap<String, String>()

    fun keysAlong(path: List<GesturePoint>): String {
        val out = StringBuilder()
        fun visit(point: GesturePoint) {
            val key = nearest(point) ?: return
            if (out.isEmpty() || out.last() != key) out.append(key)
        }
        path.firstOrNull()?.let(::visit)
        path.zipWithNext { a, b ->
            val count = ceil(hypot(b.x - a.x, b.y - a.y) / step).toInt().coerceAtLeast(1)
            for (i in 1..count) {
                val t = i.toFloat() / count
                visit(GesturePoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t))
            }
        }
        return out.toString()
    }

    fun decode(path: List<GesturePoint>, vocabulary: Collection<String>): String? {
        val drawn = keysAlong(path)
        if (drawn.length < 2) return null
        return vocabulary
            .asSequence()
            .filter { word -> word.length >= 2 && word.all { it in keys } }
            .minByOrNull { score(drawn, it) }
    }

    private fun score(drawn: String, word: String): Float {
        val ideal = idealPaths.getOrPut(word) { keysAlong(word.map { keys.getValue(it) }) }
        var score = Osa.distance(drawn, ideal).toFloat() / max(drawn.length, ideal.length)
        if (drawn.first() != word.first()) score += START_PENALTY
        if (drawn.last() != word.last()) score += END_PENALTY
        return score
    }

    private fun nearest(point: GesturePoint): Char? = keys.minByOrNull { (_, center) ->
        hypot(point.x - center.x, point.y - center.y)
    }?.key

    private companion object {
        const val SAMPLES_PER_KEY = 3f
        const val START_PENALTY = 0.5f
        const val END_PENALTY = 0.25f
    }
}
