package com.mushinlab.launcher

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.apps.LaunchResult
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.search.GesturePoint
import com.mushinlab.launcher.feature.search.SearchController
import com.mushinlab.launcher.feature.search.SearchMode
import com.mushinlab.launcher.feature.settings.SettingsRoute
import com.mushinlab.launcher.feature.settings.SettingsStack
import org.junit.Test

class HomeInteractionsTest {
    private val slack = key("slack")
    private val model = FakeLauncherModel(apps = mapOf(slack to entry(slack)))
    private val navigator = LauncherNavigator(SearchController(), SettingsStack(), NoTrace)
    private val effects = mutableListOf<LauncherEffect>()
    private val home = HomeInteractions(model, navigator, AppLaunches(model, navigator), effects::add)
    private val scribble = listOf(GesturePoint(0f, 0f), GesturePoint(500f, 10f), GesturePoint(20f, 300f))

    @Test
    fun swipeToAnAppSlidesHomeAwayThenLaunchesWithoutAnimation() {
        model.gestureBindings = mapOf(SwipeDirection.LEFT to GestureAction.LaunchApp(slack))

        home.onSwipe(SwipeDirection.LEFT, fromShadeZone = false)
        assertThat(home.exit).isEqualTo(SwipeDirection.LEFT)
        assertThat(home.gesturesEnabled).isFalse()

        home.completeSwipeLaunch()
        assertThat(model.launched).containsExactly(slack to false)
    }

    @Test
    fun failedSwipeLaunchBringsHomeBack() {
        model.gestureBindings = mapOf(SwipeDirection.LEFT to GestureAction.LaunchApp(slack))
        model.result = LaunchResult.Failed

        home.onSwipe(SwipeDirection.LEFT, fromShadeZone = false)
        home.completeSwipeLaunch()

        assertThat(home.exit).isNull()
        assertThat(navigator.notice?.text).isEqualTo(R.string.notice_launch_failed)
    }

    @Test
    fun swipeToAnUninstalledAppDoesNothing() {
        model.gestureBindings = mapOf(SwipeDirection.UP to GestureAction.LaunchApp(key("gone")))

        home.onSwipe(SwipeDirection.UP, fromShadeZone = false)

        assertThat(home.exit).isNull()
    }

    @Test
    fun searchSwipesOpenSearchInTheirMode() {
        model.gestureBindings = mapOf(
            SwipeDirection.DOWN to GestureAction.Search,
            SwipeDirection.UP to GestureAction.AllApps,
        )

        home.onSwipe(SwipeDirection.UP, fromShadeZone = false)
        assertThat(navigator.searchMode).isEqualTo(SearchMode.BROWSING)

        home.onSwipe(SwipeDirection.DOWN, fromShadeZone = false)
        assertThat(navigator.searchMode).isEqualTo(SearchMode.TYPING)
        assertThat(navigator.overlay).isEqualTo(Overlay.Search)
    }

    @Test
    fun unavailableShadeIsReportedOnlyOnce() {
        model.shadeAvailable = false

        home.onSwipe(SwipeDirection.DOWN, fromShadeZone = true)
        navigator.notice = null
        home.onSwipe(SwipeDirection.DOWN, fromShadeZone = true)

        assertThat(model.shadeRequests).isEqualTo(2)
        assertThat(navigator.notice).isNull()
    }

    @Test
    fun repeatedUnknownDrawingsOfferLearningWithTheirAverage() {
        repeat(HomeInteractions.LEARN_HINT_MISSES - 1) { home.onDrawing(scribble, width = 1080f, density = 2.6f) }
        assertThat(home.learnHint).isFalse()

        home.onDrawing(scribble, width = 1080f, density = 2.6f)
        assertThat(home.learnHint).isTrue()
        assertThat(effects).containsExactlyElementsIn(
            List(HomeInteractions.LEARN_HINT_MISSES) {
                LauncherEffect.Reject
            },
        )

        home.openLearning()
        assertThat(home.learnHint).isFalse()
        assertThat(navigator.settings.routes).containsExactly(SettingsRoute.Memory, SettingsRoute.MemoryPickApp)
        assertThat(navigator.settings.draft).isNotEmpty()
    }

    @Test
    fun drawingIsIgnoredWhileAnOverlayIsOpen() {
        navigator.openSearch(SearchMode.TYPING)

        home.onDrawing(scribble, width = 1080f, density = 2.6f)

        assertThat(effects).isEmpty()
    }

    @Test
    fun addAppsOpensTheAppPickerOfTheCurrentSpace() {
        navigator.openSearch(SearchMode.TYPING)

        home.addApps()

        assertThat(navigator.overlay).isEqualTo(Overlay.None)
        assertThat(navigator.settings.routes).containsExactly(SettingsRoute.PickApps(1))
    }

    @Test
    fun addAppsWithoutACurrentSpaceDoesNothing() {
        model.currentSpace = null

        home.addApps()

        assertThat(navigator.settings.isOpen).isFalse()
    }

    @Test
    fun stoppingCancelsThePendingSwipeAndClosesOverlays() {
        model.gestureBindings = mapOf(SwipeDirection.LEFT to GestureAction.LaunchApp(slack))
        home.onSwipe(SwipeDirection.LEFT, fromShadeZone = false)
        navigator.openSearch(SearchMode.TYPING)

        home.onStop()
        home.completeSwipeLaunch()

        assertThat(home.exit).isNull()
        assertThat(navigator.overlay).isEqualTo(Overlay.None)
        assertThat(model.launched).isEmpty()
    }
}
