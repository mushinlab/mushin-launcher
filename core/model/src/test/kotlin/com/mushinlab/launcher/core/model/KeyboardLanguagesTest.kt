package com.mushinlab.launcher.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KeyboardLanguagesTest {
    @Test
    fun supportedLanguageGetsItsLayoutFirstAndEnglishSecond() {
        assertThat(KeyboardLanguages.defaultsFor("ru")).containsExactly("ru", "en").inOrder()
        assertThat(KeyboardLanguages.defaultsFor("de")).containsExactly("de", "en").inOrder()
        assertThat(KeyboardLanguages.defaultsFor("es")).containsExactly("es", "en").inOrder()
        assertThat(KeyboardLanguages.defaultsFor("fr")).containsExactly("fr", "en").inOrder()
        assertThat(KeyboardLanguages.defaultsFor("pt")).containsExactly("pt", "en").inOrder()
    }

    @Test
    fun englishIsNotRepeated() {
        assertThat(KeyboardLanguages.defaultsFor("en")).containsExactly("en")
    }

    @Test
    fun unsupportedLanguageGetsEnglishOnly() {
        assertThat(KeyboardLanguages.defaultsFor("ja")).containsExactly("en")
        assertThat(KeyboardLanguages.defaultsFor("ar")).containsExactly("en")
        assertThat(KeyboardLanguages.defaultsFor("")).containsExactly("en")
    }

    @Test
    fun indonesianIsFoundByTheModernAndTheLegacyCode() {
        assertThat(KeyboardLanguages.defaultsFor("id")).containsExactly("id", "en").inOrder()
        assertThat(KeyboardLanguages.defaultsFor("in")).containsExactly("id", "en").inOrder()
    }

    @Test
    fun codeCaseDoesNotMatter() {
        assertThat(KeyboardLanguages.layoutFor("DE")).isEqualTo("de")
        assertThat(KeyboardLanguages.layoutFor("xx")).isNull()
    }
}
