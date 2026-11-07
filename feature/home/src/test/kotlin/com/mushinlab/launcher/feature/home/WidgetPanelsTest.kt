package com.mushinlab.launcher.feature.home

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.WidgetPanelSize
import org.junit.Test

class WidgetPanelsTest {
    @Test
    fun AC_38_1_emptyPanelsUseZeroOfTheirPreferredHeight() {
        val empty = HomeWidgetPanel(size = WidgetPanelSize.LARGE)
        assertThat(empty.hasContent(showClock = true)).isFalse()
        assertThat(HomeWidgetPanel(clock = true).hasContent(showClock = false)).isFalse()
        assertThat(panelHeights(0f, 0f, 400f)).isEqualTo(0f to 0f)
    }

    @Test
    fun AC_38_2_largePanelsShareLimitedSpaceWithoutEliminatingTheAppList() {
        val heights = panelHeights(280f, 280f, 300f)
        assertThat(heights.first).isEqualTo(150f)
        assertThat(heights.second).isEqualTo(150f)
        assertThat(panelHeights(200f, 0f, 400f)).isEqualTo(200f to 0f)
        assertThat(panelHeights(200f, 120f, -1f)).isEqualTo(0f to 0f)
    }
}
