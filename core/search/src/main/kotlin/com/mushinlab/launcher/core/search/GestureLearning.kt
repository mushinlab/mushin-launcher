package com.mushinlab.launcher.core.search

import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.LearnedGesture
import com.mushinlab.launcher.core.model.ShapeVertex
import kotlin.math.max

/**
 * Per-app memory of confirmed drawings ([LearnedGesture]) that biases [GestureDecoder] toward the user's habits.
 *
 * Only paths of at most [MAX_PATH] keys are learned; at most [MAX_SAMPLES] samples are kept, heaviest first.
 * A stored sample supports a drawing when their [similarity] is at least [MIN_SIMILARITY].
 */
object GestureLearning {
    const val MIN_SIMILARITY = 0.6
    const val MAX_SAMPLES = 300
    const val MAX_PATH = 64
    const val PASSIVE_MAX_DISTANCE = 3f
    private const val COMPETITOR_DECAY = 0.5
    private const val MIN_WEIGHT = 0.25

    /** One minus the OSA distance divided by the longer length: 1.0 for equal paths, 0.0 for unrelated ones. */
    fun similarity(a: String, b: String): Double {
        val length = max(a.length, b.length)
        return if (length == 0) 1.0 else 1.0 - Osa.distance(a, b).toDouble() / length
    }

    /** Same as the [GestureSample] overload, for key paths without outlines. */
    fun learn(samples: List<LearnedGesture>, paths: List<String>, key: AppKey): List<LearnedGesture> =
        learn(samples, paths.map { GestureSample(it) }, key)

    /**
     * Records that [gestures] were meant for [key] and returns the updated samples.
     *
     * An identical sample gains weight 1, a new one starts at 1. Similar samples of other apps lose half their
     * weight and are dropped once they fall below a minimum.
     */
    @JvmName("learnSamples")
    fun learn(samples: List<LearnedGesture>, gestures: List<GestureSample>, key: AppKey): List<LearnedGesture> {
        var result = samples
        gestures.distinctBy { it.path }.filter { it.path.length <= MAX_PATH }.forEach { gesture ->
            result = result.map { sample ->
                if (sample.key != key && similarity(sample.path, gesture.path) >= MIN_SIMILARITY) {
                    sample.copy(weight = sample.weight * COMPETITOR_DECAY)
                } else {
                    sample
                }
            }
            val existing = result.indexOfFirst { it.path == gesture.path && it.key == key }
            result = if (existing >= 0) {
                result.toMutableList().also {
                    val old = it[existing]
                    it[existing] = old.copy(weight = old.weight + 1, shape = gesture.shape.ifEmpty { old.shape })
                }
            } else {
                result + LearnedGesture(gesture.path, key, 1.0, gesture.shape)
            }
        }
        return result.filter { it.weight >= MIN_WEIGHT }.sortedByDescending { it.weight }.take(MAX_SAMPLES)
    }

    /**
     * True when the drawing scores at most [PASSIVE_MAX_DISTANCE] against any of [words], so it can be learned
     * without explicit confirmation.
     */
    fun resembles(decoder: GestureDecoder, points: List<GesturePoint>, words: List<String>): Boolean =
        words.any { word -> (decoder.distance(points, word) ?: Float.MAX_VALUE) <= PASSIVE_MAX_DISTANCE }
}

/** A drawing to learn: its key path and optional outline in fractions of the keyboard area. */
data class GestureSample(val path: String, val shape: List<ShapeVertex> = emptyList())
