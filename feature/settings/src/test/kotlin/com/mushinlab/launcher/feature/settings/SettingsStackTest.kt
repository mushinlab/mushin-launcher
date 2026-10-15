package com.mushinlab.launcher.feature.settings

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.SwipeDirection
import org.junit.Test

class SettingsStackTest {
    private val routes = listOf(
        SettingsRoute.Root,
        SettingsRoute.Spaces,
        SettingsRoute.SpaceEditor(7),
        SettingsRoute.PickApps(7),
        SettingsRoute.PickGestureApp(SwipeDirection.LEFT),
        SettingsRoute.Memory,
        SettingsRoute.MemoryApp(AppKey("org.telegram", "org.telegram.Main\$Alias", 10), sketch = true),
        SettingsRoute.MemoryPickApp,
    )

    @Test
    fun everyRouteSurvivesEncoding() {
        val decoded = routes.map { SettingsRouteCodec.decode(SettingsRouteCodec.encode(it)) }

        assertThat(decoded).isEqualTo(routes)
    }

    @Test
    fun unknownOrBrokenRoutesAreDropped() {
        assertThat(SettingsRouteCodec.decode("Nowhere")).isNull()
        assertThat(SettingsRouteCodec.decode("SpaceEditor|x")).isNull()
        assertThat(SettingsRouteCodec.decode("MemoryApp|only")).isNull()
    }

    @Test
    fun restoredStackContinuesWhereItWas() {
        val stack = SettingsStack()

        stack.restore(routes.take(3))
        stack.pop()

        assertThat(stack.routes).containsExactly(SettingsRoute.Root, SettingsRoute.Spaces).inOrder()
    }

    @Test
    fun backClosesAnOpenSheetBeforeLeavingThePage() {
        val stack = SettingsStack()
        var sheetClosed = false
        stack.open(SettingsRoute.Style)
        stack.sheet = { sheetClosed = true }

        assertThat(stack.pop()).isTrue()
        assertThat(sheetClosed).isTrue()
        assertThat(stack.top).isEqualTo(SettingsRoute.Style)
    }

    @Test
    fun popOnAnEmptyStackReportsNothingToClose() {
        assertThat(SettingsStack().pop()).isFalse()
    }
}
