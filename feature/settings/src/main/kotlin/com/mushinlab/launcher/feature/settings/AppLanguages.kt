package com.mushinlab.launcher.feature.settings

import java.util.Locale

object AppLanguages {
    val all: List<Pair<String, String>> = listOf(
        "en" to "English",
        "ru" to "Русский",
        "es" to "Español",
        "pt-BR" to "Português (Brasil)",
        "de" to "Deutsch",
        "fr" to "Français",
        "id" to "Bahasa Indonesia",
    )

    fun match(languageTags: String): String? {
        val first = languageTags.substringBefore(',').trim()
        if (first.isEmpty()) return null
        val language = when (val code = Locale.forLanguageTag(first).language) {
            "in" -> "id"
            "pt" -> "pt-BR"
            else -> code
        }
        return all.firstOrNull { it.first == language }?.first
    }
}
