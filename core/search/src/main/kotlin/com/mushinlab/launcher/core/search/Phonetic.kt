package com.mushinlab.launcher.core.search

object Phonetic {
    private val rules = listOf(
        "ck" to "k",
        "ph" to "f",
        "kh" to "h",
        "ts" to "s",
        "c" to "k",
        "q" to "k",
        "w" to "v",
        "x" to "ks",
        "y" to "i",
        "j" to "i",
    )

    fun key(latin: String): String {
        var s = latin
        rules.forEach { (from, to) -> s = s.replace(from, to) }
        if (s.length < 2) return s
        val out = StringBuilder(s.length)
        s.forEach { c -> if (out.isEmpty() || out.last() != c) out.append(c) }
        return out.toString()
    }
}
