package com.mushinlab.launcher.core.search

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TextProcessingTest {
    @Test
    fun normalizeLowercasesStripsDiacriticsAndPunctuation() {
        assertThat(Text.normalize("Google Maps")).isEqualTo("googlemaps")
        assertThat(Text.normalize("Café-Bar!")).isEqualTo("cafebar")
        assertThat(Text.normalize("Ёжик")).isEqualTo("ежик")
        assertThat(Text.normalize("Мой Йогурт")).isEqualTo("моййогурт")
        assertThat(Text.normalize("2GIS")).isEqualTo("2gis")
    }

    @Test
    fun splitWordsBySpacesDashesAndCamelCase() {
        assertThat(Text.splitWords("Google Maps")).containsExactly("google", "maps").inOrder()
        assertThat(Text.splitWords("WhatsApp")).containsExactly("whats", "app").inOrder()
        assertThat(Text.splitWords("СберБанк")).containsExactly("сбер", "банк").inOrder()
        assertThat(Text.splitWords("Wi-Fi Map")).containsExactly("wi", "fi", "map").inOrder()
    }

    @Test
    fun translitPrimaryAndAlternatives() {
        assertThat(Translit.toLatin("слак")).isEqualTo("slak")
        assertThat(Translit.toLatin("щука")).isEqualTo("schuka")
        assertThat(Translit.toLatin("съезд")).isEqualTo("sezd")
        assertThat(Translit.toLatin("latin")).isEqualTo("latin")
        val variants = Translit.variants("юля")
        assertThat(variants.first()).isEqualTo("yulya")
        assertThat(variants).hasSize(Translit.MAX_VARIANTS)
        assertThat(variants).contains("julja")
        assertThat(Translit.variants("кот")).containsExactly("kot")
    }

    @Test
    fun phoneticKeyMatchesSpecExamples() {
        assertThat(Phonetic.key("slack")).isEqualTo("slak")
        assertThat(Phonetic.key(Translit.toLatin("слак"))).isEqualTo("slak")
        assertThat(Phonetic.key("telegram")).isEqualTo(Phonetic.key(Translit.toLatin("телеграм")))
        assertThat(Phonetic.key("sberbank")).isEqualTo("sberbank")
        assertThat(Phonetic.key("whatsapp")).isEqualTo("vhasap")
        assertThat(Phonetic.key(Translit.toLatin("вацап"))).isEqualTo("vasap")
        assertThat(Phonetic.key("photo")).isEqualTo("foto")
        assertThat(Phonetic.key("xbox")).isEqualTo("ksboks")
    }

    @Test
    fun layoutSwapBothDirections() {
        assertThat(KeyboardLayouts.swap("ыдфсл")).isEqualTo("slack")
        assertThat(KeyboardLayouts.swap("ckfr")).isEqualTo("слак")
        assertThat(KeyboardLayouts.swap("ghbdtn")).isEqualTo("привет")
        assertThat(KeyboardLayouts.swap("хъ")).isEqualTo("[]")
        assertThat(KeyboardLayouts.swap("slackы")).isNull()
        assertThat(KeyboardLayouts.swap("123")).isNull()
    }

    @Test
    fun keyboardLayoutsHaveDigitsOnTopRow() {
        val en = KeyboardLayouts.EN.rows[0]
        assertThat(en.map { it.char }.joinToString("")).isEqualTo("qwertyuiop")
        assertThat(en.mapNotNull { it.digit }.joinToString("")).isEqualTo("1234567890")
        val ru = KeyboardLayouts.RU.rows[0]
        assertThat(ru.first { it.char == 'з' }.digit).isEqualTo('0')
        assertThat(ru.first { it.char == 'х' }.digit).isNull()
        assertThat(ru.first { it.char == 'ъ' }.digit).isNull()
    }

    @Test
    fun osaCountsTranspositionAsOneEdit() {
        assertThat(Osa.distance("slakc", "slack")).isEqualTo(1)
        assertThat(Osa.distance("kitten", "sitting")).isEqualTo(3)
        assertThat(Osa.prefixDistance("telegarm", "telegram")).isEqualTo(1)
        assertThat(Osa.distance("", "abc")).isEqualTo(3)
    }
}
