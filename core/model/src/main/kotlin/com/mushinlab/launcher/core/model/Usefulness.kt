package com.mushinlab.launcher.core.model

import kotlin.math.roundToInt

/** Launch statistics of an app within one space; [dismissedAt] is when a suggestion to add it was last rejected. */
data class SpaceUsage(
    val spaceId: Long,
    val key: AppKey,
    val launchCount: Int,
    val lastLaunchedAt: Long,
    val decayedScore: Double,
    val dismissedAt: Long? = null,
)

/** Proposal to put [add] on the home list in place of [replace]; scores use the [Usefulness] 1..100 scale. */
data class Suggestion(val add: AppKey, val replace: AppKey, val addScore: Int, val replaceScore: Int)

/**
 * Ranks the apps of a space by decayed launch score and proposes home-list replacements.
 *
 * Scores are relative: the most used app gets [MAX], every app gets at least [MIN].
 */
object Usefulness {
    const val MIN = 1
    const val MAX = 100
    const val MARGIN = 10
    const val MIN_LAUNCHES = 3
    const val DISMISS_MS: Long = 14L * 24 * 60 * 60 * 1000

    /** Usefulness score per app at [now] (epoch ms); all apps get [MIN] when no score is positive. */
    fun scores(usage: Collection<SpaceUsage>, now: Long): Map<AppKey, Int> {
        val decayed = usage.associate { it.key to UsageDecay.at(it.decayedScore, it.lastLaunchedAt, now) }
        val top = decayed.values.maxOrNull()?.takeIf { it > 0.0 } ?: return decayed.mapValues { MIN }
        return decayed.mapValues { (_, value) -> (MAX * value / top).roundToInt().coerceIn(MIN, MAX) }
    }

    /**
     * Proposes replacing the weakest home app (lowest score, the later one on ties) with the best app not on home.
     *
     * The candidate must pass [eligible], have at least [MIN_LAUNCHES] launches, score at least [MARGIN] above
     * the weakest app and not have been dismissed within [DISMISS_MS]. Returns null when home is empty or no
     * candidate qualifies.
     */
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
