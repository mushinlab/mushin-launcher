package com.mushinlab.launcher

import android.os.Trace
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.feature.search.SearchController
import com.mushinlab.launcher.feature.search.SearchMode
import com.mushinlab.launcher.feature.settings.SettingsRoute
import com.mushinlab.launcher.feature.settings.SettingsStack

@Stable
class LauncherNavigator(
    val search: SearchController = SearchController(),
    val settings: SettingsStack = SettingsStack(),
    private val trace: SearchTrace = SearchTrace.System,
) {
    var overlay by mutableStateOf<Overlay>(Overlay.None)
    var searchMode by mutableStateOf(SearchMode.TYPING)
    var renameTarget by mutableStateOf<AppKey?>(null)
    var notice by mutableStateOf<Notice?>(null)
    var homeEditing by mutableStateOf(false)
        private set
    private var tracePending = false

    val acceptsInput: Boolean get() = !settings.isOpen && renameTarget == null

    fun openSearch(mode: SearchMode, firstChar: Char? = null) {
        trace.begin()
        tracePending = true
        search.reset()
        firstChar?.let { search.type(it) }
        searchMode = mode
        overlay = Overlay.Search
    }

    fun onSearchDrawn() {
        if (tracePending) {
            tracePending = false
            trace.end()
        }
    }

    fun closeOverlays() {
        overlay = Overlay.None
        search.reset()
    }

    fun closeAll() {
        closeOverlays()
        settings.close()
        renameTarget = null
        homeEditing = false
    }

    fun toggleHomeEditing() {
        homeEditing = !homeEditing
    }

    fun openSettings(route: SettingsRoute = SettingsRoute.Root) {
        overlay = Overlay.None
        settings.open(route)
    }

    fun onHomeIntent(assist: Boolean) {
        closeAll()
        if (assist) openSearch(SearchMode.TYPING)
    }

    fun handleBack() {
        when {
            renameTarget != null -> renameTarget = null
            overlay is Overlay.Popup || overlay is Overlay.Suggestion -> overlay = Overlay.None
            search.panels.menu != null -> search.panels.closeMenu()
            search.panels.options != null -> search.panels.closeOptions()
            search.panels.resizing -> search.panels.toggleResize()
            overlay == Overlay.Search -> closeOverlays()
            settings.isOpen -> settings.pop()
            homeEditing -> homeEditing = false
        }
    }
}

interface SearchTrace {
    fun begin()

    fun end()

    object System : SearchTrace {
        const val NAME = "search_open"
        private const val COOKIE = 7

        override fun begin() = Trace.beginAsyncSection(NAME, COOKIE)

        override fun end() = Trace.endAsyncSection(NAME, COOKIE)
    }
}
