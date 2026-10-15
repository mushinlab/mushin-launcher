package com.mushinlab.launcher.core.search

import com.mushinlab.launcher.core.search.SearchEngine.QueryVariant

internal data class NameMatch(val level: MatchLevel, val usedTranslit: Boolean, val penalty: Int = 0)

internal class NameMatcher(private val options: SearchOptions) {
    fun match(name: IndexedName, v: QueryVariant): NameMatch? {
        val latin = options.translit || (name.latinIsNative && !v.viaTranslit)
        return strictMatch(name, v, latin)
            ?: partialMatch(name, v, latin)
            ?: if (latin) looseMatch(name, v) else null
    }

    private fun strictMatch(name: IndexedName, v: QueryVariant, latin: Boolean): NameMatch? {
        val q = v.text
        val t = v.viaTranslit
        return when {
            name.native.text == q -> NameMatch(MatchLevel.EXACT, false)
            latin && v.key.isNotEmpty() && name.key == v.key -> NameMatch(MatchLevel.EXACT, t)
            name.native.text.startsWith(q) -> NameMatch(MatchLevel.PREFIX, false)
            latin && name.latin.text.startsWith(v.latin) -> NameMatch(MatchLevel.PREFIX, t)
            name.native.words.any { it.startsWith(q) } -> NameMatch(MatchLevel.WORD_PREFIX, false)
            latin && name.latin.words.any { it.startsWith(v.latin) } -> NameMatch(MatchLevel.WORD_PREFIX, t)
            else -> null
        }
    }

    private fun partialMatch(name: IndexedName, v: QueryVariant, latin: Boolean): NameMatch? {
        val q = v.text
        val t = v.viaTranslit
        return when {
            q.length < 2 -> null
            name.native.initials.startsWith(q) -> NameMatch(MatchLevel.INITIALS, false)
            latin && name.latin.initials.startsWith(v.latin) -> NameMatch(MatchLevel.INITIALS, t)
            name.native.text.contains(q) -> NameMatch(MatchLevel.SUBSTRING, false)
            latin && name.latin.text.contains(v.latin) -> NameMatch(MatchLevel.SUBSTRING, t)
            else -> null
        }
    }

    private fun looseMatch(name: IndexedName, v: QueryVariant): NameMatch? {
        val t = v.viaTranslit
        return when {
            v.key.isNotEmpty() && name.key.startsWith(v.key) -> NameMatch(MatchLevel.PHONETIC_PREFIX, t)

            v.text.length >= MIN_LOOSE_QUERY && isSubsequence(v.latin, name.latin.text) ->
                NameMatch(MatchLevel.SUBSEQUENCE, t)

            else -> fuzzyMatch(name, v)
        }
    }

    private fun fuzzyMatch(name: IndexedName, v: QueryVariant): NameMatch? {
        val length = v.text.length
        if (!options.fuzzy || length < MIN_LOOSE_QUERY || name.key.length + 1 < v.key.length) return null
        val maxDistance = if (length <= SHORT_QUERY) 1 else 2
        val d = Osa.prefixDistance(v.key, name.key)
        return if (d <= maxDistance) NameMatch(MatchLevel.FUZZY, v.viaTranslit, penalty = FUZZY_STEP * d) else null
    }

    private fun isSubsequence(query: String, target: String): Boolean {
        if (query.isEmpty() || target.isEmpty() || query[0] != target[0]) return false
        var i = 0
        for (c in target) {
            if (c == query[i]) {
                i++
                if (i == query.length) return true
            }
        }
        return false
    }

    private companion object {
        const val MIN_LOOSE_QUERY = 3
        const val FUZZY_STEP = 60
        const val SHORT_QUERY = 5
    }
}
