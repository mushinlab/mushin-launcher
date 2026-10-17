package com.mushinlab.launcher.feature.search

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.search.GestureDecoder
import com.mushinlab.launcher.core.search.KeyboardLayout
import com.mushinlab.launcher.core.search.KeyboardLayouts
import com.mushinlab.launcher.core.search.SearchIndex
import com.mushinlab.launcher.core.search.SearchItem
import org.junit.Test

class LayoutGlideTest {
    private fun centers(layout: KeyboardLayout) = glideCenters(WIDTH, ROW, PADDING, GAP, layout)

    private fun keyCenter(layout: KeyboardLayout, char: Char) = KeyboardGeometry.compute(
        layout,
        WIDTH,
        ROW,
        PADDING,
        GAP,
    )
        .single { it.spec.char == char }
        .rect
        .center

    @Test
    fun latinLayoutsDecodeOnTheirOwnKeys() {
        listOf(KeyboardLayouts.ES, KeyboardLayouts.PT, KeyboardLayouts.DE, KeyboardLayouts.FR).forEach { layout ->
            val glide = centers(layout)
            assertThat(glide.keys).containsExactlyElementsIn(('a'..'z').toList())
            ('a'..'z').forEach { c ->
                val key = keyCenter(layout, c)
                assertThat(glide.getValue(c).x).isWithin(TOLERANCE).of(key.x)
                assertThat(glide.getValue(c).y).isWithin(TOLERANCE).of(key.y)
            }
        }
    }

    @Test
    fun russianAndIndonesianDecodeOnQwerty() {
        assertThat(centers(KeyboardLayouts.RU)).isEqualTo(centers(KeyboardLayouts.EN))
        assertThat(centers(KeyboardLayouts.ID)).isEqualTo(centers(KeyboardLayouts.EN))
    }

    @Test
    fun drawingOverTheVisibleKeysOfEachLayoutFindsTheApp() {
        val maps = AppKey("maps", "Main", 0)
        val zoom = AppKey("zoom", "Main", 0)
        val street = AppKey("strasse", "Main", 0)
        val index = SearchIndex.build(
            listOf(SearchItem(maps, "Maps"), SearchItem(zoom, "Zoom"), SearchItem(street, "Straße")),
        )
        KeyboardLayouts.all.filter { it.hasLatinLetters }.forEach { layout ->
            val glide = centers(layout)
            val decoder = GestureDecoder(glide)
            listOf("maps" to maps, "zoom" to zoom, "strasse" to street).forEach { (word, key) ->
                val path = word.map { glide.getValue(it) }
                val result = decoder.decode(path, index.gestureVocabulary)
                assertThat(result?.words?.firstOrNull()?.key).isEqualTo(key)
            }
        }
    }

    private companion object {
        const val WIDTH = 1080f
        const val ROW = 136f
        const val PADDING = 10f
        const val GAP = 10f
        const val TOLERANCE = 0.01f
    }
}
