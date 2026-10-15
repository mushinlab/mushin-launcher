package com.mushinlab.launcher.core.model

import kotlin.math.exp

/**
 * Exponential decay of launch scores with time constant [TAU_MS] (14 days).
 *
 * A score `s` recorded at `lastLaunchedAt` is worth `s * exp(-(now - lastLaunchedAt) / TAU_MS)` at `now`;
 * [increment] adds one launch to the decayed value. A clock that went backwards counts as no elapsed time.
 */
object UsageDecay {
    const val TAU_MS: Double = 14.0 * 24 * 60 * 60 * 1000

    fun increment(score: Double, lastLaunchedAt: Long, now: Long): Double = at(score, lastLaunchedAt, now) + 1.0

    fun at(score: Double, lastLaunchedAt: Long, now: Long): Double {
        val dt = (now - lastLaunchedAt).coerceAtLeast(0L)
        return score * exp(-dt / TAU_MS)
    }
}
