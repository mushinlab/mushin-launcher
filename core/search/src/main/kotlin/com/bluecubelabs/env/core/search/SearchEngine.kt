package com.bluecubelabs.env.core.search

import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.AppUsage
import com.bluecubelabs.env.core.model.UsageDecay
import kotlin.math.ln
import kotlin.math.min

enum class MatchLevel(val base: Int) {
    EXACT(1000),
    PREFIX(900),
    WORD_PREFIX(800),
    INITIALS(700),
    SUBSTRING(500),
    PHONETIC_PREFIX(450),
    SUBSEQUENCE(350),
    FUZZY(300),
}

data class SearchOptions(
    val fuzzy: Boolean = true,
    val layoutSwap: Boolean = true,
    val translit: Boolean = true,
)

data class SearchResult(
    val key: AppKey,
    val label: String,
    val score: Int,
    val level: MatchLevel,
    val hidden: Boolean,
)

class SearchEngine(private val options: SearchOptions = SearchOptions()) {
    fun search(
        index: SearchIndex,
        query: String,
        usage: Map<AppKey, AppUsage> = emptyMap(),
        now: Long = 0L,
        limit: Int = MAX_RESULTS,
    ): List<SearchResult> {
        val variants = variants(query)
        if (variants.isEmpty()) return emptyList()
        val results = ArrayList<SearchResult>()
        index.items.forEach { indexed ->
            val best = bestMatch(indexed, variants) ?: return@forEach
            val (level, score) = best
            if (indexed.item.hidden && level.base < MatchLevel.PREFIX.base) return@forEach
            results += SearchResult(
                key = indexed.item.key,
                label = indexed.item.displayLabel,
                score = score + usageBonus(usage[indexed.item.key], now),
                level = level,
                hidden = indexed.item.hidden,
            )
        }
        results.sortWith(RESULT_ORDER)
        return if (results.size > limit) results.subList(0, limit) else results
    }

    internal fun variants(query: String): List<QueryVariant> {
        val q0 = Text.normalize(query)
        if (q0.isEmpty()) return emptyList()
        val list = ArrayList<QueryVariant>(2 * Translit.MAX_VARIANTS)
        addVariants(list, q0, viaSwap = false)
        if (options.layoutSwap) {
            KeyboardLayouts.swap(q0)?.takeIf { it != q0 }?.let { addVariants(list, it, viaSwap = true) }
        }
        return list
    }

    private fun addVariants(list: MutableList<QueryVariant>, text: String, viaSwap: Boolean) {
        val cyrillic = text.any(Text::isCyrillic)
        val latins = if (cyrillic && options.translit) Translit.variants(text) else listOf(text)
        latins.forEach { lat ->
            list += QueryVariant(
                text = text,
                latin = lat,
                key = Phonetic.key(lat),
                viaSwap = viaSwap,
                viaTranslit = cyrillic && options.translit,
            )
        }
    }

    private fun bestMatch(indexed: IndexedItem, variants: List<QueryVariant>): Pair<MatchLevel, Int>? {
        var bestLevel: MatchLevel? = null
        var bestScore = Int.MIN_VALUE
        val hasCustom = indexed.item.customLabel != null
        indexed.names.forEach { name ->
            variants.forEach { v ->
                val match = match(name, v)
                if (match != null) {
                    var score = match.level.base - match.penalty
                    if (v.viaSwap) score -= SWAP_PENALTY
                    if (match.usedTranslit) score -= TRANSLIT_PENALTY
                    if (hasCustom && !name.isCustom) score -= ORIGINAL_LABEL_PENALTY
                    if (score > bestScore) {
                        bestScore = score
                        bestLevel = match.level
                    }
                }
            }
        }
        return bestLevel?.let { it to bestScore }
    }

    private fun match(name: IndexedName, v: QueryVariant): Match? {
        val latin = options.translit || (name.latinIsNative && !v.viaTranslit)
        return strictMatch(name, v, latin)
            ?: partialMatch(name, v, latin)
            ?: if (latin) looseMatch(name, v) else null
    }

    private fun strictMatch(name: IndexedName, v: QueryVariant, latin: Boolean): Match? {
        val q = v.text
        val t = v.viaTranslit
        return when {
            name.normalized == q -> Match(MatchLevel.EXACT, false)
            latin && v.key.isNotEmpty() && name.key == v.key -> Match(MatchLevel.EXACT, t)
            name.normalized.startsWith(q) -> Match(MatchLevel.PREFIX, false)
            latin && name.latin.startsWith(v.latin) -> Match(MatchLevel.PREFIX, t)
            name.words.any { it.startsWith(q) } -> Match(MatchLevel.WORD_PREFIX, false)
            latin && name.latinWords.any { it.startsWith(v.latin) } -> Match(MatchLevel.WORD_PREFIX, t)
            else -> null
        }
    }

    private fun partialMatch(name: IndexedName, v: QueryVariant, latin: Boolean): Match? {
        val q = v.text
        val t = v.viaTranslit
        return when {
            q.length < 2 -> null
            name.initials.startsWith(q) -> Match(MatchLevel.INITIALS, false)
            latin && name.latinInitials.startsWith(v.latin) -> Match(MatchLevel.INITIALS, t)
            name.normalized.contains(q) -> Match(MatchLevel.SUBSTRING, false)
            latin && name.latin.contains(v.latin) -> Match(MatchLevel.SUBSTRING, t)
            else -> null
        }
    }

    private fun looseMatch(name: IndexedName, v: QueryVariant): Match? {
        val t = v.viaTranslit
        return when {
            v.key.isNotEmpty() && name.key.startsWith(v.key) -> Match(MatchLevel.PHONETIC_PREFIX, t)
            v.text.length >= 3 && isSubsequence(v.latin, name.latin) -> Match(MatchLevel.SUBSEQUENCE, t)
            else -> fuzzyMatch(name, v)
        }
    }

    private fun fuzzyMatch(name: IndexedName, v: QueryVariant): Match? {
        val length = v.text.length
        if (!options.fuzzy || length < 3 || name.key.length + 1 < v.key.length) return null
        val maxDistance = if (length <= SHORT_QUERY) 1 else 2
        val d = Osa.prefixDistance(v.key, name.key)
        return if (d <= maxDistance) Match(MatchLevel.FUZZY, v.viaTranslit, penalty = FUZZY_STEP * d) else null
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

    private fun usageBonus(usage: AppUsage?, now: Long): Int {
        if (usage == null) return 0
        val decayed = UsageDecay.at(usage.decayedScore, usage.lastLaunchedAt, now)
        var bonus = min(MAX_USAGE_BONUS, USAGE_FACTOR * ln(1.0 + decayed)).toInt()
        if (now - usage.lastLaunchedAt in 0..RECENT_WINDOW_MS) bonus += RECENT_BONUS
        return bonus
    }

    internal data class QueryVariant(
        val text: String,
        val latin: String,
        val key: String,
        val viaSwap: Boolean,
        val viaTranslit: Boolean,
    )

    private data class Match(val level: MatchLevel, val usedTranslit: Boolean, val penalty: Int = 0)

    companion object {
        const val MAX_RESULTS = 50
        private const val SWAP_PENALTY = 40
        private const val TRANSLIT_PENALTY = 20
        private const val ORIGINAL_LABEL_PENALTY = 10
        private const val FUZZY_STEP = 60
        private const val SHORT_QUERY = 5
        private const val MAX_USAGE_BONUS = 120.0
        private const val USAGE_FACTOR = 40.0
        private const val RECENT_BONUS = 60
        private const val RECENT_WINDOW_MS = 60L * 60 * 1000

        private val RESULT_ORDER: Comparator<SearchResult> = Comparator { a, b ->
            when {
                a.score != b.score -> b.score.compareTo(a.score)
                a.label.length != b.label.length -> a.label.length.compareTo(b.label.length)
                else -> Alphabet.compare(a.label, b.label)
            }
        }
    }
}
