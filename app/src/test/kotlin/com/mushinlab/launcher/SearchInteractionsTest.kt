package com.mushinlab.launcher

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.apps.LaunchResult
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.search.SearchIndex
import com.mushinlab.launcher.core.search.SearchItem
import com.mushinlab.launcher.feature.search.HardwareKey
import com.mushinlab.launcher.feature.search.SearchController
import com.mushinlab.launcher.feature.search.SearchMode
import com.mushinlab.launcher.feature.settings.SettingsStack
import org.junit.Test

class SearchInteractionsTest {
    private val slack = key("slack")
    private val camera = key("camera")
    private val model = FakeLauncherModel(apps = listOf(slack, camera).associateWith { entry(it, it == camera) })
    private val navigator = LauncherNavigator(SearchController(), SettingsStack(), NoTrace)
    private val effects = mutableListOf<LauncherEffect>()
    private val launches = AppLaunches(model, navigator)
    private val search = SearchInteractions(model, navigator, launches, effects::add)

    private fun openSearch() {
        navigator.search.sources.update(
            SearchIndex.build(listOf(SearchItem(slack, "Slack"), SearchItem(camera, "Camera"))),
            emptyMap(),
        )
        navigator.openSearch(SearchMode.TYPING)
    }

    @Test
    fun menuDescribesTheApp() {
        model.items += slack

        search.openMenu(camera, "Camera", Offset.Zero)

        val menu = navigator.search.panels.menu
        assertThat(menu?.label).isEqualTo("Camera")
        assertThat(menu?.canUninstall).isFalse()
    }

    @Test
    fun hidingUsesTheMenuLabel() {
        search.openMenu(slack, "Slack", Offset.Zero)

        search.toggleHidden(slack, hidden = true)

        assertThat(model.hidden).containsExactly(slack, true)
        assertThat(navigator.notice?.arg).isEqualTo("Slack")
        assertThat(navigator.search.panels.menu).isNull()
    }

    @Test
    fun layoutSwitchCyclesThroughEnabledLayouts() {
        model.settings =
            LauncherSettings(keyboardLayouts = listOf("en", "ru"), activeKeyboardLayout = "ru")

        search.switchLayout()

        assertThat(model.selectedLayout).isEqualTo("en")
    }

    @Test
    fun typingOnHomeOpensSearchWithThatLetter() {
        assertThat(search.accepts(HardwareKey.Backspace)).isFalse()
        assertThat(search.accepts(HardwareKey.Text('s'))).isTrue()

        search.onHardwareKey(HardwareKey.Text('s'))

        assertThat(navigator.overlay).isEqualTo(Overlay.Search)
        assertThat(navigator.searchMode).isEqualTo(SearchMode.BROWSING)
        assertThat(navigator.search.query).isEqualTo("s")
    }

    @Test
    fun enterLaunchesTheTargetAndClosesSearch() {
        openSearch()
        search.onHardwareKey(HardwareKey.Text('s'))

        search.onHardwareKey(HardwareKey.Enter)

        assertThat(model.launched).containsExactly(slack to true)
        assertThat(navigator.overlay).isEqualTo(Overlay.None)
    }

    @Test
    fun enterWithoutResultsRejects() {
        openSearch()
        search.onHardwareKey(HardwareKey.Text('x'))
        search.onHardwareKey(HardwareKey.Text('q'))

        search.onHardwareKey(HardwareKey.Enter)

        assertThat(effects).contains(LauncherEffect.Reject)
        assertThat(model.launched).isEmpty()
    }

    @Test
    fun failedLaunchOfAHomeAppOffersToRemoveIt() {
        model.items += slack
        model.result = LaunchResult.Failed

        launches.launch(slack, fromSearch = false)
        navigator.notice?.action?.invoke()

        assertThat(navigator.notice?.actionLabel).isEqualTo(R.string.notice_remove_from_space)
        assertThat(model.items).isEmpty()
    }

    @Test
    fun suspendedAppIsReported() {
        model.result = LaunchResult.Suspended

        launches.launch(slack, fromSearch = true)

        assertThat(navigator.notice?.text).isEqualTo(R.string.notice_suspended)
    }

    @Test
    fun keysAreIgnoredWhileOnboardingIsShown() {
        model.settings = LauncherSettings(onboardingDone = false)

        assertThat(search.accepts(HardwareKey.Text('s'))).isFalse()
    }
}
