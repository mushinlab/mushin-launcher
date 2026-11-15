package com.mushinlab.launcher.feature.search

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class SearchPanels {
    var menu by mutableStateOf<SearchMenu?>(null)
        private set
    var resizing by mutableStateOf(false)
        private set
    val open: Boolean get() = menu != null

    fun openMenu(menu: SearchMenu) {
        this.menu = menu
    }

    fun closeMenu() {
        menu = null
    }

    fun toggleResize() {
        resizing = !resizing
    }

    internal fun reset() {
        menu = null
        resizing = false
    }
}
