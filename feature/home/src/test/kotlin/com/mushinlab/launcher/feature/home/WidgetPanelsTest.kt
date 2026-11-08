package com.mushinlab.launcher.feature.home

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.WidgetArrangement
import com.mushinlab.launcher.core.model.WidgetPanelMode
import org.junit.Test

class WidgetPanelsTest {
    @Test
    fun AC_39_2_standardColumnAndExpandedGrid() {
        assertThat(HomeWidgetPanel(arrangement = WidgetArrangement.COLUMN).columns).isEqualTo(1)
        assertThat(HomeWidgetPanel().columns).isEqualTo(2)
        assertThat(HomeWidgetPanel(WidgetPanelMode.EXPANDED, WidgetArrangement.COLUMN).columns).isEqualTo(2)
    }

    @Test
    fun AC_39_3_dropIntoNeighbouringCellAndClampOutsidePanel() {
        assertThat(widgetDropIndex(0, 1f, 0f, 2, 2)).isEqualTo(1)
        assertThat(widgetDropIndex(0, 0f, 1f, 1, 2)).isEqualTo(1)
        assertThat(widgetDropIndex(0, 1f, 1f, 2, 4)).isEqualTo(3)
        assertThat(widgetDropIndex(3, -1f, -1f, 2, 4)).isEqualTo(0)
        assertThat(widgetDropIndex(0, -100f, -100f, 2, 4)).isEqualTo(0)
        assertThat(widgetDropIndex(0, 100f, 100f, 2, 3)).isEqualTo(2)
    }
}
