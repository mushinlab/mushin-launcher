package com.bluecubelabs.env.core.search

import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.LearnedGesture
import kotlin.math.max

object GestureLearning {
    const val MIN_SIMILARITY = 0.6
    const val MAX_SAMPLES = 300
    private const val COMPETITOR_DECAY = 0.5
    private const val MIN_WEIGHT = 0.25

    fun similarity(a: String, b: String): Double {
        val length = max(a.length, b.length)
        return if (length == 0) 1.0 else 1.0 - Osa.distance(a, b).toDouble() / length
    }

    fun learn(samples: List<LearnedGesture>, paths: List<String>, key: AppKey): List<LearnedGesture> {
        var result = samples
        paths.distinct().forEach { path ->
            result = result.map { sample ->
                if (sample.key != key && similarity(sample.path, path) >= MIN_SIMILARITY) {
                    sample.copy(weight = sample.weight * COMPETITOR_DECAY)
                } else {
                    sample
                }
            }
            val existing = result.indexOfFirst { it.path == path && it.key == key }
            result = if (existing >= 0) {
                result.toMutableList().also { it[existing] = it[existing].copy(weight = it[existing].weight + 1) }
            } else {
                result + LearnedGesture(path, key, 1.0)
            }
        }
        return result.filter { it.weight >= MIN_WEIGHT }.sortedByDescending { it.weight }.take(MAX_SAMPLES)
    }
}
