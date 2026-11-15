package com.mushinlab.launcher.feature.search

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppKey
import org.junit.Test

class SearchPanelsTest {
    @Test
    fun openWhileTheAppMenuIsShown() {
        val panels = SearchPanels()
        assertThat(panels.open).isFalse()
        panels.openMenu(SearchMenu(AppKey("p", "A", 0), "App", Offset.Zero, canUninstall = true))
        assertThat(panels.open).isTrue()
        panels.toggleResize()
        panels.closeMenu()
        assertThat(panels.open).isFalse()
    }
}
