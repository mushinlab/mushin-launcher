package com.mushinlab.launcher

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.mushinlab.launcher.core.apps.AppCatalog
import com.mushinlab.launcher.core.apps.icons.IconKey
import com.mushinlab.launcher.core.apps.icons.IconLoader
import com.mushinlab.launcher.core.apps.icons.LoadedIcon
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.ui.AppIconImage
import com.mushinlab.launcher.core.ui.IconProvider
import java.util.WeakHashMap
import kotlinx.coroutines.flow.first

class LauncherIconProvider(
    private val loader: IconLoader,
    private val catalog: AppCatalog,
    private val colored: Boolean = false,
) : IconProvider {
    private val images = WeakHashMap<Bitmap, ImageBitmap>()

    override fun peek(key: AppKey, sizePx: Int): AppIconImage? = iconKey(key, sizePx)?.let {
        loader.peek(it)
    }?.toImage()

    override suspend fun load(key: AppKey, sizePx: Int): AppIconImage {
        catalog.loaded.first { it }
        val iconKey = iconKey(key, sizePx) ?: return AppIconImage.Letter
        return loader.load(iconKey).toImage()
    }

    private fun iconKey(key: AppKey, sizePx: Int): IconKey? {
        val entry = catalog.apps.value.firstOrNull { it.key == key } ?: return null
        return IconKey(key, entry.lastUpdateTime, sizePx, colored)
    }

    fun withColors(enabled: Boolean): LauncherIconProvider =
        if (enabled == colored) this else LauncherIconProvider(loader, catalog, enabled)

    private fun LoadedIcon.toImage(): AppIconImage = when (this) {
        LoadedIcon.Letter -> AppIconImage.Letter
        is LoadedIcon.Mask -> AppIconImage.Mask(image(bitmap))
        is LoadedIcon.Full -> AppIconImage.Full(image(bitmap))
    }

    private fun image(bitmap: Bitmap): ImageBitmap = synchronized(images) {
        images.getOrPut(bitmap) { bitmap.asImageBitmap() }
    }
}
