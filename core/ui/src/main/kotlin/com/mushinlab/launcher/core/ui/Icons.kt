package com.mushinlab.launcher.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.AppKey
import kotlin.math.roundToInt

/**
 * An app icon ready to draw: a [Mask] tinted with the palette icon color, a [Full] color bitmap, or [Letter]
 * for the first letter of the label in a circle.
 */
sealed interface AppIconImage {
    @Immutable
    data class Mask(val bitmap: ImageBitmap) : AppIconImage

    @Immutable
    data class Full(val bitmap: ImageBitmap) : AppIconImage

    data object Letter : AppIconImage
}

/**
 * Source of app icons for [AppIcon]. [peek] must return only what is already in memory and never block;
 * [load] may render and is called from a coroutine.
 */
@Stable
interface IconProvider {
    fun peek(key: AppKey, sizePx: Int): AppIconImage?

    suspend fun load(key: AppKey, sizePx: Int): AppIconImage
}

/** Icon provider for [AppIcon]; null (in tests and previews) shows nothing until an icon is provided. */
val LocalIconProvider = staticCompositionLocalOf<IconProvider?> { null }

/**
 * The icon of [key] at [size]. Shows a cached icon immediately when available, otherwise loads it
 * asynchronously; [label] supplies the letter for [AppIconImage.Letter].
 */
@Composable
fun AppIcon(key: AppKey, label: String, size: Dp, modifier: Modifier = Modifier) {
    val provider = LocalIconProvider.current
    val sizePx = with(LocalDensity.current) { size.toPx().roundToInt() }
    var image by remember(provider, key, sizePx) { mutableStateOf(provider?.peek(key, sizePx)) }
    if (image == null && provider != null) {
        LaunchedEffect(provider, key, sizePx) { image = provider.load(key, sizePx) }
    }
    val colors = LocalMushinColors.current
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        when (val current = image) {
            is AppIconImage.Mask -> Image(
                bitmap = current.bitmap,
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.iconTint, BlendMode.SrcIn),
                modifier = Modifier.size(size),
            )

            is AppIconImage.Full -> Image(
                bitmap = current.bitmap,
                contentDescription = null,
                modifier = Modifier.size(size),
            )

            AppIconImage.Letter -> Box(
                Modifier.size(size).border(1.5.dp, colors.iconTint, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = label.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "•",
                    style = MushinType.style((size.value * LETTER_RATIO).sp, colors.iconTint),
                )
            }

            null -> Unit
        }
    }
}

private const val LETTER_RATIO = 18f / 28f
