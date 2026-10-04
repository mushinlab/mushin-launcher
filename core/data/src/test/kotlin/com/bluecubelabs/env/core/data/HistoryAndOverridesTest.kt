package com.bluecubelabs.env.core.data

import com.bluecubelabs.env.core.data.repo.EnvironmentRepository
import com.bluecubelabs.env.core.data.repo.GestureRepository
import com.bluecubelabs.env.core.data.repo.HistoryRepository
import com.bluecubelabs.env.core.data.repo.OverrideRepository
import com.bluecubelabs.env.core.data.repo.PackageDataCleaner
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.EnvLimits
import com.bluecubelabs.env.core.model.EnvType
import com.bluecubelabs.env.core.model.GestureAction
import com.bluecubelabs.env.core.model.SwipeDirection
import com.google.common.truth.Truth.assertThat
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

    private suspend fun normalId(repo: EnvironmentRepository): Long =
        repo.environments.first { it.isNotEmpty() }.first { it.type == EnvType.NORMAL }.id

    @Test
    fun AC_10_3_onlySearchLaunchesCreateRecents() = runTest {
        val envs = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val history = HistoryRepository(rule.db, backgroundScope, rule.clock)
        val env = normalId(envs)
        history.recordLaunch(slack, env, fromSearch = false)
        history.recordLaunch(maps, env, fromSearch = true)
        val recents = history.recents.first { it[env] != null }[env].orEmpty()
        assertThat(recents.map { it.key }).containsExactly(maps)
        val usage = history.usage.first { it.size == 2 }
        assertThat(usage[slack]?.launchCount).isEqualTo(1)
        assertThat(usage[maps]?.launchCount).isEqualTo(1)
    }

    @Test
    fun usageAndAllRecentsCanBeErased() = runTest {
        val envs = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val history = HistoryRepository(rule.db, backgroundScope, rule.clock)
        val normal = normalId(envs)
        val work = envs.environments.first { it.isNotEmpty() }.first { it.type != EnvType.NORMAL }.id
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
    fun everyLaunchIsCountedPerSpaceAndDismissalIsKept() = runTest {
        val envs = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val history = HistoryRepository(rule.db, backgroundScope, rule.clock)
        val normal = normalId(envs)
        val work = envs.environments.first { it.isNotEmpty() }.first { it.type != EnvType.NORMAL }.id
        history.recordLaunch(slack, normal, fromSearch = false)
        history.recordLaunch(slack, normal, fromSearch = true)
        history.recordLaunch(slack, work, fromSearch = false)
        history.recordLaunch(maps, null, fromSearch = false)
        val spaces = history.spaceUsage.first { it[normal]?.firstOrNull()?.launchCount == 2 && it[work] != null }
        assertThat(spaces[normal]?.map { it.key }).containsExactly(slack)
        assertThat(spaces[work]?.single()?.launchCount).isEqualTo(1)
        history.dismissSuggestion(normal, slack)
        rule.now += 1000
        history.recordLaunch(slack, normal, fromSearch = false)
        val after = history.spaceUsage.first { it[normal]?.single()?.launchCount == 3 }[normal]?.single()
        assertThat(after?.dismissedAt).isEqualTo(rule.now - 1000)
        history.clearUsage()
        assertThat(history.spaceUsage.first { it.isEmpty() }).isEmpty()
    }

    @Test
    fun recentsAreCappedPerEnvironment() = runTest {
        val envs = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val history = HistoryRepository(rule.db, backgroundScope, rule.clock)
        val env = normalId(envs)
        repeat(EnvLimits.RECENTS_PER_ENV + 5) { i ->
            rule.now += 1000
            history.recordLaunch(AppKey("p$i", "a$i", 0), env, fromSearch = true)
        }
        val rows = history.recents.first { it[env]?.firstOrNull()?.key?.packageName == "p54" }[env].orEmpty()
        assertThat(rows).hasSize(EnvLimits.RECENTS_PER_ENV)
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
    fun Q12_removedPackageClearsDataAndKeepsOverrideFor30Days() = runTest {
        val envs = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val history = HistoryRepository(rule.db, backgroundScope, rule.clock)
        val overrides = OverrideRepository(rule.db, backgroundScope, rule.clock)
        val cleaner = PackageDataCleaner(rule.db, rule.clock)
        val env = normalId(envs)
        envs.setItems(env, listOf(slack, maps))
        history.recordLaunch(slack, env, fromSearch = true)
        overrides.rename(slack, "Чат")

        cleaner.onPackageRemoved(slack.packageName, slack.userSerial)

        assertThat(envs.itemsOf(env)).containsExactly(maps)
        assertThat(rule.db.appUsageDao().get(slack.packageName, slack.activityName, 0)).isNull()
        assertThat(rule.db.searchRecentDao().get(env, slack.packageName, slack.activityName, 0)).isNull()
        val orphan = rule.db.appOverrideDao().get(slack.packageName, slack.activityName, 0)
        assertThat(orphan?.orphanedAt).isEqualTo(rule.now)

        rule.now += EnvLimits.ORPHAN_TTL_MS - 1
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
        rule.now += EnvLimits.ORPHAN_TTL_MS * 2
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
