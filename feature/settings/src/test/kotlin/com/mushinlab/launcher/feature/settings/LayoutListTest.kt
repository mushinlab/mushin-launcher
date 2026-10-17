package com.mushinlab.launcher.feature.settings

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.LauncherSettings
import org.junit.Test

class LayoutListTest {
    private val settings = LauncherSettings(keyboardLayouts = listOf("en", "ru"), activeKeyboardLayout = "ru")

    @Test
    fun addedLayoutGoesToTheEndOnce() {
        val added = settings.withLayout("de")
        assertThat(added.keyboardLayouts).containsExactly("en", "ru", "de").inOrder()
        assertThat(added.activeKeyboardLayout).isEqualTo("ru")
        assertThat(added.withLayout("de")).isEqualTo(added)
    }

    @Test
    fun removingTheActiveLayoutActivatesTheFirstRemaining() {
        val removed = settings.withLayout("de").withoutLayout("ru")
        assertThat(removed.keyboardLayouts).containsExactly("en", "de").inOrder()
        assertThat(removed.activeKeyboardLayout).isEqualTo("en")
    }

    @Test
    fun removingAnotherLayoutKeepsTheActiveOne() {
        val removed = settings.withoutLayout("en")
        assertThat(removed.keyboardLayouts).containsExactly("ru")
        assertThat(removed.activeKeyboardLayout).isEqualTo("ru")
    }

    @Test
    fun theLastLayoutStays() {
        val single = settings.withoutLayout("en")
        assertThat(single.withoutLayout("ru")).isEqualTo(single)
    }
}
