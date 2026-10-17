package com.mushinlab.launcher.feature.search

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppKey
import org.junit.Test

class SearchPanelsTest {
    @Test
    fun openWhileTheAppMenuOrTheOptionsAreShown() {
        val panels = SearchPanels()
        assertThat(panels.open).isFalse()
        panels.openMenu(SearchMenu(AppKey("p", "A", 0), "App", Offset.Zero, inSpace = false, canUninstall = true))
        assertThat(panels.open).isTrue()
        panels.closeMenu()
        assertThat(panels.open).isFalse()
        panels.openOptions(Offset.Zero)
        assertThat(panels.open).isTrue()
        panels.toggleResize()
        panels.closeOptions()
        assertThat(panels.open).isFalse()
    }
}
