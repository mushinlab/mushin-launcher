package com.bluecubelabs.env.core.search

import com.bluecubelabs.env.core.model.AppKey
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GestureDecoderTest {
    private val keys: Map<Char, GesturePoint> = buildMap {
        listOf("qwertyuiop" to 0f, "asdfghjkl" to 0.5f, "zxcvbnm" to 1.5f).forEachIndexed { row, (letters, shift) ->
            letters.forEachIndexed { i, c -> put(c, GesturePoint(i + shift, row.toFloat())) }
        }
    }
    private val decoder = GestureDecoder(keys)
    private val vocabulary = listOf("slack", "spotify", "settings", "chrome", "camera", "telegram", "maps")

    private fun path(word: String, wobble: Float = 0f): List<GesturePoint> = word.mapIndexed { i, c ->
        val p = keys.getValue(c)
        GesturePoint(p.x + if (i % 2 == 0) wobble else -wobble, p.y + wobble)
    }

    @Test
    fun keysAlongVisitsEveryKeyBetweenPoints() {
        assertThat(decoder.keysAlong(path("sl"))).isEqualTo("sdfghjkl")
        assertThat(decoder.keysAlong(path("aa"))).isEqualTo("a")
    }

    @Test
    fun decodesExactPaths() {
        vocabulary.forEach { word -> assertThat(decoder.decode(path(word), vocabulary)).isEqualTo(word) }
    }

    @Test
    fun decodesSloppyPaths() {
        assertThat(decoder.decode(path("slack", wobble = 0.3f), vocabulary)).isEqualTo("slack")
        assertThat(decoder.decode(path("telegram", wobble = 0.3f), vocabulary)).isEqualTo("telegram")
    }

    @Test
    fun tapIsNotAWord() {
        assertThat(decoder.decode(path("s"), vocabulary)).isNull()
    }

    @Test
    fun vocabularyHasLatinWordsAndWholeNamesWithoutHiddenApps() {
        val index = SearchIndex.build(
            listOf(
                SearchItem(AppKey("a", "A", 0), "Google Maps"),
                SearchItem(AppKey("b", "B", 0), "Слак"),
                SearchItem(AppKey("c", "C", 0), "Secret", hidden = true),
            ),
        )
        assertThat(index.vocabulary).containsExactly("googlemaps", "google", "maps", "slak")
    }
}
