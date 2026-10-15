package com.mushinlab.launcher.core.search

import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.LearnedGesture

/**
 * Recognizes drawings by outline rather than by keys, against the learned shapes of [LearnedGesture] samples.
 *
 * Outlines are normalized for position and size. A sample matches when the size ratio is within [maxScale]
 * and the mean point distance, in units of outline radius, is at most [threshold]. Samples without an outline
 * use their key path over [keys]; [areaWidth] and [areaHeight] turn stored fractions back into pixels.
 */
class ShapeMatcher(
    private val keys: Map<Char, GesturePoint>,
    private val areaWidth: Float,
    private val areaHeight: Float,
    private val maxScale: Float = MAX_SCALE,
    private val threshold: Float = THRESHOLD,
) {
    /** Apps whose learned shapes match [drawn], best first, limited to those close to the best match. */
    fun match(drawn: List<GesturePoint>, learned: List<LearnedGesture>): List<AppKey> {
        val shape = Shapes.normalize(drawn) ?: return emptyList()
        val best = HashMap<AppKey, Float>()
        learned.forEach { sample ->
            val ideal = Shapes.normalize(outline(sample)) ?: return@forEach
            val scale = shape.radius / ideal.radius
            if (scale > maxScale * SLACK || scale * maxScale * SLACK < 1) return@forEach
            val distance = shape.distanceTo(ideal)
            if (distance <= threshold) best.merge(sample.key, distance, ::minOf)
        }
        val top = best.values.minOrNull() ?: return emptyList()
        return best.entries.filter { it.value <= top + MARGIN }.sortedBy { it.value }.map { it.key }
    }

    private fun outline(sample: LearnedGesture): List<GesturePoint> = if (sample.shape.size >= 2) {
        sample.shape.map { GesturePoint(it.x * areaWidth, it.y * areaHeight) }
    } else {
        sample.path.mapNotNull { keys[it] }
    }

    companion object {
        const val MAX_SCALE = 2f
        const val THRESHOLD = 0.35f
        private const val SLACK = 1.15f
        private const val MARGIN = 0.05f
    }
}
