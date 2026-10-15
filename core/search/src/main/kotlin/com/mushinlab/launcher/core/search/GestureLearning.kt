package com.mushinlab.launcher.core.search

import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.LearnedGesture
import com.mushinlab.launcher.core.model.ShapeVertex
import kotlin.math.max

object GestureLearning {
    const val MIN_SIMILARITY = 0.6
    const val MAX_SAMPLES = 300
    const val MAX_PATH = 64
    const val PASSIVE_MAX_DISTANCE = 3f
    private const val COMPETITOR_DECAY = 0.5
    private const val MIN_WEIGHT = 0.25

    fun similarity(a: String, b: String): Double {
        val length = max(a.length, b.length)
        return if (length == 0) 1.0 else 1.0 - Osa.distance(a, b).toDouble() / length
    }

    fun learn(samples: List<LearnedGesture>, paths: List<String>, key: AppKey): List<LearnedGesture> =
        learn(samples, paths.map { GestureSample(it) }, key)

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

    fun resembles(decoder: GestureDecoder, points: List<GesturePoint>, words: List<String>): Boolean =
        words.any { word -> (decoder.distance(points, word) ?: Float.MAX_VALUE) <= PASSIVE_MAX_DISTANCE }
}

data class GestureSample(val path: String, val shape: List<ShapeVertex> = emptyList())
