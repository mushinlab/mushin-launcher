package com.mushinlab.launcher.core.search

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.KeyboardLanguages
import org.junit.Test

class KeyboardLayoutsTest {
    private fun letters(layout: KeyboardLayout, row: Int) = layout.rows[row].mapNotNull { it.char }.joinToString("")

    private fun app(label: String) = SearchItem(AppKey("pkg.$label", "Main", 0), label)

    private fun typeAndSearch(layout: KeyboardLayout, typed: String, labels: List<String>): List<String> {
        val keys = layout.rows.flatten().mapNotNull { it.char }.toSet()
        assertThat(keys).containsAtLeastElementsIn(typed.toSet())
        return SearchEngine().search(SearchIndex.build(labels.map(::app)), typed).map { it.label }
    }

    @Test
    fun layoutsAreTheSupportedLanguagesWithTwoLetterLabels() {
        assertThat(KeyboardLayouts.all.map { it.id }).containsExactlyElementsIn(KeyboardLanguages.SUPPORTED).inOrder()
        assertThat(KeyboardLayouts.all.map { it.label })
            .containsExactly("EN", "RU", "ES", "PT", "DE", "FR", "ID")
            .inOrder()
        assertThat(KeyboardLayouts.byId("de")).isSameInstanceAs(KeyboardLayouts.DE)
        assertThat(KeyboardLayouts.byId("xx")).isNull()
    }

    @Test
    fun nativeLayoutsHaveTheirOwnLetters() {
        assertThat(letters(KeyboardLayouts.ES, 1)).isEqualTo("asdfghjklñ")
        assertThat(letters(KeyboardLayouts.PT, 1)).isEqualTo("asdfghjklç")
        assertThat(letters(KeyboardLayouts.DE, 0)).isEqualTo("qwertzuiopü")
        assertThat(letters(KeyboardLayouts.DE, 1)).isEqualTo("asdfghjklöä")
        assertThat(letters(KeyboardLayouts.DE, 2)).isEqualTo("yxcvbnm")
        assertThat(letters(KeyboardLayouts.FR, 0)).isEqualTo("azertyuiop")
        assertThat(letters(KeyboardLayouts.FR, 1)).isEqualTo("qsdfghjklm")
        assertThat(letters(KeyboardLayouts.FR, 2)).isEqualTo("wxcvbn")
        assertThat(KeyboardLayouts.ID.rows).isEqualTo(KeyboardLayouts.EN.rows)
        assertThat(KeyboardLayouts.ID.rowInsets).isEqualTo(KeyboardLayouts.EN.rowInsets)
    }

    @Test
    fun letterKeysOfARowShareOneWidthWhereTheRowsAreEquallyLong() {
        listOf(KeyboardLayouts.ES, KeyboardLayouts.PT, KeyboardLayouts.DE, KeyboardLayouts.FR).forEach { layout ->
            val widths = (0 until KeyboardLayouts.LETTER_ROWS).map { row ->
                val keys = layout.rows[row]
                (1 - 2 * layout.rowInsets[row]) / keys.sumOf { it.weight.toDouble() }.toFloat()
            }
            widths.forEach { assertThat(it).isWithin(TOLERANCE).of(widths.first()) }
        }
    }

    @Test
    fun everyLayoutButRussianHasAllLatinLetters() {
        assertThat(KeyboardLayouts.all.filterNot { it.hasLatinLetters }).containsExactly(KeyboardLayouts.RU)
    }

    @Test
    fun spanishTypingFindsNamesWithAndWithoutTilde() {
        val labels = listOf("España TV", "Espana Radio", "Slack")
        assertThat(typeAndSearch(KeyboardLayouts.ES, "españa", labels)).containsExactly("España TV", "Espana Radio")
        assertThat(typeAndSearch(KeyboardLayouts.ES, "espana", labels)).containsExactly("España TV", "Espana Radio")
    }

    @Test
    fun portugueseTypingFindsNamesWithAndWithoutCedilla() {
        val labels = listOf("Ação", "Acao Bank", "Maps")
        assertThat(typeAndSearch(KeyboardLayouts.PT, "aç", labels)).containsExactly("Ação", "Acao Bank")
        assertThat(typeAndSearch(KeyboardLayouts.PT, "acao", labels)).containsExactly("Ação", "Acao Bank")
    }

    @Test
    fun germanTypingFindsUmlautsAndSharpS() {
        val labels = listOf("Über", "Uber Eats", "Straße", "Ärzte", "Maps")
        assertThat(typeAndSearch(KeyboardLayouts.DE, "über", labels)).containsExactly("Über", "Uber Eats")
        assertThat(typeAndSearch(KeyboardLayouts.DE, "uber", labels)).containsExactly("Über", "Uber Eats")
        assertThat(typeAndSearch(KeyboardLayouts.DE, "strasse", labels).first()).isEqualTo("Straße")
        assertThat(typeAndSearch(KeyboardLayouts.DE, "ärz", labels).first()).isEqualTo("Ärzte")
    }

    @Test
    fun frenchTypingFindsAccentedNames() {
        val labels = listOf("Météo", "Café", "Slack")
        assertThat(typeAndSearch(KeyboardLayouts.FR, "meteo", labels).first()).isEqualTo("Météo")
        assertThat(typeAndSearch(KeyboardLayouts.FR, "cafe", labels).first()).isEqualTo("Café")
    }

    @Test
    fun indonesianTypingFindsLatinNames() {
        val labels = listOf("Gojek", "Tokopedia", "Slack")
        assertThat(typeAndSearch(KeyboardLayouts.ID, "goj", labels).first()).isEqualTo("Gojek")
        assertThat(typeAndSearch(KeyboardLayouts.ID, "toko", labels).first()).isEqualTo("Tokopedia")
    }

    @Test
    fun drawableWordsOfNamesWithDiacriticsUseBaseLetters() {
        val index = SearchIndex.build(listOf(app("Straße"), app("España"), app("Ação")))
        assertThat(index.gestureVocabulary.map { it.latin }).containsAtLeast("strasse", "espana", "acao")
    }

    @Test
    fun wrongLayoutStaysBetweenEnglishAndRussian() {
        assertThat(KeyboardLayouts.swap(Text.normalize("über"))).isEqualTo("гиук")
        assertThat(KeyboardLayouts.swap("ñ")).isNull()
    }

    private companion object {
        const val TOLERANCE = 1e-5f
    }
}
