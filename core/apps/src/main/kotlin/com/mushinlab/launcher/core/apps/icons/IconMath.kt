package com.mushinlab.launcher.core.apps.icons

object IconMath {
    const val MIN_LUMINANCE = 0.15f
    const val COVERAGE_LIMIT = 0.92f
    const val ADAPTIVE_INSET_FRACTION = 0.25f
    private const val OPAQUE_THRESHOLD = 128
    private const val SIZE_BUCKET = 8
    private const val ALPHA_SHIFT = 24
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val CHANNEL_MASK = 0xFF
    private const val CHANNEL_MAX = 255
    private const val CHANNEL_SCALE = 255f
    private const val RED_WEIGHT = 0.2126f
    private const val GREEN_WEIGHT = 0.7152f
    private const val BLUE_WEIGHT = 0.0722f
    private const val FLAT_RANGE = 1e-3f
    private const val ROUNDING = 0.5f

    fun bucketSize(sizePx: Int): Int = (sizePx + SIZE_BUCKET - 1) / SIZE_BUCKET * SIZE_BUCKET

    fun luminance(argb: Int): Float {
        val r = (argb shr RED_SHIFT) and CHANNEL_MASK
        val g = (argb shr GREEN_SHIFT) and CHANNEL_MASK
        val b = argb and CHANNEL_MASK
        return (RED_WEIGHT * r + GREEN_WEIGHT * g + BLUE_WEIGHT * b) / CHANNEL_SCALE
    }

    fun brightnessToAlpha(pixels: IntArray): ByteArray {
        var min = 1f
        var max = 0f
        pixels.forEach { p ->
            if ((p ushr ALPHA_SHIFT) > 0) {
                val l = luminance(p)
                if (l < min) min = l
                if (l > max) max = l
            }
        }
        val range = max - min
        return ByteArray(pixels.size) { i ->
            val p = pixels[i]
            val a = (p ushr ALPHA_SHIFT) / CHANNEL_SCALE
            if (a == 0f) {
                0
            } else {
                val normalized = if (range < FLAT_RANGE) 1f else (luminance(p) - min) / range
                val l = MIN_LUMINANCE + normalized * (1f - MIN_LUMINANCE)
                (a * l * CHANNEL_SCALE + ROUNDING).toInt().coerceIn(0, CHANNEL_MAX).toByte()
            }
        }
    }

    fun alphaFromPixels(pixels: IntArray): ByteArray =
        ByteArray(pixels.size) { i -> (pixels[i] ushr ALPHA_SHIFT).toByte() }

    fun coverage(alpha: ByteArray): Float {
        if (alpha.isEmpty()) return 0f
        val opaque = alpha.count { (it.toInt() and CHANNEL_MASK) >= OPAQUE_THRESHOLD }
        return opaque.toFloat() / alpha.size
    }
}
