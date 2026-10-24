package com.mushinlab.launcher.feature.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackgroundPresetsTest {
    @Test
    fun plainFolderHasEightDistinctOpaqueColours() {
        val colours = BackgroundPresets.of(PresetFolder.PLAIN).map { (it as BackgroundPreset.Plain).argb }

        assertThat(colours).hasSize(8)
        assertThat(colours.toSet()).hasSize(8)
        assertThat(colours.all { it ushr 24 == 0xFF }).isTrue()
    }

    @Test
    fun japaneseFolderHasTheTenPicturesWithThumbnails() {
        val pictures = BackgroundPresets.of(PresetFolder.JAPANESE).map { it as BackgroundPreset.Picture }

        assertThat(pictures).hasSize(10)
        assertThat(pictures.map { it.image }.toSet()).hasSize(10)
        assertThat(pictures.none { it.image == it.thumbnail }).isTrue()
    }

    @Test
    fun cosmosFolderIsEmptyForNow() {
        assertThat(BackgroundPresets.of(PresetFolder.COSMOS)).isEmpty()
    }
}
