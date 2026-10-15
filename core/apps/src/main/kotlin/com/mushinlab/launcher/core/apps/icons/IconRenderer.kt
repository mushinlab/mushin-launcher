package com.mushinlab.launcher.core.apps.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable

/** Rasterizes icons from an [IconSource] at the display density; failures fall back to [LoadedIcon.Letter]. */
class IconRenderer(private val source: IconSource, private val densityDpi: Int) {
    /**
     * Monochrome mask: the monochrome layer of an adaptive icon when present, otherwise the brightness of the
     * full icon. Returns [LoadedIcon.Letter] when the result is too solid.
     */
    fun mask(key: IconKey): LoadedIcon = runCatching { renderMask(key) }.getOrDefault(LoadedIcon.Letter)

    /** The icon in full color at [IconKey.sizePx]. */
    fun full(key: IconKey): LoadedIcon = runCatching {
        val drawable = source.drawable(key.app, densityDpi) ?: return@runCatching LoadedIcon.Letter
        val bitmap = Bitmap.createBitmap(key.sizePx, key.sizePx, Bitmap.Config.ARGB_8888)
        drawable.drawInto(bitmap, inset = 0)
        LoadedIcon.Full(bitmap)
    }.getOrDefault(LoadedIcon.Letter)

    private fun renderMask(key: IconKey): LoadedIcon {
        val drawable = source.drawable(key.app, densityDpi) ?: return LoadedIcon.Letter
        val size = key.sizePx
        val mono = (drawable as? AdaptiveIconDrawable)?.monochrome
        val alpha = if (mono != null) {
            IconMath.alphaFromPixels(rasterize(mono, size, (size * IconMath.ADAPTIVE_INSET_FRACTION).toInt()))
        } else {
            IconMath.brightnessToAlpha(rasterize(drawable, size, inset = 0))
        }
        return if (IconMath.coverage(alpha) > IconMath.COVERAGE_LIMIT) {
            LoadedIcon.Letter
        } else {
            LoadedIcon.Mask(alphaMask(alpha, size))
        }
    }

    private fun rasterize(drawable: Drawable, size: Int, inset: Int): IntArray {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable.drawInto(bitmap, inset)
        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        bitmap.recycle()
        return pixels
    }

    private fun Drawable.drawInto(bitmap: Bitmap, inset: Int) {
        val oldBounds = copyBounds()
        setBounds(-inset, -inset, bitmap.width + inset, bitmap.height + inset)
        draw(Canvas(bitmap))
        bounds = oldBounds
    }
}

internal fun alphaMask(alpha: ByteArray, size: Int): Bitmap {
    val argb = IntArray(alpha.size) { i -> ((alpha[i].toInt() and BYTE_MASK) shl ALPHA_SHIFT) or WHITE_RGB }
    val full = Bitmap.createBitmap(argb, size, size, Bitmap.Config.ARGB_8888)
    val mask = full.extractAlpha()
    full.recycle()
    return mask
}

private const val BYTE_MASK = 0xFF
private const val ALPHA_SHIFT = 24
private const val WHITE_RGB = 0xFFFFFF
