package com.mushinlab.launcher.core.model

import kotlin.math.roundToInt

data class SpaceUsage(
    val envId: Long,
    val key: AppKey,
    val launchCount: Int,
    val lastLaunchedAt: Long,
    val decayedScore: Double,
    val dismissedAt: Long? = null,
)

data class Suggestion(val add: AppKey, val replace: AppKey, val addScore: Int, val replaceScore: Int)

object Usefulness {
    const val MIN = 1
    const val MAX = 100
    const val MARGIN = 10
    const val MIN_LAUNCHES = 3
    const val DISMISS_MS: Long = 14L * 24 * 60 * 60 * 1000

    fun scores(usage: Collection<SpaceUsage>, now: Long): Map<AppKey, Int> {
        val decayed = usage.associate { it.key to UsageDecay.at(it.decayedScore, it.lastLaunchedAt, now) }
        val top = decayed.values.maxOrNull()?.takeIf { it > 0.0 } ?: return decayed.mapValues { MIN }
        return decayed.mapValues { (_, value) -> (MAX * value / top).roundToInt().coerceIn(MIN, MAX) }
    }

    fun suggest(
        home: List<AppKey>,
        usage: Collection<SpaceUsage>,
        eligible: (AppKey) -> Boolean,
        now: Long,
    ): Suggestion? {
        if (home.isEmpty()) return null
        val scores = scores(usage, now)
        val onHome = home.toSet()
        val weakest = home.withIndex()
            .minWith(compareBy<IndexedValue<AppKey>> { scores[it.value] ?: MIN }.thenByDescending { it.index })
            .value
        val weakestScore = scores[weakest] ?: MIN
        val candidate = usage
            .asSequence()
            .filter { it.key !in onHome && eligible(it.key) }
            .filter { it.launchCount >= MIN_LAUNCHES }
            .filter { it.dismissedAt == null || now - it.dismissedAt >= DISMISS_MS }
            .map { it.key to (scores[it.key] ?: MIN) }
            .filter { (_, score) -> score >= weakestScore + MARGIN }
            .maxByOrNull { (_, score) -> score }
            ?: return null
        return Suggestion(candidate.first, weakest, candidate.second, weakestScore)
    }
}
