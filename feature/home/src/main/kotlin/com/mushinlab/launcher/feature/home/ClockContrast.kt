package com.mushinlab.launcher.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import com.mushinlab.launcher.core.model.BackgroundCrop
import com.mushinlab.launcher.core.model.CropFrame
import com.mushinlab.launcher.core.ui.BackgroundLook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val LocalClockBackdrop = staticCompositionLocalOf { ClockBackdrop() }

@Composable
internal fun rememberBackdropPixels(image: ImageBitmap?): State<PixelMap?> = key(image) {
    produceState<PixelMap?>(null, image) {
        value = withContext(Dispatchers.Default) { image?.toPixelMap() }
    }
}

@Immutable
internal data class ClockBackdrop(
    val pixels: PixelMap? = null,
    val surface: Color = Color.Black,
    val brightness: Float = 0f,
    val blurPx: Float = 0f,
    val frame: Rect = Rect.Zero,
    val loading: Boolean = false,
) {
    fun ink(bounds: Rect): Color {
        val area = bounds.inflate(blurPx).intersect(frame)
        val luminance = if (sampleable(bounds, area)) {
            pixels?.let { imageLuminance(it, area) } ?: surface.luminance()
        } else {
            surface.luminance()
        }
        val lightContrast = (1f + CONTRAST_OFFSET) / (luminance + CONTRAST_OFFSET)
        val darkContrast = (luminance + CONTRAST_OFFSET) / CONTRAST_OFFSET
        return if (lightContrast > darkContrast) Color.White else Color.Black
    }

    private fun sampleable(bounds: Rect, area: Rect): Boolean =
        !bounds.isEmpty && !frame.isEmpty && !area.isEmpty

    private fun imageLuminance(pixels: PixelMap, area: Rect): Float {
        val crop = BackgroundCrop()
        val source = CropFrame(pixels.width, pixels.height, frame.width, frame.height)
        val region = source.region(crop)
        val scale = source.scale(crop)
        val overlay = BackgroundLook.overlay(brightness)
        var luminance = 0f
        repeat(SAMPLES) { row ->
            repeat(SAMPLES) { column ->
                val x = area.left + area.width * (column + SAMPLE_CENTER) / SAMPLES
                val y = area.top + area.height * (row + SAMPLE_CENTER) / SAMPLES
                val sourceX = (region.left + (x - frame.left) / scale).toInt().coerceIn(0, pixels.width - 1)
                val sourceY = (region.top + (y - frame.top) / scale).toInt().coerceIn(0, pixels.height - 1)
                val background = pixels[sourceX, sourceY].compositeOver(surface)
                luminance += overlay.compositeOver(background).luminance()
            }
        }
        return luminance / (SAMPLES * SAMPLES)
    }
}

private const val SAMPLES = 9
private const val SAMPLE_CENTER = 0.5f
private const val CONTRAST_OFFSET = 0.05f
