package com.mushinlab.launcher.core.search

import java.util.Locale

/** Special keys; [RESIZE] starts adjusting the keyboard width and position. */
enum class KeyAction { SWITCH_LAYOUT, SPACE, RESIZE }

/**
 * A key: a letter [char] with an optional [digit] typed by holding or swiping up, or a special [action].
 * [weight] is the key width relative to other keys in its row. A spec with neither [char] nor [action] is an empty
 * gap that only takes up space.
 */
data class KeySpec(
    val char: Char? = null,
    val digit: Char? = null,
    val action: KeyAction? = null,
    val weight: Float = 1f,
) {
    /** True for an empty gap: nothing is drawn or pressed there. */
    val isGap: Boolean get() = char == null && action == null
}

/**
 * A keyboard layout. [id] is the language code, [label] the two-letter code on the layout key and [nativeName]
 * the language name in that language. [rowInsets] is the indent on both sides of each row as a fraction of the
 * keyboard width; [rowScales] multiplies the height of each row.
 */
data class KeyboardLayout(
    val id: String,
    val label: String,
    val nativeName: String,
    val rows: List<List<KeySpec>>,
    val rowInsets: List<Float>,
    val rowScales: List<Float> = rows.map { 1f },
) {
    /** True when every letter a-z has a key, so drawings on this layout can be decoded by its own key centers. */
    val hasLatinLetters: Boolean by lazy {
        val chars = rows.flatten().mapNotNull { it.char }.toSet()
        ('a'..'z').all { it in chars }
    }
}

/**
 * The layouts: EN (QWERTY), RU (ЙЦУКЕН), ES (QWERTY with ñ), PT (QWERTY with ç), DE (QWERTZ with ü, ö, ä),
 * FR (AZERTY) and ID (QWERTY). Each has three letter rows (the bottom one with the layout switch on the left and
 * an empty gap on the right, where backspace used to be, so the letters keep their places), then a row with the
 * resize and space keys. The top row carries digit hints.
 */
object KeyboardLayouts {
    private const val DIGITS = "1234567890"
    private const val HALF_KEY_OF_TEN = 0.05f
    private const val HALF_KEY_OF_ELEVEN = 1f / 22
    private const val SIDE_KEY_WEIGHT = 1.5f
    private const val SPACE_WEIGHT = 8.5f
    private const val SPACE_ROW_SCALE = 0.5f
    const val LETTER_ROWS = 3

    val EN: KeyboardLayout = layout(
        id = "en",
        nativeName = "English",
        letters = LetterRows(top = "qwertyuiop", middle = "asdfghjkl", bottom = "zxcvbnm"),
        middleInset = HALF_KEY_OF_TEN,
    )

    val RU: KeyboardLayout = layout(
        id = "ru",
        nativeName = "Русский",
        letters = LetterRows(top = "йцукенгшщзхъ", middle = "фывапролджэ", bottom = "ячсмитьбю"),
        middleInset = 0f,
    )

    val ES: KeyboardLayout = layout(
        id = "es",
        nativeName = "Español",
        letters = LetterRows(top = "qwertyuiop", middle = "asdfghjklñ", bottom = "zxcvbnm"),
        middleInset = 0f,
    )

    val PT: KeyboardLayout = layout(
        id = "pt",
        nativeName = "Português",
        letters = LetterRows(top = "qwertyuiop", middle = "asdfghjklç", bottom = "zxcvbnm"),
        middleInset = 0f,
    )

    val DE: KeyboardLayout = layout(
        id = "de",
        nativeName = "Deutsch",
        letters = LetterRows(top = "qwertzuiopü", middle = "asdfghjklöä", bottom = "yxcvbnm"),
        middleInset = 0f,
        bottomInset = HALF_KEY_OF_ELEVEN,
    )

    val FR: KeyboardLayout = layout(
        id = "fr",
        nativeName = "Français",
        letters = LetterRows(top = "azertyuiop", middle = "qsdfghjklm", bottom = "wxcvbn"),
        middleInset = 0f,
        bottomInset = HALF_KEY_OF_TEN,
    )

    val ID: KeyboardLayout = layout(
        id = "id",
        nativeName = "Bahasa Indonesia",
        letters = LetterRows(top = "qwertyuiop", middle = "asdfghjkl", bottom = "zxcvbnm"),
        middleInset = HALF_KEY_OF_TEN,
    )

    /** All layouts in the order of [com.mushinlab.launcher.core.model.KeyboardLanguages.SUPPORTED]. */
    val all: List<KeyboardLayout> = listOf(EN, RU, ES, PT, DE, FR, ID)

    fun byId(id: String): KeyboardLayout? = all.firstOrNull { it.id == id }

    private const val EN_PHYSICAL = "qwertyuiop[]asdfghjkl;'zxcvbnm,."
    private const val RU_PHYSICAL = "йцукенгшщзхъфывапролджэячсмитьбю"

    private val enToRu: Map<Char, Char> = EN_PHYSICAL.zip(RU_PHYSICAL).toMap()
    private val ruToEn: Map<Char, Char> = RU_PHYSICAL.zip(EN_PHYSICAL).toMap()

    /**
     * Retypes [text] as if the other layout had been active, for example "ghbdtn" to "привет".
     * Returns null when the letters belong to neither layout or mix both.
     */
    fun swap(text: String): String? {
        val letters = text.filter { it.isLetter() }
        val map = when {
            letters.isEmpty() -> null
            letters.all { it in enToRu } -> enToRu
            letters.all { it in ruToEn } -> ruToEn
            else -> null
        }
        return map?.let { m -> text.map { m[it] ?: it }.joinToString("") }
    }

    private class LetterRows(val top: String, val middle: String, val bottom: String)

    private fun layout(
        id: String,
        nativeName: String,
        letters: LetterRows,
        middleInset: Float,
        bottomInset: Float = 0f,
    ) = KeyboardLayout(
        id = id,
        label = id.uppercase(Locale.ROOT),
        nativeName = nativeName,
        rows = listOf(
            letters.top.mapIndexed { i, c -> KeySpec(char = c, digit = DIGITS.getOrNull(i)) },
            letters.middle.map { KeySpec(char = it) },
            listOf(KeySpec(action = KeyAction.SWITCH_LAYOUT, weight = SIDE_KEY_WEIGHT)) +
                letters.bottom.map { KeySpec(char = it) } +
                KeySpec(weight = SIDE_KEY_WEIGHT),
            listOf(
                KeySpec(action = KeyAction.RESIZE, weight = SIDE_KEY_WEIGHT),
                KeySpec(action = KeyAction.SPACE, weight = SPACE_WEIGHT),
            ),
        ),
        rowInsets = listOf(0f, middleInset, bottomInset, 0f),
        rowScales = listOf(1f, 1f, 1f, SPACE_ROW_SCALE),
    )
}
