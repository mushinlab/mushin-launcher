package com.mushinlab.launcher.core.search

object Translit {
    private val table: Map<Char, List<String>> = mapOf(
        'а' to listOf("a"),
        'б' to listOf("b"),
        'в' to listOf("v"),
        'г' to listOf("g"),
        'д' to listOf("d"),
        'е' to listOf("e"),
        'ё' to listOf("e"),
        'ж' to listOf("zh"),
        'з' to listOf("z"),
        'и' to listOf("i"),
        'й' to listOf("y", "i", "j"),
        'к' to listOf("k"),
        'л' to listOf("l"),
        'м' to listOf("m"),
        'н' to listOf("n"),
        'о' to listOf("o"),
        'п' to listOf("p"),
        'р' to listOf("r"),
        'с' to listOf("s"),
        'т' to listOf("t"),
        'у' to listOf("u"),
        'ф' to listOf("f"),
        'х' to listOf("h", "kh"),
        'ц' to listOf("ts", "c"),
        'ч' to listOf("ch"),
        'ш' to listOf("sh"),
        'щ' to listOf("sch", "shch"),
        'ъ' to listOf(""),
        'ы' to listOf("y"),
        'ь' to listOf(""),
        'э' to listOf("e"),
        'ю' to listOf("yu", "ju"),
        'я' to listOf("ya", "ja"),
    )

    fun toLatin(text: String): String {
        if (text.none(Text::isCyrillic)) return text
        val out = StringBuilder(text.length + LATIN_EXTRA_CAPACITY)
        text.forEach { c -> out.append(table[c]?.first() ?: c) }
        return out.toString()
    }

    fun variants(text: String, max: Int = MAX_VARIANTS): List<String> {
        if (text.none(Text::isCyrillic)) return listOf(text)
        var acc = listOf("")
        text.forEach { c ->
            val options = table[c] ?: listOf(c.toString())
            acc = acc.flatMap { prefix -> options.map { prefix + it } }.take(max)
        }
        val primary = toLatin(text)
        return (listOf(primary) + acc).distinct().take(max)
    }

    const val MAX_VARIANTS = 4
    private const val LATIN_EXTRA_CAPACITY = 4
}
