package com.mushinlab.launcher.core.data

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.repo.GestureRepository
import com.mushinlab.launcher.core.data.repo.HistoryRepository
import com.mushinlab.launcher.core.data.repo.OverrideRepository
import com.mushinlab.launcher.core.data.repo.PackageDataCleaner
import com.mushinlab.launcher.core.data.repo.SpaceRepository
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.OverrideLimits
import com.mushinlab.launcher.core.model.SpaceLimits
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.model.SwipeDirection
import kotlin.math.exp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryAndOverridesTest {
    @get:Rule
    val rule = DbTestRule()

    private val slack = AppKey("com.slack", "com.slack.Main", 0)
    private val maps = AppKey("com.google.maps", "com.google.maps.Main", 0)

    private suspend fun normalId(repo: SpaceRepository): Long =
        repo.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.NORMAL }.id

    @Test
    fun AC_10_3_onlySearchLaunchesCreateRecents() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val history = HistoryRepository(rule.db, backgroundScope, rule.clock)
        val space = normalId(spaces)
        history.recordLaunch(slack, space, fromSearch = false)
        history.recordLaunch(maps, space, fromSearch = true)
        val recents = history.recents.first { it[space] != null }[space].orEmpty()
        assertThat(recents.map { it.key }).containsExactly(maps)
        val usage = history.usage.first { it.size == 2 }
        assertThat(usage[slack]?.launchCount).isEqualTo(1)
        assertThat(usage[maps]?.launchCount).isEqualTo(1)
    }

    @Test
    fun usageAndAllRecentsCanBeErased() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val history = HistoryRepository(rule.db, backgroundScope, rule.clock)
        val normal = normalId(spaces)
        val work = spaces.spaces.first { it.isNotEmpty() }.first { it.type != SpaceType.NORMAL }.id
        history.recordLaunch(slack, normal, fromSearch = true)
        history.recordLaunch(maps, work, fromSearch = true)
        history.recents.first { it.size == 2 }
        history.clearAllRecents()
        assertThat(history.recents.first { it.isEmpty() }).isEmpty()
        assertThat(history.usage.value).hasSize(2)
        history.clearUsage()
        assertThat(history.usage.first { it.isEmpty() }).isEmpty()
    }

    @Test
    fun erasingUsageAlsoErasesRecentsOfEverySpace() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val history = HistoryRepository(rule.db, backgroundScope, rule.clock)
        val normal = normalId(spaces)
        val work = spaces.spaces.first { it.isNotEmpty() }.first { it.type != SpaceType.NORMAL }.id
        history.recordLaunch(slack, normal, fromSearch = true)
        history.recordLaunch(maps, work, fromSearch = true)
        history.recents.first { it.size == 2 }
        history.clearUsage()
        assertThat(history.recents.first { it.isEmpty() }).isEmpty()
        assertThat(history.usage.first { it.isEmpty() }).isEmpty()
    }

    @Test
    fun everyLaunchIsCountedPerSpaceAndDismissalIsKept() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val history = HistoryRepository(rule.db, backgroundScope, rule.clock)
        val normal = normalId(spaces)
        val work = spaces.spaces.first { it.isNotEmpty() }.first { it.type != SpaceType.NORMAL }.id
        history.recordLaunch(slack, normal, fromSearch = false)
        history.recordLaunch(slack, normal, fromSearch = true)
        history.recordLaunch(slack, work, fromSearch = false)
        history.recordLaunch(maps, null, fromSearch = false)
        val usage = history.spaceUsage.first { it[normal]?.firstOrNull()?.launchCount == 2 && it[work] != null }
        assertThat(usage[normal]?.map { it.key }).containsExactly(slack)
        assertThat(usage[work]?.single()?.launchCount).isEqualTo(1)
        history.dismissSuggestion(normal, slack)
        rule.now += 1000
        history.recordLaunch(slack, normal, fromSearch = false)
        val after = history.spaceUsage.first { it[normal]?.single()?.launchCount == 3 }[normal]?.single()
        assertThat(after?.dismissedAt).isEqualTo(rule.now - 1000)
        history.clearUsage()
        assertThat(history.spaceUsage.first { it.isEmpty() }).isEmpty()
    }

    @Test
    fun recentsAreCappedPerSpace() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val history = HistoryRepository(rule.db, backgroundScope, rule.clock)
        val space = normalId(spaces)
        repeat(SpaceLimits.RECENTS_PER_SPACE + 5) { i ->
            rule.now += 1000
            history.recordLaunch(AppKey("p$i", "a$i", 0), space, fromSearch = true)
        }
        val rows = history.recents.first { it[space]?.firstOrNull()?.key?.packageName == "p54" }[space].orEmpty()
        assertThat(rows).hasSize(SpaceLimits.RECENTS_PER_SPACE)
        assertThat(rows.map { it.key.packageName }).doesNotContain("p0")
    }

    @Test
    fun decayedScoreIsIncrementedWithExponentialDecay() = runTest {
        val history = HistoryRepository(rule.db, backgroundScope, rule.clock)
        history.recordLaunch(slack, null, fromSearch = false)
        rule.now += 14L * 24 * 60 * 60 * 1000
        history.recordLaunch(slack, null, fromSearch = false)
        val usage = history.usage.first { it[slack]?.launchCount == 2 }[slack]
        assertThat(usage?.decayedScore).isWithin(1e-9).of(exp(-1.0) + 1.0)
    }

    @Test
    fun AC_11_4_renameAndResetRemovesOverride() = runTest {
        val overrides = OverrideRepository(rule.db, backgroundScope, rule.clock)
        assertThat(overrides.rename(slack, "  Работа-чат ")).isTrue()
        assertThat(overrides.overrides.first { it.isNotEmpty() }[slack]?.customLabel).isEqualTo("Работа-чат")
        assertThat(overrides.rename(slack, "x".repeat(41))).isFalse()
        overrides.rename(slack, "")
        assertThat(overrides.overrides.first { it.isEmpty() }).isEmpty()
    }

    @Test
    fun AC_11_5_hideKeepsLabelAndUnhideKeepsRow() = runTest {
        val overrides = OverrideRepository(rule.db, backgroundScope, rule.clock)
        overrides.setHidden(slack, true)
        overrides.rename(slack, "Чат")
        val first = overrides.overrides.first { it[slack]?.customLabel != null }[slack]
        assertThat(first?.hidden).isTrue()
        overrides.setHidden(slack, false)
        val second = overrides.overrides.first { it[slack]?.hidden == false }[slack]
        assertThat(second?.customLabel).isEqualTo("Чат")
    }

    @Test
    fun unhidingAnAppWithoutCustomLabelRemovesTheRow() = runTest {
        val overrides = OverrideRepository(rule.db, backgroundScope, rule.clock)
        overrides.setHidden(slack, true)
        assertThat(overrides.overrides.first { it.isNotEmpty() }[slack]?.hidden).isTrue()
        overrides.setHidden(slack, false)
        assertThat(overrides.overrides.first { it.isEmpty() }).isEmpty()
    }

    @Test
    fun Q12_removedPackageClearsDataAndKeepsOverrideFor30Days() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val history = HistoryRepository(rule.db, backgroundScope, rule.clock)
        val overrides = OverrideRepository(rule.db, backgroundScope, rule.clock)
        val cleaner = PackageDataCleaner(rule.db, rule.clock)
        val space = normalId(spaces)
        spaces.setItems(space, listOf(slack, maps))
        history.recordLaunch(slack, space, fromSearch = true)
        overrides.rename(slack, "Чат")

        cleaner.onPackageRemoved(slack.packageName, slack.userSerial)

        assertThat(spaces.itemsOf(space)).containsExactly(maps)
        assertThat(rule.db.appUsageDao().get(slack.packageName, slack.activityName, 0)).isNull()
        assertThat(rule.db.searchRecentDao().get(space, slack.packageName, slack.activityName, 0)).isNull()
        val orphan = rule.db.appOverrideDao().get(slack.packageName, slack.activityName, 0)
        assertThat(orphan?.orphanedAt).isEqualTo(rule.now)

        rule.now += OverrideLimits.ORPHAN_TTL_MS - 1
        assertThat(cleaner.purgeExpiredOverrides()).isEqualTo(0)
        rule.now += 2
        assertThat(cleaner.purgeExpiredOverrides()).isEqualTo(1)
    }

    @Test
    fun Q12_reinstallWithin30DaysRestoresOverride() = runTest {
        val overrides = OverrideRepository(rule.db, backgroundScope, rule.clock)
        val cleaner = PackageDataCleaner(rule.db, rule.clock)
        overrides.rename(slack, "Чат")
        cleaner.onPackageRemoved(slack.packageName, 0)
        cleaner.onPackageAdded(slack.packageName, 0)
        rule.now += OverrideLimits.ORPHAN_TTL_MS * 2
        assertThat(cleaner.purgeExpiredOverrides()).isEqualTo(0)
        assertThat(rule.db.appOverrideDao().get(slack.packageName, slack.activityName, 0)?.customLabel).isEqualTo("Чат")
    }

    @Test
    fun AC_12_1_defaultGestureBindingsAreSeeded() = runTest {
        val gestures = GestureRepository(rule.db, backgroundScope)
        val stored = rule.db.gestureBindingDao().observeAll().first()
        assertThat(stored.associate { it.direction to it.action }).containsExactly(
            "UP",
            "SEARCH",
            "DOWN",
            "NOTIFICATIONS",
            "LEFT",
            "NONE",
            "RIGHT",
            "NONE",
        )
        gestures.set(SwipeDirection.LEFT, GestureAction.LaunchApp(slack))
        val bindings = gestures.bindings.first { it[SwipeDirection.LEFT] is GestureAction.LaunchApp }
        assertThat(bindings[SwipeDirection.LEFT]).isEqualTo(GestureAction.LaunchApp(slack))
        assertThat(bindings[SwipeDirection.UP]).isEqualTo(GestureAction.Search)
    }
}
