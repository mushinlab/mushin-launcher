package com.mushinlab.launcher.feature.settings

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.annotation.DrawableRes
import kotlin.math.roundToInt

object PresetArt {
    fun render(art: Bitmap, dark: Boolean): Bitmap {
        val width = art.width
        val height = art.height
        val pixels = IntArray(width * height)
        art.getPixels(pixels, 0, width, 0, 0, width, height)
        return Bitmap.createBitmap(compose(pixels, width, dark), width, height, Bitmap.Config.ARGB_8888)
    }

    internal fun preview(resources: Resources, @DrawableRes art: Int, dark: Boolean, width: Int): Bitmap? =
        runCatching {
            val source = ImageDecoder.createSource(resources, art)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.setTargetSize(width, (info.size.height * width.toFloat() / info.size.width).roundToInt())
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            render(bitmap, dark)
        }.getOrNull()

    internal fun compose(art: IntArray, width: Int, dark: Boolean): IntArray = IntArray(art.size) { i ->
        val grain = grain(i % width, i / width)
        val argb = art[i]
        val white = 1f - (argb ushr ALPHA_SHIFT) / MAX
        rgb { shift ->
            val channel = argb shr shift and BYTE
            val painted = if (dark) darkInk(argb, channel, shift, grain) else lightInk(channel, shift, grain)
            painted * (1f - white) + MAX * white
        }
    }

    private fun lightInk(channel: Int, shift: Int, grain: Int): Float =
        channel * ((LIGHT_PAPER shr shift and BYTE) + grain) / MAX

    private fun darkInk(argb: Int, channel: Int, shift: Int, grain: Int): Float {
        val r = argb shr RED_SHIFT and BYTE
        val g = argb shr GREEN_SHIFT and BYTE
        val b = argb and BYTE
        val red = ((r - maxOf(g, b)) / RED_SPAN).coerceIn(0f, 1f)
        val ink = 1f - (r * LUMA_R + g * LUMA_G + b * LUMA_B) / MAX
        val paper = (DARK_PAPER shr shift and BYTE) + grain
        return channel * red + (paper + (DARK_INK - paper) * ink) * (1f - red)
    }

    private fun rgb(channel: (shift: Int) -> Float): Int {
        var color = OPAQUE
        for (shift in SHIFTS) color = color or (channel(shift).roundToInt().coerceIn(0, BYTE) shl shift)
        return color
    }

    private fun grain(x: Int, y: Int): Int {
        var hash = x * HASH_X + y * HASH_Y
        hash = (hash xor (hash ushr HASH_SHIFT_A)) * HASH_MIX
        hash = hash xor (hash ushr HASH_SHIFT_B)
        return Math.floorMod(hash, GRAIN * 2 + 1) - GRAIN
    }

    private const val LIGHT_PAPER = 0xFAF8F5
    private const val DARK_PAPER = 0x0E0E0F
    private const val DARK_INK = 200f
    private const val RED_SPAN = 96f
    private const val GRAIN = 3
    private const val BYTE = 0xFF
    private const val MAX = 255f
    private const val LUMA_R = 0.299f
    private const val LUMA_G = 0.587f
    private const val LUMA_B = 0.114f
    private const val OPAQUE = -0x1000000
    private const val ALPHA_SHIFT = 24
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val HASH_X = 374_761_393
    private const val HASH_Y = 668_265_263
    private const val HASH_MIX = 1_274_126_177
    private const val HASH_SHIFT_A = 13
    private const val HASH_SHIFT_B = 16
    private val SHIFTS = intArrayOf(RED_SHIFT, GREEN_SHIFT, 0)
}
