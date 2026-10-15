package com.mushinlab.launcher.core.search

import com.mushinlab.launcher.core.model.AppKey
import java.util.Locale

/** An app as search sees it: the system label, an optional user label that is shown instead, and the hidden flag. */
data class SearchItem(
    val key: AppKey,
    val originalLabel: String,
    val customLabel: String? = null,
    val hidden: Boolean = false,
) {
    val displayLabel: String get() = customLabel ?: originalLabel
}

internal class NameForm(
    val text: String,
    val words: List<String>,
) {
    val initials: String = words.mapNotNull { it.firstOrNull() }.joinToString("")
}

internal class IndexedName(
    val label: String,
    val native: NameForm,
    val latin: NameForm,
    val key: String,
    val isCustom: Boolean,
) {
    val latinIsNative: Boolean = native.text == latin.text
}

internal class IndexedItem(
    val item: SearchItem,
    val names: List<IndexedName>,
)

/**
 * Precomputed normalized, transliterated and phonetic forms of every app label. Immutable and thread-safe;
 * rebuild it with [build] when the app list or overrides change.
 *
 * @property alphabetical visible apps in [Alphabet] order.
 */
class SearchIndex internal constructor(
    internal val items: List<IndexedItem>,
    val alphabetical: List<SearchItem>,
) {
    val size: Int get() = items.size

    /** All apps, hidden ones included, in [Alphabet] order. */
    val alphabeticalWithHidden: List<SearchItem> by lazy {
        items.map { it.item }.sortedWith(Alphabet.comparator { it.displayLabel })
    }

    /**
     * Drawable words of visible apps: each whole label plus its words of three or more letters, as a-z key
     * sequences.
     */
    val gestureVocabulary: List<GestureWord> by lazy {
        items.asSequence()
            .filterNot { it.item.hidden }
            .flatMap { indexed -> indexed.names.asSequence().flatMap { gestureWords(indexed.item.key, it) } }
            .distinctBy { it.latin to it.key }
            .toList()
    }

    companion object {
        val EMPTY = SearchIndex(emptyList(), emptyList())

        /** Indexes [items]; a custom label is indexed in addition to the original one. */
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
            return IndexedName(
                label = label,
                native = NameForm(normalized, words),
                latin = NameForm(latin, words.map(Translit::toLatin)),
                key = Phonetic.key(latin),
                isCustom = isCustom,
            )
        }

        private const val MIN_GESTURE_WORD = 3

        private fun gestureWords(key: AppKey, name: IndexedName): Sequence<GestureWord> {
            val full = GestureWord(letters(name.latin.text), name.label.lowercase(Locale.ROOT), key)
            val words = name.native.words.asSequence().zip(name.latin.words.asSequence()) { word, latin ->
                GestureWord(letters(latin), word, key)
            }
            val tokens = name.label.split(NOT_LETTER).asSequence()
                .map(Text::normalize)
                .filter { it.isNotEmpty() }
                .map { token -> GestureWord(letters(Translit.toLatin(token)), token, key) }
            return (sequenceOf(full) + (words + tokens).filter { it.latin.length >= MIN_GESTURE_WORD })
                .filter { it.latin.length >= 2 }
        }

        private val NOT_LETTER = Regex("[^\\p{L}\\p{N}]+")

        private fun letters(latin: String): String = latin.filter { it in 'a'..'z' }
    }
}
