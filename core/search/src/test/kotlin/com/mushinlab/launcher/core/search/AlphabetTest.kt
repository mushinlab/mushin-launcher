package com.mushinlab.launcher.core.search

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AlphabetTest {
    @Test
    fun cyrillicNamesComeFirstThenLatinThenEverythingElse() {
        val sorted = listOf("Zoom", "123", "Яндекс", "slack", "Альфа", "Ёлка").sortedWith(
            Alphabet.comparator {
                it
            },
        )

        assertThat(sorted).containsExactly("Альфа", "Ёлка", "Яндекс", "slack", "Zoom", "123").inOrder()
    }

    @Test
    fun caseDoesNotSplitTheOrder() {
        assertThat(Alphabet.compare("maps", "Maps")).isEqualTo(0)
        assertThat(Alphabet.compare("maps", "Notes")).isLessThan(0)
    }

    @Test
    fun leadingPunctuationIsSkippedWhenGrouping() {
        assertThat(Alphabet.compare("«Почта»", "Slack")).isLessThan(0)
    }
}
