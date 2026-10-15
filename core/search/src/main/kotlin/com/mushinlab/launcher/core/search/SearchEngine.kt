package com.mushinlab.launcher.core.search

import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.AppUsage
import com.mushinlab.launcher.core.model.UsageDecay
import kotlin.math.ln
import kotlin.math.min

enum class MatchLevel(val base: Int) {
    EXACT(base = 1000),
    PREFIX(base = 900),
    WORD_PREFIX(base = 800),
    INITIALS(base = 700),
    SUBSTRING(base = 500),
    PHONETIC_PREFIX(base = 450),
    SUBSEQUENCE(base = 350),
    FUZZY(base = 300),
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
    private val matcher = NameMatcher(options)

    fun search(
        index: SearchIndex,
        query: String,
        usage: Map<AppKey, AppUsage> = emptyMap(),
        now: Long = 0L,
        limit: Int = MAX_RESULTS,
        includeHidden: Boolean = false,
    ): List<SearchResult> {
        val variants = variants(query)
        if (variants.isEmpty()) return emptyList()
        val results = ArrayList<SearchResult>()
        index.items.forEach { indexed ->
            val best = bestMatch(indexed, variants) ?: return@forEach
            val (level, score) = best
            if (indexed.item.hidden && !includeHidden && level.base < MatchLevel.PREFIX.base) return@forEach
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
            val originalOfCustom = hasCustom && !name.isCustom
            variants.forEach { v ->
                val match = matcher.match(name, v) ?: return@forEach
                val score = score(match, v, originalOfCustom)
                if (score > bestScore) {
                    bestScore = score
                    bestLevel = match.level
                }
            }
        }
        return bestLevel?.let { it to bestScore }
    }

    private fun score(match: NameMatch, v: QueryVariant, originalOfCustom: Boolean): Int {
        var score = match.level.base - match.penalty
        if (v.viaSwap) score -= SWAP_PENALTY
        if (match.usedTranslit) score -= TRANSLIT_PENALTY
        if (originalOfCustom) score -= ORIGINAL_LABEL_PENALTY
        return score
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

    companion object {
        const val MAX_RESULTS = 50
        private const val SWAP_PENALTY = 40
        private const val TRANSLIT_PENALTY = 20
        private const val ORIGINAL_LABEL_PENALTY = 10
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
