package com.mushinlab.launcher.core.search

import java.text.Normalizer
import java.util.Locale

/** Text normalization shared by indexing and queries. */
object Text {
    /** True for the basic Cyrillic Unicode block (U+0400..U+04FF). */
    fun isCyrillic(c: Char): Boolean = c in 'Ѐ'..'ӿ'

    fun isLatin(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z'

    /**
     * Lowercases, maps "ё" to "е", folds other letters to their base letters (strips diacritics, so "ñ" becomes
     * "n" and "ç" becomes "c"; "ß" becomes "ss", "æ" becomes "ae") and drops everything that is not a letter
     * or digit.
     */
    fun normalize(raw: String): String {
        val lower = raw.lowercase(Locale.ROOT)
        val out = StringBuilder(lower.length)
        lower.forEach { c ->
            when {
                c == 'ё' -> out.append('е')
                isCyrillic(c) -> if (c.isLetterOrDigit()) out.append(c)
                c.isLetterOrDigit() && c.code < ASCII_LIMIT -> out.append(c)
                else -> stripMarks(c, out)
            }
        }
        return out.toString()
    }

    /**
     * Splits a label into normalized words at non-alphanumeric characters and camelCase boundaries,
     * for example "YouTube Music" to you, tube, music.
     */
    fun splitWords(raw: String): List<String> {
        val words = mutableListOf<String>()
        val current = StringBuilder()
        var previous: Char? = null
        fun flush() {
            if (current.isNotEmpty()) {
                val word = normalize(current.toString())
                if (word.isNotEmpty()) words += word
                current.clear()
            }
        }
        raw.forEach { c ->
            if (!c.isLetterOrDigit()) {
                flush()
            } else {
                if (isCamelBoundary(previous, c)) flush()
                current.append(c)
            }
            previous = c
        }
        flush()
        return words
    }

    private fun isCamelBoundary(previous: Char?, c: Char): Boolean =
        previous != null && previous.isLetterOrDigit() && previous.isLowerCase() && c.isUpperCase()

    private fun stripMarks(c: Char, out: StringBuilder) {
        UNDECOMPOSED_LETTERS[c]?.let {
            out.append(it)
            return
        }
        Normalizer.normalize(c.toString(), Normalizer.Form.NFKD).forEach { d ->
            if (Character.getType(d) != Character.NON_SPACING_MARK.toInt() && d.isLetterOrDigit()) {
                out.append(d.lowercaseChar())
            }
        }
    }

    private const val ASCII_LIMIT = 128

    private val UNDECOMPOSED_LETTERS: Map<Char, String> = mapOf(
        'ß' to "ss",
        'æ' to "ae",
        'œ' to "oe",
        'ø' to "o",
        'ł' to "l",
        'đ' to "d",
        'ı' to "i",
    )
}
