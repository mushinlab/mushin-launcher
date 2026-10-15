package com.mushinlab.launcher.feature.search

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

@Stable
class SearchPanels {
    var menu by mutableStateOf<SearchMenu?>(null)
        private set
    var options by mutableStateOf<Offset?>(null)
        private set
    var resizing by mutableStateOf(false)
        private set

    fun openMenu(menu: SearchMenu) {
        this.menu = menu
    }

    fun closeMenu() {
        menu = null
    }

    fun openOptions(anchor: Offset) {
        options = anchor
    }

    fun closeOptions() {
        options = null
    }

    fun toggleResize() {
        resizing = !resizing
    }

    internal fun reset() {
        menu = null
        options = null
        resizing = false
    }
}
