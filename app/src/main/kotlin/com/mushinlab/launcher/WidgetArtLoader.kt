package com.mushinlab.launcher

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.mushinlab.launcher.feature.settings.WidgetArt
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class WidgetArtLoader(private val context: Context, private val choices: List<WidgetChoice>) : WidgetArt {
    private val density = context.resources.displayMetrics.densityDpi
    private val maxSide = (PREVIEW_DP * context.resources.displayMetrics.density).toInt()

    override suspend fun preview(id: Int): ImageBitmap? {
        val info = (choices.getOrNull(id) as? WidgetChoice.Hosted)?.info ?: return null
        val layout = if (info.previewLayout != 0) withContext(Dispatchers.Main) { renderLayout(info) } else null
        return layout ?: withContext(Dispatchers.IO) {
            runCatching { (info.loadPreviewImage(context, density) ?: info.loadIcon(context, density))?.scaled() }
                .getOrNull()
        }
    }

    override suspend fun appIcon(id: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        val packageName = when (val choice = choices.getOrNull(id)) {
            is WidgetChoice.Hosted -> choice.info.provider.packageName
            WidgetChoice.Clock -> context.packageName
            null -> return@withContext null
        }
        runCatching { context.packageManager.getApplicationIcon(packageName).scaled() }.getOrNull()
    }

    private fun renderLayout(info: AppWidgetProviderInfo): ImageBitmap? = runCatching {
        val view = RemoteViews(info.provider.packageName, info.previewLayout).apply(context, FrameLayout(context))
        val width = info.minWidth.coerceAtLeast(1)
        val height = info.minHeight.coerceAtLeast(1)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, width, height)
        val scale = min(1f, maxSide.toFloat() / maxOf(width, height))
        val bitmap = Bitmap.createBitmap(
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        Canvas(bitmap).apply { scale(scale, scale) }.also(view::draw)
        bitmap.asImageBitmap()
    }.getOrNull()

    private fun Drawable.scaled(): ImageBitmap {
        val width = intrinsicWidth.takeIf { it > 0 } ?: maxSide
        val height = intrinsicHeight.takeIf { it > 0 } ?: maxSide
        val scale = min(1f, maxSide.toFloat() / maxOf(width, height))
        return toBitmap((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
            .asImageBitmap()
    }

    private companion object {
        const val PREVIEW_DP = 160f
    }
}
