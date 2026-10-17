package com.mushinlab.launcher

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.apps.NotificationSource
import com.mushinlab.launcher.core.data.repo.SpaceError
import com.mushinlab.launcher.core.model.AppUsage
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.LearnedGesture
import com.mushinlab.launcher.core.model.SearchRecent
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.feature.settings.ERR_TAKEN
import com.mushinlab.launcher.feature.settings.ERR_WORK
import org.junit.Test

class LauncherDataTest {
    private val slack = key("slack")
    private val maps = key("maps")
    private val camera = key("camera")
    private val gone = key("gone")
    private val normal = space(1)
    private val work = space(2, SpaceType.WORK)

    private fun data(settings: LauncherSettings = LauncherSettings()) = LauncherData(
        spaces = listOf(normal, work),
        current = normal,
        items = mapOf(normal.id to listOf(slack, gone, maps), work.id to listOf(camera)),
        apps = listOf(slack, maps, camera).associateWith(::entry),
        overrides = mapOf(maps to override(maps, label = "Navigator"), camera to override(camera, hidden = true)),
        settings = settings,
    )

    @Test
    fun homeRowsFollowTheCurrentSpaceAndSkipUninstalledApps() {
        val state = homeState(data(), roleHeld = true, counts = emptyMap(), swipes = emptyMap())

        assertThat(state.rows.map { it.label }).containsExactly("Slack", "Navigator").inOrder()
        assertThat(state.roleMissing).isFalse()
    }

    @Test
    fun notificationCountsAreShownOnlyWhenTheSettingIsOn() {
        val counts = mapOf(NotificationSource(slack.packageName, slack.userSerial) to 3)

        val off = homeState(data(), true, counts, emptyMap())
        val on = homeState(data(LauncherSettings(notificationCounts = true)), true, counts, emptyMap())

        assertThat(off.rows.first().notifications).isEqualTo(0)
        assertThat(on.rows.first().notifications).isEqualTo(3)
    }

    @Test
    fun swipesAreAttachedToTheirRows() {
        val state = homeState(data(), false, emptyMap(), mapOf(slack to listOf(SwipeDirection.LEFT)))

        assertThat(state.rows.first().swipes).containsExactly(SwipeDirection.LEFT)
        assertThat(state.roleMissing).isTrue()
    }

    @Test
    fun snapshotRestoresTheSameRows() {
        val state = homeState(data(), true, emptyMap(), emptyMap())

        val restored = snapshotState(homeSnapshot(data(), state), roleHeld = true)

        assertThat(restored.rows.map { it.key to it.label }).isEqualTo(state.rows.map { it.key to it.label })
    }

    @Test
    fun listOffsetIsAutomaticUntilTheSpaceOrSettingsSaveOne() {
        val auto = homeState(data(), true, emptyMap(), emptyMap())
        val global = homeState(data(LauncherSettings(homeListOffset = 0)), true, emptyMap(), emptyMap())
        val own = homeState(
            data(LauncherSettings(homeListOffset = 0)).copy(current = normal.copy(listOffset = 90)),
            true,
            emptyMap(),
            emptyMap(),
        )

        assertThat(auto.homeListOffset).isNull()
        assertThat(snapshotState(homeSnapshot(data(), auto), roleHeld = true).homeListOffset).isNull()
        assertThat(global.homeListOffset).isEqualTo(0)
        assertThat(own.homeListOffset).isEqualTo(90)
    }

    @Test
    fun noSnapshotWithoutACurrentSpace() {
        val state = homeState(data(), true, emptyMap(), emptyMap())

        assertThat(homeSnapshot(data().copy(current = null), state)).isNull()
    }

    @Test
    fun popupMarksTheCurrentSpace() {
        val spaces = popupSpaces(data())

        assertThat(spaces.map { it.id to it.current }).containsExactly(1L to true, 2L to false).inOrder()
        assertThat(popupSpaces(null)).isEmpty()
    }

    @Test
    fun recentKeysAreThoseOfTheCurrentSpaceInLaunchOrder() {
        val recents = mapOf(
            normal.id to listOf(slack, camera, maps).map { SearchRecent(it, 0, 1) },
            work.id to listOf(SearchRecent(gone, 0, 1)),
        )

        assertThat(recentKeys(data(), recents)).containsExactly(slack, camera, maps).inOrder()
        assertThat(recentKeys(data().copy(current = work), recents)).containsExactly(gone)
        assertThat(recentKeys(null, recents)).isEmpty()
    }

    @Test
    fun searchIndexHasEveryAppButListsOnlyVisibleOnes() {
        val index = searchIndex(data())

        assertThat(index.size).isEqualTo(3)
        assertThat(index.alphabetical.map { it.key }).containsExactly(maps, slack).inOrder()
        assertThat(index.alphabetical.single { it.key == maps }.customLabel).isEqualTo("Navigator")
    }

    @Test
    fun settingsStateMarksMissingAppsAndUnknownGestureTargets() {
        val gestures = mapOf(
            SwipeDirection.LEFT to GestureAction.LaunchApp(gone),
            SwipeDirection.RIGHT to GestureAction.LaunchApp(maps),
        )

        val state = settingsState(data(), gestures, roleHeld = true, capacity = 8, versionName = "1.0")

        assertThat(state.spaceItems.getValue(normal.id).single { it.key == gone }.missing).isTrue()
        assertThat(state.gestures.getValue(SwipeDirection.LEFT).appMissing).isTrue()
        assertThat(state.gestures.getValue(SwipeDirection.RIGHT).appLabel).isEqualTo("Navigator")
        assertThat(state.renamed.map { it.custom }).containsExactly("Navigator")
        assertThat(state.spaces.single { it.current }.id).isEqualTo(normal.id)
    }

    @Test
    fun memoryGroupsDrawingsByAppStrongestFirst() {
        val base = settingsState(data(), emptyMap(), true, 8, "1.0")
        val samples = listOf(LearnedGesture("sl", slack, 1.0), LearnedGesture("slk", slack, 3.0))
        val usage = mapOf(slack to AppUsage(slack, 4, 0, 0.0), maps to AppUsage(maps, 9, 0, 0.0))

        val state = base.withMemory(samples, usage)

        assertThat(state.learned.single().drawings.map { it.path }).containsExactly("slk", "sl").inOrder()
        assertThat(state.usage.map { it.label }).containsExactly("Navigator", "Slack").inOrder()
    }

    @Test
    fun spaceErrorsMapToSettingsCodes() {
        assertThat(SpaceError.NameTaken.code()).isEqualTo(ERR_TAKEN)
        assertThat(SpaceError.WorkLimitReached.code()).isEqualTo(ERR_WORK)
    }
}
