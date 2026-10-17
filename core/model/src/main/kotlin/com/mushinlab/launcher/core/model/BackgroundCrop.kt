package com.mushinlab.launcher.core.model

import kotlin.math.max

/**
 * Pan and zoom of a background image inside a screen-shaped frame (ADR-0020).
 *
 * [zoom] 1 is "cover": the smallest scale at which the image fills the frame; it grows up to [MAX_ZOOM].
 * [centerX] and [centerY] are the image point shown at the frame centre, as fractions of the image size.
 * The default crop shows the middle of the image at cover scale.
 */
data class BackgroundCrop(val zoom: Float = 1f, val centerX: Float = HALF, val centerY: Float = HALF) {
    companion object {
        const val MAX_ZOOM = 3f
    }
}

/** A rectangle in source image pixels. */
data class CropRect(val left: Float, val top: Float, val width: Float, val height: Float)

/**
 * Crop math for a [sourceWidth] × [sourceHeight] image shown in a [frameWidth] × [frameHeight] frame.
 *
 * Every result keeps the image covering the whole frame: zoom stays within 1..[BackgroundCrop.MAX_ZOOM] and the
 * visible region never leaves the image.
 */
data class CropFrame(val sourceWidth: Int, val sourceHeight: Int, val frameWidth: Float, val frameHeight: Float) {
    private val coverScale: Float = max(frameWidth / sourceWidth, frameHeight / sourceHeight)

    /** Frame pixels per source pixel at the zoom of [crop]. */
    fun scale(crop: BackgroundCrop): Float = coverScale * crop.zoom.coerceIn(1f, BackgroundCrop.MAX_ZOOM)

    /** The part of the image visible in the frame, in source pixels. */
    fun region(crop: BackgroundCrop): CropRect {
        val scale = scale(crop)
        val width = (frameWidth / scale).coerceAtMost(sourceWidth.toFloat())
        val height = (frameHeight / scale).coerceAtMost(sourceHeight.toFloat())
        val left = (crop.centerX * sourceWidth - width / 2).coerceIn(0f, sourceWidth - width)
        val top = (crop.centerY * sourceHeight - height / 2).coerceIn(0f, sourceHeight - height)
        return CropRect(left, top, width, height)
    }

    /** [crop] with zoom and centre limited so that the image covers the frame. */
    fun clamp(crop: BackgroundCrop): BackgroundCrop {
        val zoom = crop.zoom.coerceIn(1f, BackgroundCrop.MAX_ZOOM)
        val rect = region(crop.copy(zoom = zoom))
        return BackgroundCrop(
            zoom = zoom,
            centerX = (rect.left + rect.width / 2) / sourceWidth,
            centerY = (rect.top + rect.height / 2) / sourceHeight,
        )
    }

    /** [crop] after the image was dragged by [dx], [dy] frame pixels. */
    fun pan(crop: BackgroundCrop, dx: Float, dy: Float): BackgroundCrop {
        val scale = scale(crop)
        return clamp(
            crop.copy(
                centerX = crop.centerX - dx / scale / sourceWidth,
                centerY = crop.centerY - dy / scale / sourceHeight,
            ),
        )
    }

    /** [crop] zoomed by [factor] around the frame point [focusX], [focusY], which keeps the same image point. */
    fun zoom(crop: BackgroundCrop, focusX: Float, focusY: Float, factor: Float): BackgroundCrop {
        val before = region(crop)
        val scale = scale(crop)
        val pointX = before.left + focusX / scale
        val pointY = before.top + focusY / scale
        val zoom = (crop.zoom * factor).coerceIn(1f, BackgroundCrop.MAX_ZOOM)
        val nextScale = coverScale * zoom
        val left = pointX - focusX / nextScale
        val top = pointY - focusY / nextScale
        return clamp(
            BackgroundCrop(
                zoom = zoom,
                centerX = (left + frameWidth / nextScale / 2) / sourceWidth,
                centerY = (top + frameHeight / nextScale / 2) / sourceHeight,
            ),
        )
    }
}

private const val HALF = 0.5f
