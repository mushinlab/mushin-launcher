package com.mushinlab.launcher.core.search

import java.text.Normalizer
import java.util.Locale

object Text {
    fun isCyrillic(c: Char): Boolean = c in 'Ѐ'..'ӿ'

    fun isLatin(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z'

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
                val prev = previous
                if (prev != null && prev.isLetterOrDigit() && prev.isLowerCase() && c.isUpperCase()) flush()
                current.append(c)
            }
            previous = c
        }
        flush()
        return words
    }

    private fun stripMarks(c: Char, out: StringBuilder) {
        Normalizer.normalize(c.toString(), Normalizer.Form.NFKD).forEach { d ->
            if (Character.getType(d) != Character.NON_SPACING_MARK.toInt() && d.isLetterOrDigit()) {
                out.append(d.lowercaseChar())
            }
        }
    }

    private const val ASCII_LIMIT = 128
}
