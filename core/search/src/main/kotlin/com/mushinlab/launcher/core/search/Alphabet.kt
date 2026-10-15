package com.mushinlab.launcher.core.search

import java.text.Collator
import java.util.Locale

/**
 * Sort order of app labels: Cyrillic first, then Latin, then the rest (by first letter or digit);
 * within a group, Russian collation that ignores case.
 */
object Alphabet {
    private val collator: Collator = Collator.getInstance(Locale.forLanguageTag("ru")).apply {
        strength = Collator.SECONDARY
    }

    private fun group(label: String): Int {
        val first = label.firstOrNull { it.isLetterOrDigit() } ?: return 2
        return when {
            Text.isCyrillic(first) -> 0
            Text.isLatin(first) -> 1
            else -> 2
        }
    }

    fun compare(a: String, b: String): Int {
        val byGroup = group(a).compareTo(group(b))
        return if (byGroup != 0) byGroup else collator.compare(a, b)
    }

    fun <T> comparator(label: (T) -> String): Comparator<T> = Comparator { x, y -> compare(label(x), label(y)) }
}
