package com.mushinlab.launcher.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UsefulnessTest {
    private val now = 1_000_000_000_000L
    private val a = AppKey("a", "A", 0)
    private val b = AppKey("b", "B", 0)
    private val c = AppKey("c", "C", 0)
    private val d = AppKey("d", "D", 0)

    private val week = Usefulness.WEEK_MS
    private val open = HintHistory(observedSince = now - week, hintAt = null, launchDays = everyDay(a, b, c, d))

    private fun everyDay(vararg keys: AppKey) = keys.associateWith { Usefulness.WINDOW_DAYS }

    private fun usage(key: AppKey, score: Double, launches: Int = 5, dismissedAt: Long? = null) =
        SpaceUsage(1, key, launches, now, score, dismissedAt)

    private val home = listOf(a, b)
    private val base = listOf(usage(a, 10.0), usage(b, 1.0), usage(c, 8.0))

    private fun suggestWith(history: HintHistory) = Usefulness.suggest(home, base, { true }, now, history)

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
            history = open,
        )

        assertThat(result).isEqualTo(Suggestion(add = c, replace = b, addScore = 60, replaceScore = 10))
    }

    @Test
    fun homeAppWithoutLaunchesCountsAsMinimum() {
        val result = Usefulness.suggest(listOf(a, b), listOf(usage(a, 10.0), usage(c, 2.0)), { true }, now, open)

        assertThat(result?.replace).isEqualTo(b)
        assertThat(result?.replaceScore).isEqualTo(Usefulness.MIN)
    }

    @Test
    fun noSuggestionWhenTheGapIsBelowTheMargin() {
        val result = Usefulness.suggest(listOf(a, b), listOf(usage(a, 10.0), usage(b, 5.0), usage(c, 5.5)), {
            true
        }, now, open)

        assertThat(result).isNull()
    }

    @Test
    fun ignoresRareDismissedAndIneligibleApps() {
        val base = listOf(usage(a, 10.0), usage(b, 1.0))

        assertThat(Usefulness.suggest(home, base + usage(c, 8.0, launches = 2), { true }, now, open)).isNull()
        assertThat(Usefulness.suggest(home, base + usage(c, 8.0, dismissedAt = now - 1), { true }, now, open)).isNull()
        assertThat(Usefulness.suggest(home, base + usage(c, 8.0), { it != c }, now, open)).isNull()
        val expired = usage(c, 8.0, dismissedAt = now - Usefulness.DISMISS_MS)
        assertThat(Usefulness.suggest(home, base + expired, { true }, now, open)?.add).isEqualTo(c)
    }

    @Test
    fun tieBetweenWeakHomeAppsPicksTheLowestOnScreen() {
        val result = Usefulness.suggest(listOf(a, b, d), listOf(usage(a, 10.0), usage(c, 8.0)), { true }, now, open)

        assertThat(result?.replace).isEqualTo(d)
    }

    @Test
    fun emptyHomeGivesNoSuggestion() {
        assertThat(Usefulness.suggest(emptyList(), listOf(usage(c, 8.0)), { true }, now, open)).isNull()
    }

    @Test
    fun theFirstHintWaitsAWeekOfObservation() {
        assertThat(suggestWith(open.copy(observedSince = now - week + 1))).isNull()
        assertThat(suggestWith(open.copy(observedSince = now))).isNull()
        assertThat(suggestWith(open.copy(observedSince = now - week))?.add).isEqualTo(c)
    }

    @Test
    fun theCandidateNeedsLaunchesOnThreeDistinctDays() {
        val twoDays = open.copy(launchDays = everyDay(a, b) + (c to Usefulness.MIN_LAUNCH_DAYS - 1))
        val threeDays = open.copy(launchDays = everyDay(a, b) + (c to Usefulness.MIN_LAUNCH_DAYS))

        assertThat(suggestWith(twoDays)).isNull()
        assertThat(suggestWith(open.copy(launchDays = everyDay(a, b)))).isNull()
        assertThat(suggestWith(threeDays)?.add).isEqualTo(c)
    }

    @Test
    fun manyLaunchesOnFewDaysDoNotMakeACandidate() {
        val busyDay = listOf(usage(a, 10.0), usage(b, 1.0), usage(c, 9.0, launches = 30))
        val oneDay = open.copy(launchDays = mapOf(c to 1))

        assertThat(Usefulness.suggest(home, busyDay, { true }, now, oneDay)).isNull()
    }

    @Test
    fun threeDaysWithFewerThanThreeLaunchesAreStillTooRare() {
        val rare = listOf(usage(a, 10.0), usage(b, 1.0), usage(c, 8.0, launches = Usefulness.MIN_LAUNCHES - 1))

        assertThat(Usefulness.suggest(home, rare, { true }, now, open)).isNull()
    }

    @Test
    fun aHintIsShownAtMostOnceAWeek() {
        assertThat(suggestWith(open.copy(hintAt = now))).isNull()
        assertThat(suggestWith(open.copy(hintAt = now - week + 1))).isNull()
        assertThat(suggestWith(open.copy(hintAt = now - week))?.add).isEqualTo(c)
        assertThat(suggestWith(open.copy(hintAt = null))?.add).isEqualTo(c)
    }

    @Test
    fun aWeeklyPauseDoesNotLiftTheTwoWeekDismissal() {
        val dismissed = listOf(usage(a, 10.0), usage(b, 1.0), usage(c, 8.0, dismissedAt = now - week))
        val pauseOver = open.copy(hintAt = now - week)

        assertThat(Usefulness.suggest(home, dismissed, { true }, now, pauseOver)).isNull()
        val later = now + Usefulness.DISMISS_MS - week
        assertThat(Usefulness.suggest(home, dismissed, { true }, later, pauseOver)?.add).isEqualTo(c)
    }

    @Test
    fun anotherQualifiedCandidateIsChosenWhenTheBestHasTooFewDays() {
        val usage = base + usage(d, 6.0)
        val history = open.copy(launchDays = everyDay(a, b, d) + (c to 2))

        assertThat(Usefulness.suggest(home, usage, { true }, now, history)?.add).isEqualTo(d)
    }
}
