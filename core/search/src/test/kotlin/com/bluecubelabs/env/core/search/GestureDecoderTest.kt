package com.bluecubelabs.env.core.search

import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.LearnedGesture
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GestureDecoderTest {
    private val keys: Map<Char, GesturePoint> = buildMap {
        listOf("qwertyuiop" to 0f, "asdfghjkl" to 0.5f, "zxcvbnm" to 1.5f).forEachIndexed { row, (letters, shift) ->
            letters.forEachIndexed { i, c -> put(c, GesturePoint(i + shift, row.toFloat())) }
        }
    }
    private val decoder = GestureDecoder(keys)
    private val words = listOf("slack", "spotify", "settings", "chrome", "camera", "telegram", "maps")
    private val vocabulary = words.mapIndexed { i, word -> GestureWord(word, word, key(i)) }

    private fun key(i: Int) = AppKey("p$i", "A", 0)

    private fun path(word: String, wobble: Float = 0f): List<GesturePoint> = word.mapIndexed { i, c ->
        val p = keys.getValue(c)
        GesturePoint(p.x + if (i % 2 == 0) wobble else -wobble, p.y + wobble)
    }

    private fun decodeText(path: List<GesturePoint>, vocabulary: List<GestureWord> = this.vocabulary) =
        decoder.decode(path, vocabulary)?.word?.text

    @Test
    fun keysAlongVisitsEveryKeyBetweenPoints() {
        assertThat(decoder.keysAlong(path("sl"))).isEqualTo("sdfghjkl")
        assertThat(decoder.keysAlong(path("aa"))).isEqualTo("a")
    }

    @Test
    fun dwellRepeatsTheLetter() {
        val l = keys.getValue('l')
        assertThat(decoder.keysAlong(path("pol") + GesturePoint(l.x, l.y, repeat = true))).isEqualTo("poll")
        val both = listOf(GestureWord("pol", "pol", key(0)), GestureWord("poll", "poll", key(1)))
        assertThat(decodeText(path("pol"), both)).isEqualTo("pol")
        assertThat(decodeText(path("pol") + GesturePoint(l.x, l.y, repeat = true), both)).isEqualTo("poll")
    }

    @Test
    fun decodesExactPaths() {
        words.forEach { word -> assertThat(decodeText(path(word))).isEqualTo(word) }
    }

    @Test
    fun decodesSloppyPaths() {
        assertThat(decodeText(path("slack", wobble = 0.3f))).isEqualTo("slack")
        assertThat(decodeText(path("telegram", wobble = 0.3f))).isEqualTo("telegram")
        assertThat(decodeText(path("camera", wobble = 0.3f))).isEqualTo("camera")
    }

    @Test
    fun tapIsNotAGesture() {
        assertThat(decoder.decode(path("s"), vocabulary)).isNull()
    }

    @Test
    fun shapeFarFromEveryAppWordGivesNoWord() {
        val result = decoder.decode(path("nm"), listOf(GestureWord("qw", "qw", key(0))))
        assertThat(result?.path).isEqualTo("nm")
        assertThat(result?.word).isNull()
    }

    @Test
    fun learnedSampleTipsTheChoiceToItsApp() {
        val twins = listOf(GestureWord("slack", "slack", key(0)), GestureWord("slack", "slack", key(1)))
        assertThat(decoder.decode(path("slack"), twins)?.word?.key).isEqualTo(key(0))
        val drawn = decoder.keysAlong(path("slack"))
        val learned = listOf(LearnedGesture(drawn, key(1), 1.0))
        assertThat(decoder.decode(path("slack"), twins, learned)?.word?.key).isEqualTo(key(1))
    }

    @Test
    fun vocabularyIsMadeOfAppNamesAndSkipsShortWordsAndHiddenApps() {
        val maps = AppKey("a", "A", 0)
        val slack = AppKey("b", "B", 0)
        val files = AppKey("c", "C", 0)
        val index = SearchIndex.build(
            listOf(
                SearchItem(maps, "Google Maps"),
                SearchItem(slack, "Слак"),
                SearchItem(files, "Files by Google"),
                SearchItem(AppKey("d", "D", 0), "Secret", hidden = true),
            ),
        )
        assertThat(index.gestureVocabulary).containsExactly(
            GestureWord("googlemaps", "google maps", maps),
            GestureWord("google", "google", maps),
            GestureWord("maps", "maps", maps),
            GestureWord("slak", "слак", slack),
            GestureWord("filesbygoogle", "files by google", files),
            GestureWord("files", "files", files),
            GestureWord("google", "google", files),
        )
    }
}
