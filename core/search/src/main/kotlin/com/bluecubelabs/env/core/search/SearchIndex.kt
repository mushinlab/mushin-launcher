package com.bluecubelabs.env.core.search

import com.bluecubelabs.env.core.model.AppKey

data class SearchItem(
    val key: AppKey,
    val originalLabel: String,
    val customLabel: String? = null,
    val hidden: Boolean = false,
) {
    val displayLabel: String get() = customLabel ?: originalLabel
}

internal class IndexedName(
    val normalized: String,
    val latin: String,
    val key: String,
    val words: List<String>,
    val latinWords: List<String>,
    val initials: String,
    val latinInitials: String,
    val isCustom: Boolean,
) {
    val latinIsNative: Boolean = normalized == latin
}

internal class IndexedItem(
    val item: SearchItem,
    val names: List<IndexedName>,
)

class SearchIndex internal constructor(
    internal val items: List<IndexedItem>,
    val alphabetical: List<SearchItem>,
) {
    val size: Int get() = items.size

    val vocabulary: List<String> by lazy {
        items.asSequence()
            .filterNot { it.item.hidden }
            .flatMap { indexed -> indexed.names.asSequence().flatMap { sequenceOf(it.latin) + it.latinWords } }
            .map { word -> word.filter { it in 'a'..'z' } }
            .filter { it.length >= 2 }
            .distinct()
            .toList()
    }

    companion object {
        val EMPTY = SearchIndex(emptyList(), emptyList())

        fun build(items: List<SearchItem>): SearchIndex {
            val indexed = items.map { item ->
                val names = buildList {
                    item.customLabel?.let { add(indexName(it, isCustom = true)) }
                    add(indexName(item.originalLabel, isCustom = false))
                }
                IndexedItem(item, names)
            }
            val visible = items.filterNot { it.hidden }.sortedWith(Alphabet.comparator { it.displayLabel })
            return SearchIndex(indexed, visible)
        }

        private fun indexName(label: String, isCustom: Boolean): IndexedName {
            val normalized = Text.normalize(label)
            val latin = Translit.toLatin(normalized)
            val words = Text.splitWords(label).ifEmpty { listOf(normalized) }
            val latinWords = words.map(Translit::toLatin)
            return IndexedName(
                normalized = normalized,
                latin = latin,
                key = Phonetic.key(latin),
                words = words,
                latinWords = latinWords,
                initials = words.mapNotNull { it.firstOrNull() }.joinToString(""),
                latinInitials = latinWords.mapNotNull { it.firstOrNull() }.joinToString(""),
                isCustom = isCustom,
            )
        }
    }
}
