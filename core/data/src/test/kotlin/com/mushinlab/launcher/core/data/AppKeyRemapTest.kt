package com.mushinlab.launcher.core.data

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.repo.EnvironmentRepository
import com.mushinlab.launcher.core.data.repo.GestureLearningRepository
import com.mushinlab.launcher.core.data.repo.GestureRepository
import com.mushinlab.launcher.core.data.repo.HistoryRepository
import com.mushinlab.launcher.core.data.repo.OverrideRepository
import com.mushinlab.launcher.core.data.repo.PackageDataCleaner
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.EnvType
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.LearnedGesture
import com.mushinlab.launcher.core.model.SwipeDirection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppKeyRemapTest {
    @get:Rule
    val rule = DbTestRule()

    private val oldIcon = AppKey("org.telegram.messenger", "org.telegram.messenger.DefaultIcon", 0)
    private val newIcon = AppKey("org.telegram.messenger", "org.telegram.messenger.VintageIcon", 0)
    private val maps = AppKey("com.google.maps", "com.google.maps.Main", 0)

    @Test
    fun switchedIconAliasKeepsSwipeEnvRenameHistoryAndLearning() = runTest {
        val envs = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val overrides = OverrideRepository(rule.db, backgroundScope, rule.clock)
        val history = HistoryRepository(rule.db, backgroundScope, rule.clock)
        val gestures = GestureRepository(rule.db, backgroundScope)
        val learning = GestureLearningRepository(rule.db, backgroundScope)
        val env = envs.environments.first { it.isNotEmpty() }.first { it.type == EnvType.NORMAL }.id
        envs.setItems(env, listOf(maps, oldIcon))
        overrides.rename(oldIcon, "TG")
        gestures.set(SwipeDirection.LEFT, GestureAction.LaunchApp(oldIcon))
        learning.update { listOf(LearnedGesture("tgh", oldIcon, 2.0), LearnedGesture("trf", oldIcon, 1.0)) }
        repeat(3) { history.recordLaunch(oldIcon, env, fromSearch = true) }
        rule.now += 60_000
        history.recordLaunch(newIcon, env, fromSearch = true)
        learning.update { it + LearnedGesture("tgh", newIcon, 1.0) }

        PackageDataCleaner(rule.db, rule.clock).reconcile(listOf(maps, newIcon))

        assertThat(envs.itemsOf(env)).containsExactly(maps, newIcon).inOrder()
        assertThat(overrides.overrides.first { newIcon in it }[newIcon]?.customLabel).isEqualTo("TG")
        assertThat(overrides.overrides.value).doesNotContainKey(oldIcon)
        assertThat(gestures.bindings.first { (it[SwipeDirection.LEFT] as? GestureAction.LaunchApp)?.key == newIcon })
            .isNotEmpty()
        val usage = history.usage.first { oldIcon !in it }
        assertThat(usage[newIcon]?.launchCount).isEqualTo(4)
        assertThat(usage[newIcon]?.lastLaunchedAt).isEqualTo(rule.now)
        val recents = history.recents.first { rows -> rows[env].orEmpty().none { it.key == oldIcon } }[env].orEmpty()
        assertThat(recents.map { it.key }).containsExactly(newIcon)
        val space = history.spaceUsage.first { rows -> rows[env].orEmpty().none { it.key == oldIcon } }[env].orEmpty()
        assertThat(space.map { it.key to it.launchCount }).containsExactly(newIcon to 4)
        assertThat(learning.samples.first { s -> s.none { it.key == oldIcon } })
            .containsExactly(LearnedGesture("tgh", newIcon, 3.0), LearnedGesture("trf", newIcon, 1.0))
    }

    @Test
    fun nothingChangesWhileEveryStoredAppStillExists() = runTest {
        val envs = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val env = envs.environments.first { it.isNotEmpty() }.first { it.type == EnvType.NORMAL }.id
        envs.setItems(env, listOf(maps, oldIcon))
        PackageDataCleaner(rule.db, rule.clock).reconcile(listOf(maps, oldIcon, newIcon))
        assertThat(envs.itemsOf(env)).containsExactly(maps, oldIcon).inOrder()
    }
}
