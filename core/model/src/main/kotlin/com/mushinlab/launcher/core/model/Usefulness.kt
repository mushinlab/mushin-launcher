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

/**
 * What a space knows for its hints (ADR-0025): [observedSince] is when its launch statistics started (epoch ms,
 * the space creation time), [hintAt] when a hint was last accepted or dismissed, and [launchDays] the number of
 * distinct local calendar days each app was launched in the space within the last [Usefulness.WINDOW_DAYS] days.
 */
data class HintHistory(val observedSince: Long, val hintAt: Long?, val launchDays: Map<AppKey, Int>)

/** Proposal to put [add] on the home list in place of [replace]; scores use the [Usefulness] 1..100 scale. */
data class Suggestion(val add: AppKey, val replace: AppKey, val addScore: Int, val replaceScore: Int)

/**
 * Ranks the apps of a space by decayed launch score and proposes home-list replacements.
 *
 * Scores are relative: the most used app gets [MAX], every app gets at least [MIN]. Hints rely on a week of data
 * (ADR-0025): launch days are counted over [WINDOW_DAYS] days and kept for [DAYS_KEPT] days.
 */
object Usefulness {
    const val MIN = 1
    const val MAX = 100
    const val MARGIN = 10
    const val MIN_LAUNCHES = 3
    const val DISMISS_MS: Long = 14L * 24 * 60 * 60 * 1000
    const val WEEK_MS: Long = 7L * 24 * 60 * 60 * 1000
    const val MIN_LAUNCH_DAYS = 3
    const val WINDOW_DAYS = 7
    const val DAYS_KEPT = 14

    /** Usefulness score per app at [now] (epoch ms); all apps get [MIN] when no score is positive. */
    fun scores(usage: Collection<SpaceUsage>, now: Long): Map<AppKey, Int> {
        val decayed = usage.associate { it.key to UsageDecay.at(it.decayedScore, it.lastLaunchedAt, now) }
        val top = decayed.values.maxOrNull()?.takeIf { it > 0.0 } ?: return decayed.mapValues { MIN }
        return decayed.mapValues { (_, value) -> (MAX * value / top).roundToInt().coerceIn(MIN, MAX) }
    }

    /**
     * Proposes replacing the weakest home app (lowest score, the later one on ties) with the best app not on home.
     *
     * Nothing is proposed until [WEEK_MS] has passed since [HintHistory.observedSince], nor within [WEEK_MS] of
     * [HintHistory.hintAt]. The candidate must pass [eligible], have at least [MIN_LAUNCHES] launches on at least
     * [MIN_LAUNCH_DAYS] days of [HintHistory.launchDays], score at least [MARGIN] above the weakest app and not have
     * been dismissed within [DISMISS_MS]. Returns null when home is empty or no candidate qualifies.
     */
    fun suggest(
        home: List<AppKey>,
        usage: Collection<SpaceUsage>,
        eligible: (AppKey) -> Boolean,
        now: Long,
        history: HintHistory,
    ): Suggestion? {
        if (home.isEmpty() || !history.allowsHint(now)) return null
        val scores = scores(usage, now)
        val onHome = home.toSet()
        val weakest = home.withIndex()
            .minWith(compareBy<IndexedValue<AppKey>> { scores[it.value] ?: MIN }.thenByDescending { it.index })
            .value
        val weakestScore = scores[weakest] ?: MIN
        val candidate = usage
            .asSequence()
            .filter { it.key !in onHome && eligible(it.key) }
            .filter { it.launchCount >= MIN_LAUNCHES && (history.launchDays[it.key] ?: 0) >= MIN_LAUNCH_DAYS }
            .filter { it.dismissedAt == null || now - it.dismissedAt >= DISMISS_MS }
            .map { it.key to (scores[it.key] ?: MIN) }
            .filter { (_, score) -> score >= weakestScore + MARGIN }
            .maxByOrNull { (_, score) -> score }
            ?: return null
        return Suggestion(candidate.first, weakest, candidate.second, weakestScore)
    }

    private fun HintHistory.allowsHint(now: Long): Boolean =
        now - observedSince >= WEEK_MS && (hintAt == null || now - hintAt >= WEEK_MS)
}
