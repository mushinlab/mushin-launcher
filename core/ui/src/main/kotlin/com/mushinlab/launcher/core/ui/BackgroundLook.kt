package com.mushinlab.launcher.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.model.BackgroundCrop
import com.mushinlab.launcher.core.model.CropFrame
import com.mushinlab.launcher.core.model.SpaceBackground

/**
 * How a space background is drawn (ADR-0020): negative brightness lays black up to [DARKEN_MAX] opacity over the
 * image, positive brightness white up to [LIGHTEN_MAX]; blur 1 is a [BLUR_MAX] radius. Home rows over an image
 * are drawn at the space's row opacity (ADR-0026, [MushinColors.row]).
 */
object BackgroundLook {
    const val DARKEN_MAX = 0.7f
    const val LIGHTEN_MAX = 0.5f
    val BLUR_MAX: Dp = 24.dp

    /** The layer drawn over the image for [brightness]; transparent at 0. */
    fun overlay(brightness: Float): Color {
        val value = SpaceBackground.brightness(brightness)
        return when {
            value < 0f -> Color.Black.copy(alpha = -value * DARKEN_MAX)
            value > 0f -> Color.White.copy(alpha = value * LIGHTEN_MAX)
            else -> Color.Transparent
        }
    }

    /** Blur radius of the image for [blur]. */
    fun blurRadius(blur: Float): Dp = BLUR_MAX * SpaceBackground.blur(blur)
}

/**
 * Fills the area with [image] cropped by [crop] (the middle at cover scale by default), blurred by [blur] and
 * then darkened or lightened by [brightness] as [BackgroundLook] describes. Only the image is blurred.
 */
@Composable
fun SpaceBackdrop(
    image: ImageBitmap,
    brightness: Float,
    blur: Float,
    modifier: Modifier = Modifier,
    crop: BackgroundCrop = BackgroundCrop(),
) {
    val radius = BackgroundLook.blurRadius(blur)
    Box(modifier) {
        Spacer(
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .then(if (radius > 0.dp) Modifier.blur(radius, BlurredEdgeTreatment.Rectangle) else Modifier)
                .drawBehind {
                    val frame = CropFrame(image.width, image.height, size.width, size.height)
                    val region = frame.region(crop)
                    val factor = frame.scale(crop)
                    scale(factor, factor, pivot = Offset.Zero) {
                        translate(-region.left, -region.top) { drawImage(image) }
                    }
                },
        )
        val overlay = BackgroundLook.overlay(brightness)
        if (overlay.alpha > 0f) Spacer(Modifier.fillMaxSize().background(overlay))
    }
}
