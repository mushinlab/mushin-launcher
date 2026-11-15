package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import com.mushinlab.launcher.core.ui.MushinColors

internal object MiniKeyboard {
    fun key(top: Float, row: Int, column: Int) = Offset(
        (Scene.WIDTH - KEY_STEP * KeyCounts[row]) / 2 + KEY_STEP * column + KEY_STEP / 2,
        top + KEYS_TOP + KEY_ROW * row + (KEY_ROW - KEY_GAP) / 2,
    )
}

internal fun DrawScope.miniKeyboard(colors: MushinColors, top: Float, query: TextLayoutResult? = null) {
    drawRoundRect(colors.surface, Offset(0f, top), Size(Scene.WIDTH, Scene.HEIGHT - top), CornerRadius(KEY_CORNER))
    val caret = Scene.MARGIN + (query?.size?.width ?: 0)
    if (query != null) text(query, colors.textPrimary, Offset(Scene.MARGIN, top + QUERY_TOP))
    drawLine(
        colors.accent,
        Offset(caret, top + CARET_TOP),
        Offset(caret, top + CARET_TOP + CARET_HEIGHT),
        strokeWidth = 1f,
    )
    KeyCounts.forEachIndexed { row, count ->
        val width = if (row == KeyCounts.lastIndex) SPACE_WIDTH else KEY_STEP * count
        repeat(count) { column ->
            drawRoundRect(
                colors.keyBackground,
                Offset((Scene.WIDTH - width) / 2 + KEY_STEP * column + KEY_GAP / 2, top + KEYS_TOP + KEY_ROW * row),
                Size(if (row == KeyCounts.lastIndex) SPACE_WIDTH else KEY_STEP - KEY_GAP, KEY_ROW - KEY_GAP),
                CornerRadius(KEY_CORNER),
            )
        }
    }
}

private val KeyCounts = listOf(10, 9, 7, 1)
private const val QUERY_TOP = 7f
private const val CARET_TOP = 8f
private const val CARET_HEIGHT = 12f
private const val KEYS_TOP = 26f
private const val KEY_ROW = 22f
private const val KEY_STEP = 17f
private const val KEY_GAP = 2f
private const val KEY_CORNER = 3f
private const val SPACE_WIDTH = 90f
