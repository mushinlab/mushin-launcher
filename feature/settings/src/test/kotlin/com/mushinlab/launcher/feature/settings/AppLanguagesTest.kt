package com.mushinlab.launcher.feature.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppLanguagesTest {
    @Test
    fun sevenLanguagesEachNamedInItself() {
        assertThat(AppLanguages.all.map { it.first })
            .containsExactly("en", "ru", "es", "pt-BR", "de", "fr", "id")
            .inOrder()
        assertThat(AppLanguages.all.toMap().getValue("ru")).isEqualTo("Русский")
        assertThat(AppLanguages.all.toMap().getValue("pt-BR")).isEqualTo("Português (Brasil)")
        assertThat(AppLanguages.all.toMap().getValue("id")).isEqualTo("Bahasa Indonesia")
    }

    @Test
    fun emptyLocalesMeanSystemDefault() {
        assertThat(AppLanguages.match("")).isNull()
    }

    @Test
    fun appLocalesAreMatchedByLanguage() {
        assertThat(AppLanguages.match("ru")).isEqualTo("ru")
        assertThat(AppLanguages.match("en-US")).isEqualTo("en")
        assertThat(AppLanguages.match("de-AT,en")).isEqualTo("de")
        assertThat(AppLanguages.match("pt-BR")).isEqualTo("pt-BR")
        assertThat(AppLanguages.match("pt-PT")).isEqualTo("pt-BR")
        assertThat(AppLanguages.match("id")).isEqualTo("id")
        assertThat(AppLanguages.match("in-ID")).isEqualTo("id")
    }

    @Test
    fun unsupportedLocaleIsNotOffered() {
        assertThat(AppLanguages.match("ja-JP")).isNull()
    }
}
