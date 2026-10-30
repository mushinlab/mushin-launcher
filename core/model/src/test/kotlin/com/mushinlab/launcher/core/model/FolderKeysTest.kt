package com.mushinlab.launcher.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FolderKeysTest {
    @Test
    fun folderKeyRoundTripsItsId() {
        assertThat(FolderKeys.idOf(FolderKeys.of(42))).isEqualTo(42L)
    }

    @Test
    fun appKeysAreNotFolders() {
        assertThat(FolderKeys.idOf(AppKey("com.google.maps", "Main", 0))).isNull()
        assertThat(FolderKeys.idOf(AppKey(FolderKeys.PACKAGE, "7", 0))).isNull()
        assertThat(FolderKeys.idOf(AppKey(FolderKeys.PACKAGE, "Main", FolderKeys.USER_SERIAL))).isNull()
    }
}
