package com.mushinlab.launcher.core.search

enum class KeyAction { SWITCH_LAYOUT, BACKSPACE, SPACE, RESIZE }

data class KeySpec(
    val char: Char? = null,
    val digit: Char? = null,
    val action: KeyAction? = null,
    val weight: Float = 1f,
)

data class KeyboardLayout(
    val id: String,
    val label: String,
    val rows: List<List<KeySpec>>,
    val rowInsets: List<Float>,
    val rowScales: List<Float> = rows.map { 1f },
)

object KeyboardLayouts {
    private const val DIGITS = "1234567890"
    private const val SIDE_KEY_WEIGHT = 1.5f
    private const val SPACE_WEIGHT = 8.5f
    private const val SPACE_ROW_SCALE = 0.5f
    const val LETTER_ROWS = 3

    val EN: KeyboardLayout = layout(
        id = "en",
        label = "EN",
        top = "qwertyuiop",
        middle = "asdfghjkl",
        bottom = "zxcvbnm",
        middleInset = 0.05f,
    )

    val RU: KeyboardLayout = layout(
        id = "ru",
        label = "RU",
        top = "йцукенгшщзхъ",
        middle = "фывапролджэ",
        bottom = "ячсмитьбю",
        middleInset = 0f,
    )

    val all: List<KeyboardLayout> = listOf(EN, RU)

    fun byId(id: String): KeyboardLayout? = all.firstOrNull { it.id == id }

    private const val EN_PHYSICAL = "qwertyuiop[]asdfghjkl;'zxcvbnm,."
    private const val RU_PHYSICAL = "йцукенгшщзхъфывапролджэячсмитьбю"

    private val enToRu: Map<Char, Char> = EN_PHYSICAL.zip(RU_PHYSICAL).toMap()
    private val ruToEn: Map<Char, Char> = RU_PHYSICAL.zip(EN_PHYSICAL).toMap()

    fun swap(text: String): String? {
        val letters = text.filter { it.isLetter() }
        if (letters.isEmpty()) return null
        val map = when {
            letters.all { it in enToRu } -> enToRu
            letters.all { it in ruToEn } -> ruToEn
            else -> return null
        }
        return text.map { map[it] ?: it }.joinToString("")
    }

    private fun layout(id: String, label: String, top: String, middle: String, bottom: String, middleInset: Float) =
        KeyboardLayout(
            id = id,
            label = label,
            rows = listOf(
                top.mapIndexed { i, c -> KeySpec(char = c, digit = DIGITS.getOrNull(i)) },
                middle.map { KeySpec(char = it) },
                listOf(KeySpec(action = KeyAction.SWITCH_LAYOUT, weight = SIDE_KEY_WEIGHT)) +
                    bottom.map { KeySpec(char = it) } +
                    KeySpec(action = KeyAction.BACKSPACE, weight = SIDE_KEY_WEIGHT),
                listOf(
                    KeySpec(action = KeyAction.RESIZE, weight = SIDE_KEY_WEIGHT),
                    KeySpec(action = KeyAction.SPACE, weight = SPACE_WEIGHT),
                ),
            ),
            rowInsets = listOf(0f, middleInset, 0f, 0f),
            rowScales = listOf(1f, 1f, 1f, SPACE_ROW_SCALE),
        )
}
