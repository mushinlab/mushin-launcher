package com.mushinlab.launcher.feature.search

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.ShapeVertex
import com.mushinlab.launcher.core.search.GestureDecoder
import com.mushinlab.launcher.core.search.GesturePoint
import com.mushinlab.launcher.core.search.KeySpec
import com.mushinlab.launcher.core.search.KeyboardLayout
import com.mushinlab.launcher.core.search.KeyboardLayouts

interface KeyboardActions {
    fun onChar(c: Char): Boolean

    fun onBackspace(): Boolean

    fun onClear()

    fun onSwitchLayout()

    fun onClose()

    fun onResize()
}

object KeyboardDimens {
    val rowHeight = 66.dp
    val verticalPadding = 4.dp
    val bottomLift = rowHeight
    val gap = 4.dp
    val radius = 6.dp
    val digitSwipe = 24.dp
    val clearSwipe = 48.dp
    val bubbleWidth = 48.dp
    val bubbleHeight = 56.dp
    val glideStart = 20.dp
    val crossSize = 32.sp
    val resizeMark = 22.dp
    const val DWELL_MS = 1000L
    const val VERTICAL_RATIO = 0.5f
    const val DIGIT_HOLD_MS = 300L
    const val REPEAT_START_MS = 400L
    const val REPEAT_STEP_MS = 50L
}

data class KeyGeometry(val spec: KeySpec, val rect: Rect)

object KeyboardGeometry {
    fun compute(
        layout: KeyboardLayout,
        width: Float,
        rowHeight: Float,
        topPadding: Float,
        gap: Float,
    ): List<KeyGeometry> {
        val keys = ArrayList<KeyGeometry>()
        var top = topPadding
        layout.rows.forEachIndexed { rowIndex, row ->
            val inset = width * layout.rowInsets.getOrElse(rowIndex) { 0f }
            val usable = width - 2 * inset
            val totalWeight = row.sumOf { it.weight.toDouble() }.toFloat()
            val height = rowHeight * layout.rowScales.getOrElse(rowIndex) { 1f }
            var x = inset
            row.forEach { spec ->
                val w = usable * spec.weight / totalWeight
                keys += KeyGeometry(spec, Rect(x + gap / 2, top + gap / 2, x + w - gap / 2, top + height - gap / 2))
                x += w
            }
            top += height
        }
        return keys
    }

    fun height(layout: KeyboardLayout, rowHeight: Float, padding: Float): Float =
        rowHeight * layout.rowScales.sum() + padding * 2

    fun hit(keys: List<KeyGeometry>, position: Offset): Int? {
        keys.forEachIndexed { i, k -> if (k.rect.contains(position)) return i }
        var best: Int? = null
        var bestDistance = Float.MAX_VALUE
        keys.forEachIndexed { i, k ->
            val dx = position.x - k.rect.center.x
            val dy = position.y - k.rect.center.y
            val inRowBand = position.y >= k.rect.top - k.rect.height && position.y <= k.rect.bottom + k.rect.height
            val d = dx * dx + dy * dy
            if (inRowBand && d < bestDistance) {
                bestDistance = d
                best = i
            }
        }
        return best
    }
}

@Stable
class KeyboardState {
    val pressed = mutableStateMapOf<Int, Boolean>()
    var bubbleKey by mutableStateOf<Int?>(null)
    val trail = mutableStateListOf<Offset>()
    val dwells = mutableStateListOf<Offset>()
    var vectors by mutableStateOf<List<ShapeVertex>>(emptyList())

    fun clearDrawing() {
        trail.clear()
        dwells.clear()
        vectors = emptyList()
    }
}

class Glide(
    val shiftPenalty: Float,
    val suggestMargin: Float,
    val onGlide: (GestureDecoder, List<GesturePoint>, List<ShapeVertex>) -> Boolean,
)

fun glideCenters(width: Float, rowHeight: Float, padding: Float, gap: Float): Map<Char, GesturePoint> =
    KeyboardGeometry.compute(KeyboardLayouts.EN, width, rowHeight, padding, gap)
        .mapNotNull { key -> key.spec.char?.takeIf { it in 'a'..'z' }?.let { it to key.rect.center } }
        .associate { (char, center) -> char to GesturePoint(center.x, center.y) }
