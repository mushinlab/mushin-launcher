package com.mushinlab.launcher.feature.settings

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.SwipeDirection
import org.junit.Test

class SettingsStackTest {
    private val routes = listOf(
        SettingsRoute.Root,
        SettingsRoute.SpaceEditor(7),
        SettingsRoute.PickApps(7),
        SettingsRoute.BackgroundEditor(7),
        SettingsRoute.BackgroundPicker(7),
        SettingsRoute.BackgroundFolder(7, PresetFolder.JAPANESE),
        SettingsRoute.PickGestureApp(SwipeDirection.LEFT),
        SettingsRoute.ClockSheet,
        SettingsRoute.TextSheet,
        SettingsRoute.ThemeSheet,
        SettingsRoute.Memory,
        SettingsRoute.MemoryApp(AppKey("org.telegram", "org.telegram.Main\$Alias", 10), sketch = true),
        SettingsRoute.MemoryPickApp,
        SettingsRoute.Tips,
        SettingsRoute.Tip("memory"),
    )

    @Test
    fun everyRouteSurvivesEncoding() {
        val decoded = routes.map { SettingsRouteCodec.decode(SettingsRouteCodec.encode(it)) }

        assertThat(decoded).isEqualTo(routes)
    }

    @Test
    fun unknownOrBrokenRoutesAreDropped() {
        assertThat(SettingsRouteCodec.decode("Nowhere")).isNull()
        assertThat(SettingsRouteCodec.decode("Spaces")).isNull()
        assertThat(SettingsRouteCodec.decode("SpaceEditor|x")).isNull()
        assertThat(SettingsRouteCodec.decode("BackgroundEditor")).isNull()
        assertThat(SettingsRouteCodec.decode("BackgroundEditor|")).isNull()
        assertThat(SettingsRouteCodec.decode("MemoryApp|only")).isNull()
        assertThat(SettingsRouteCodec.decode("Tip|nowhere")).isNull()
        assertThat(SettingsRouteCodec.decode("Tip")).isNull()
    }

    @Test
    fun restoredStackContinuesWhereItWas() {
        val stack = SettingsStack()

        stack.restore(routes.take(3))
        stack.pop()

        assertThat(stack.routes).containsExactly(SettingsRoute.Root, SettingsRoute.SpaceEditor(7)).inOrder()
    }

    @Test
    fun backClosesAnOpenSheetBeforeLeavingThePage() {
        val stack = SettingsStack()
        var sheetClosed = false
        stack.open(SettingsRoute.Keyboard)
        stack.sheet = { sheetClosed = true }

        assertThat(stack.pop()).isTrue()
        assertThat(sheetClosed).isTrue()
        assertThat(stack.top).isEqualTo(SettingsRoute.Keyboard)
    }

    @Test
    fun popOnAnEmptyStackReportsNothingToClose() {
        assertThat(SettingsStack().pop()).isFalse()
    }
}
