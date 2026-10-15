package com.mushinlab.launcher.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KeyRemapTest {
    private val oldIcon = AppKey("org.telegram.messenger", "org.telegram.messenger.DefaultIcon", 0)
    private val newIcon = AppKey("org.telegram.messenger", "org.telegram.messenger.VintageIcon", 0)

    @Test
    fun missingActivityMovesToTheOnlyActivityLeftInItsPackage() {
        assertThat(KeyRemap.moves(listOf(oldIcon, oldIcon), listOf(newIcon))).containsExactly(oldIcon, newIcon)
    }

    @Test
    fun presentKeysAndAbsentPackagesStayAsTheyAre() {
        val maps = AppKey("com.google.maps", "Main", 0)
        assertThat(KeyRemap.moves(listOf(maps), listOf(maps))).isEmpty()
        assertThat(KeyRemap.moves(listOf(oldIcon), listOf(maps))).isEmpty()
    }

    @Test
    fun severalActivitiesLeftIsAmbiguous() {
        val other = AppKey("org.telegram.messenger", "org.telegram.messenger.Other", 0)
        assertThat(KeyRemap.moves(listOf(oldIcon), listOf(newIcon, other))).isEmpty()
    }

    @Test
    fun workProfileIsNotMixedWithMainProfile() {
        val work = newIcon.copy(userSerial = 10)
        assertThat(KeyRemap.moves(listOf(oldIcon), listOf(work))).isEmpty()
        assertThat(KeyRemap.moves(listOf(oldIcon.copy(userSerial = 10)), listOf(work, newIcon)))
            .containsExactly(oldIcon.copy(userSerial = 10), work)
    }
}
