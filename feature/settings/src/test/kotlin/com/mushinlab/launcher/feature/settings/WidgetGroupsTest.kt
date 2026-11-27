package com.mushinlab.launcher.feature.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WidgetGroupsTest {
    @Test
    fun AC_55_5_widgetsAreGroupedByAppWithTheClockFirst() {
        val widgets = listOf(
            WidgetLabel(0, "Weather", app = "Weather"),
            WidgetLabel(1, "Day", app = "calendar"),
            WidgetLabel(2, "", app = "Mushin Launcher", clock = true),
            WidgetLabel(3, "Forecast", app = "Weather"),
        )
        assertThat(widgetGroups(widgets).map { group -> group.map { it.id } })
            .containsExactly(listOf(2), listOf(1), listOf(0, 3))
            .inOrder()
    }
}
