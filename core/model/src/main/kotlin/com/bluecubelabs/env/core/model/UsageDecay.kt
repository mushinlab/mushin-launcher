package com.bluecubelabs.env.core.model

import kotlin.math.exp

object UsageDecay {
    const val TAU_MS: Double = 14.0 * 24 * 60 * 60 * 1000

    fun increment(score: Double, lastLaunchedAt: Long, now: Long): Double = at(score, lastLaunchedAt, now) + 1.0

    fun at(score: Double, lastLaunchedAt: Long, now: Long): Double {
        val dt = (now - lastLaunchedAt).coerceAtLeast(0L)
        return score * exp(-dt / TAU_MS)
    }
}
