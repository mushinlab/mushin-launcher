package com.mushinlab.launcher

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.feature.search.SearchController
import com.mushinlab.launcher.feature.search.SearchMode
import com.mushinlab.launcher.feature.settings.SettingsRoute
import com.mushinlab.launcher.feature.settings.SettingsStack
import org.junit.Test

class LauncherNavigatorTest {
    private val navigator = LauncherNavigator(SearchController(), SettingsStack(), NoTrace)

    @Test
    fun openingSearchResetsItAndTypesTheFirstCharacter() {
        navigator.openSearch(SearchMode.BROWSING, 's')

        assertThat(navigator.overlay).isEqualTo(Overlay.Search)
        assertThat(navigator.searchMode).isEqualTo(SearchMode.BROWSING)
        assertThat(navigator.search.query).isEqualTo("s")
    }

    @Test
    fun backClosesTheTopmostLayerFirst() {
        navigator.openSettings(SettingsRoute.Keyboard)
        navigator.overlay = Overlay.Popup(Offset.Zero, null)
        navigator.renameTarget = key("slack")

        navigator.handleBack()
        assertThat(navigator.renameTarget).isNull()
        assertThat(navigator.overlay).isInstanceOf(Overlay.Popup::class.java)

        navigator.handleBack()
        assertThat(navigator.overlay).isEqualTo(Overlay.None)
        assertThat(navigator.settings.isOpen).isTrue()

        navigator.handleBack()
        assertThat(navigator.settings.isOpen).isFalse()
    }

    @Test
    fun backFromSearchClosesSearchBeforeSettings() {
        navigator.openSettings()
        navigator.openSearch(SearchMode.TYPING)

        navigator.handleBack()

        assertThat(navigator.overlay).isEqualTo(Overlay.None)
        assertThat(navigator.settings.isOpen).isTrue()
    }

    @Test
    fun editButtonTogglesHomeEditing() {
        navigator.toggleHomeEditing()
        assertThat(navigator.homeEditing).isTrue()

        navigator.toggleHomeEditing()
        assertThat(navigator.homeEditing).isFalse()
    }

    @Test
    fun backClosesAnEditSheetBeforeLeavingHomeEditing() {
        navigator.toggleHomeEditing()
        navigator.openSettings(SettingsRoute.TextSheet)

        navigator.handleBack()
        assertThat(navigator.settings.isOpen).isFalse()
        assertThat(navigator.homeEditing).isTrue()

        navigator.handleBack()
        assertThat(navigator.homeEditing).isFalse()
    }

    @Test
    fun homeIntentLeavesHomeEditing() {
        navigator.toggleHomeEditing()

        navigator.onHomeIntent(assist = false)

        assertThat(navigator.homeEditing).isFalse()
    }

    @Test
    fun homeIntentClosesEverythingAndAssistOpensSearch() {
        navigator.openSettings()
        navigator.renameTarget = key("slack")

        navigator.onHomeIntent(assist = false)
        assertThat(navigator.settings.isOpen).isFalse()
        assertThat(navigator.renameTarget).isNull()
        assertThat(navigator.overlay).isEqualTo(Overlay.None)

        navigator.onHomeIntent(assist = true)
        assertThat(navigator.overlay).isEqualTo(Overlay.Search)
        assertThat(navigator.searchMode).isEqualTo(SearchMode.TYPING)
    }

    @Test
    fun hardwareInputIsIgnoredWhileSettingsOrRenameAreOpen() {
        assertThat(navigator.acceptsInput).isTrue()

        navigator.openSettings()
        assertThat(navigator.acceptsInput).isFalse()

        navigator.closeAll()
        navigator.renameTarget = key("slack")
        assertThat(navigator.acceptsInput).isFalse()
    }
}
