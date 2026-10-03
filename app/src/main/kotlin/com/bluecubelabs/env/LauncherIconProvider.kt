package com.bluecubelabs.env

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.bluecubelabs.env.core.apps.AppCatalog
import com.bluecubelabs.env.core.apps.icons.IconKey
import com.bluecubelabs.env.core.apps.icons.IconLoader
import com.bluecubelabs.env.core.apps.icons.LoadedIcon
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.ui.AppIconImage
import com.bluecubelabs.env.core.ui.IconProvider
import java.util.WeakHashMap
import kotlinx.coroutines.flow.first

class LauncherIconProvider(
    private val loader: IconLoader,
    private val catalog: AppCatalog,
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
        return IconKey(key, entry.lastUpdateTime, sizePx)
    }

    private fun LoadedIcon.toImage(): AppIconImage = when (this) {
        LoadedIcon.Letter -> AppIconImage.Letter

        is LoadedIcon.Mask -> AppIconImage.Mask(
            synchronized(images) {
                images.getOrPut(bitmap) { bitmap.asImageBitmap() }
            },
        )
    }
}
