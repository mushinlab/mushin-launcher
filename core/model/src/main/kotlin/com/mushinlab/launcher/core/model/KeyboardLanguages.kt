package com.mushinlab.launcher.core.model

import java.util.Locale

/** Ids of the keyboard layouts the launcher offers and the list enabled when nothing is stored (ADR-0023). */
object KeyboardLanguages {
    const val ENGLISH = "en"

    /** Layout ids, which are also the language codes of the layouts, in the order they are offered. */
    val SUPPORTED: List<String> = listOf(ENGLISH, "ru", "es", "pt", "de", "fr", "id")

    /**
     * The layout of the device [language] (an ISO 639 code; the legacy "in" stands for Indonesian), or null when
     * the launcher has no layout for it.
     */
    fun layoutFor(language: String): String? {
        val code = language.lowercase(Locale.ROOT)
        return (if (code == LEGACY_INDONESIAN) INDONESIAN else code).takeIf { it in SUPPORTED }
    }

    /** The layout of the device [language] followed by English; English alone when the language has no layout. */
    fun defaultsFor(language: String): List<String> = listOfNotNull(layoutFor(language), ENGLISH).distinct()

    private const val INDONESIAN = "id"
    private const val LEGACY_INDONESIAN = "in"
}
