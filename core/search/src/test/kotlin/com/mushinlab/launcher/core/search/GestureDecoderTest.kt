package com.mushinlab.launcher.core.search

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.LearnedGesture
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

    private fun decodeTexts(path: List<GesturePoint>, vocabulary: List<GestureWord> = this.vocabulary) =
        decoder.decode(path, vocabulary)?.words?.map { it.text }

    private fun moved(path: List<GesturePoint>, dx: Float, dy: Float, scale: Float = 1f): List<GesturePoint> {
        val cx = path.map { it.x }.average().toFloat()
        val cy = path.map { it.y }.average().toFloat()
        return path.map { GesturePoint(cx + (it.x - cx) * scale + dx, cy + (it.y - cy) * scale + dy) }
    }

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
        assertThat(decodeTexts(path("pol"), both)).containsExactly("pol", "poll").inOrder()
        assertThat(decodeTexts(path("pol") + GesturePoint(l.x, l.y, repeat = true), both)).containsExactly("poll")
    }

    @Test
    fun decodesExactPaths() {
        words.forEach { word -> assertThat(decodeTexts(path(word))).containsExactly(word) }
    }

    @Test
    fun decodesSloppyPaths() {
        assertThat(decodeTexts(path("slack", wobble = 0.3f))).containsExactly("slack")
        assertThat(decodeTexts(path("telegram", wobble = 0.3f))).containsExactly("telegram")
        assertThat(decodeTexts(path("camera", wobble = 0.3f))).containsExactly("camera")
    }

    @Test
    fun recognisesTheShapeDrawnAwayFromTheKeys() {
        assertThat(decodeTexts(moved(path("telegram"), dx = 0.5f, dy = 1f))).containsExactly("telegram")
        assertThat(decodeTexts(moved(path("camera"), dx = 0f, dy = 0f, scale = 0.6f))).containsExactly("camera")
        assertThat(decodeTexts(moved(path("settings", wobble = 0.2f), dx = -0.4f, dy = 0.6f, scale = 1.3f)))
            .containsExactly("settings")
    }

    @Test
    fun tuningControlsShiftForgivenessAndHowManyAlternativesAreShown() {
        val far = moved(path("telegram"), dx = 1f, dy = 1f)
        assertThat(decoder.decode(far, vocabulary)?.words?.map { it.text }).containsExactly("telegram")
        assertThat(GestureDecoder(keys, shiftPenalty = 1.5f).decode(far, vocabulary)?.words).isEmpty()
        val both = listOf(GestureWord("pol", "pol", key(0)), GestureWord("poll", "poll", key(1)))
        assertThat(decoder.decode(path("pol"), both)?.words).hasSize(2)
        assertThat(GestureDecoder(keys, suggestMargin = 0.05f).decode(path("pol"), both)?.words?.map { it.text })
            .containsExactly("pol")
    }

    @Test
    fun drawingInReverseIsNotTheWord() {
        val maps = listOf(GestureWord("maps", "maps", key(0)))
        assertThat(decodeTexts(path("spam"), maps)).isEmpty()
    }

    @Test
    fun tapIsNotAGesture() {
        assertThat(decoder.decode(path("s"), vocabulary)).isNull()
    }

    @Test
    fun shapeFarFromEveryAppWordGivesNoWord() {
        val result = decoder.decode(path("nm"), listOf(GestureWord("qw", "qw", key(0))))
        assertThat(result?.path).isEqualTo("nm")
        assertThat(result?.words).isEmpty()
    }

    @Test
    fun similarDrawingsOfDifferentAppsAreAllSuggestedUntilLearned() {
        val twins = listOf(
            GestureWord("slack", "slack", key(0)),
            GestureWord("slack", "slack", key(1)),
            GestureWord("slak", "slak", key(1)),
            GestureWord("spotify", "spotify", key(2)),
        )
        assertThat(decoder.decode(path("slack"), twins)?.words?.map { it.key }).containsExactly(key(0), key(1))
        val drawn = decoder.keysAlong(path("slack"))
        val learned = listOf(LearnedGesture(drawn, key(1), 1.0))
        assertThat(decoder.decode(path("slack"), twins, learned)?.words?.map { it.key }).containsExactly(key(1))
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

    @Test
    fun namesWithSeveralCapitalsAlsoGiveTheWholeWord() {
        val geforce = AppKey("nvidia", "A", 0)
        val github = AppKey("github", "A", 0)
        val index = SearchIndex.build(
            listOf(SearchItem(geforce, "GeForce NOW"), SearchItem(github, "GitHub")),
        )
        assertThat(index.gestureVocabulary.filter { it.key == geforce }.map { it.latin })
            .containsExactly("geforcenow", "force", "now", "geforce")
        assertThat(index.gestureVocabulary.filter { it.key == github }.map { it.latin })
            .containsExactly("github", "git", "hub")
        val geforcePath = "geforce".map { keys.getValue(it) }
        assertThat(decodeTexts(geforcePath, index.gestureVocabulary)).containsExactly("geforce")
    }
}
