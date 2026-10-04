package com.bluecubelabs.env.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UsefulnessTest {
    private val now = 1_000_000_000_000L
    private val a = AppKey("a", "A", 0)
    private val b = AppKey("b", "B", 0)
    private val c = AppKey("c", "C", 0)
    private val d = AppKey("d", "D", 0)

    private fun usage(key: AppKey, score: Double, launches: Int = 5, dismissedAt: Long? = null) =
        SpaceUsage(1, key, launches, now, score, dismissedAt)

    @Test
    fun scoresAreRelativeToTheMostUsefulAppAndClampedToOneHundred() {
        val scores = Usefulness.scores(listOf(usage(a, 10.0), usage(b, 5.0), usage(c, 0.01)), now)

        assertThat(scores).containsExactly(a, 100, b, 50, c, 1)
    }

    @Test
    fun olderLaunchesDecay() {
        val old = SpaceUsage(1, b, 5, now - UsageDecay.TAU_MS.toLong(), 10.0)

        val scores = Usefulness.scores(listOf(usage(a, 10.0), old), now)

        assertThat(scores[b]).isEqualTo(37)
    }

    @Test
    fun suggestsTheBestAppThatBeatsTheWeakestHomeApp() {
        val result = Usefulness.suggest(
            home = listOf(a, b),
            usage = listOf(usage(a, 10.0), usage(b, 1.0), usage(c, 6.0), usage(d, 4.0)),
            eligible = { true },
            now = now,
        )

        assertThat(result).isEqualTo(Suggestion(add = c, replace = b, addScore = 60, replaceScore = 10))
    }

    @Test
    fun homeAppWithoutLaunchesCountsAsMinimum() {
        val result = Usefulness.suggest(listOf(a, b), listOf(usage(a, 10.0), usage(c, 2.0)), { true }, now)

        assertThat(result?.replace).isEqualTo(b)
        assertThat(result?.replaceScore).isEqualTo(Usefulness.MIN)
    }

    @Test
    fun noSuggestionWhenTheGapIsBelowTheMargin() {
        val result = Usefulness.suggest(listOf(a, b), listOf(usage(a, 10.0), usage(b, 5.0), usage(c, 5.5)), {
            true
        }, now)

        assertThat(result).isNull()
    }

    @Test
    fun ignoresRareDismissedAndIneligibleApps() {
        val home = listOf(a, b)
        val base = listOf(usage(a, 10.0), usage(b, 1.0))

        assertThat(Usefulness.suggest(home, base + usage(c, 8.0, launches = 2), { true }, now)).isNull()
        assertThat(Usefulness.suggest(home, base + usage(c, 8.0, dismissedAt = now - 1), { true }, now)).isNull()
        assertThat(Usefulness.suggest(home, base + usage(c, 8.0), { it != c }, now)).isNull()
        val expired = usage(c, 8.0, dismissedAt = now - Usefulness.DISMISS_MS)
        assertThat(Usefulness.suggest(home, base + expired, { true }, now)?.add).isEqualTo(c)
    }

    @Test
    fun tieBetweenWeakHomeAppsPicksTheLowestOnScreen() {
        val result = Usefulness.suggest(listOf(a, b, d), listOf(usage(a, 10.0), usage(c, 8.0)), { true }, now)

        assertThat(result?.replace).isEqualTo(d)
    }

    @Test
    fun emptyHomeGivesNoSuggestion() {
        assertThat(Usefulness.suggest(emptyList(), listOf(usage(c, 8.0)), { true }, now)).isNull()
    }
}
