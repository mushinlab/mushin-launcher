package com.mushinlab.launcher.core.apps.icons

object IconMath {
    const val MIN_LUMINANCE = 0.15f
    const val COVERAGE_LIMIT = 0.92f
    const val ADAPTIVE_INSET_FRACTION = 0.25f
    private const val OPAQUE_THRESHOLD = 128

    fun bucketSize(sizePx: Int): Int = (sizePx + 7) / 8 * 8

    fun luminance(argb: Int): Float {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
    }

    fun brightnessToAlpha(pixels: IntArray): ByteArray {
        var min = 1f
        var max = 0f
        pixels.forEach { p ->
            if ((p ushr 24) > 0) {
                val l = luminance(p)
                if (l < min) min = l
                if (l > max) max = l
            }
        }
        val range = max - min
        return ByteArray(pixels.size) { i ->
            val p = pixels[i]
            val a = (p ushr 24) / 255f
            if (a == 0f) {
                0
            } else {
                val normalized = if (range < 1e-3f) 1f else (luminance(p) - min) / range
                val l = MIN_LUMINANCE + normalized * (1f - MIN_LUMINANCE)
                (a * l * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
            }
        }
    }

    fun alphaFromPixels(pixels: IntArray): ByteArray = ByteArray(pixels.size) { i -> (pixels[i] ushr 24).toByte() }

    fun coverage(alpha: ByteArray): Float {
        if (alpha.isEmpty()) return 0f
        val opaque = alpha.count { (it.toInt() and 0xFF) >= OPAQUE_THRESHOLD }
        return opaque.toFloat() / alpha.size
    }
}
