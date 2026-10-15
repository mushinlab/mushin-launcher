package com.mushinlab.launcher.core.apps.icons

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.LruCache
import com.mushinlab.launcher.core.apps.AndroidLauncherAppsGateway
import com.mushinlab.launcher.core.model.AppKey
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface LoadedIcon {
    data class Mask(val bitmap: Bitmap) : LoadedIcon

    data class Full(val bitmap: Bitmap) : LoadedIcon

    data object Letter : LoadedIcon
}

data class IconKey(
    val app: AppKey,
    val lastUpdateTime: Long,
    val sizePx: Int,
    val colored: Boolean = false,
)

fun interface IconSource {
    fun drawable(app: AppKey, densityDpi: Int): Drawable?
}

class LauncherIconSource(context: Context, private val gateway: AndroidLauncherAppsGateway) : IconSource {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    override fun drawable(app: AppKey, densityDpi: Int): Drawable? {
        val user = gateway.userFor(app.userSerial) ?: return null
        val info = runCatching { launcherApps.getActivityList(app.packageName, user) }.getOrNull()
            ?.firstOrNull { it.componentName.className == app.activityName }
            ?: return null
        return runCatching { info.getIcon(densityDpi) }.getOrNull()
    }
}

class IconLoader(
    context: Context,
    source: IconSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(2),
    maxMemoryBytes: Int = MAX_MEMORY_BYTES,
) {
    private val renderer = IconRenderer(source, context.resources.displayMetrics.densityDpi)
    private val disk = IconDiskCache(File(context.cacheDir, "icons"))
    private val memory = object : LruCache<IconKey, LoadedIcon>(maxMemoryBytes) {
        override fun sizeOf(key: IconKey, value: LoadedIcon): Int = when (value) {
            is LoadedIcon.Mask -> value.bitmap.allocationByteCount
            is LoadedIcon.Full -> value.bitmap.allocationByteCount
            LoadedIcon.Letter -> LETTER_COST
        }
    }

    fun peek(key: IconKey): LoadedIcon? = memory.get(key.normalized())

    suspend fun load(key: IconKey): LoadedIcon {
        val normalized = key.normalized()
        memory.get(normalized)?.let { return it }
        return withContext(dispatcher) {
            memory.get(normalized) ?: render(normalized).also { memory.put(normalized, it) }
        }
    }

    fun invalidate(packageName: String, userSerial: Long) {
        memory.snapshot().keys
            .filter { it.app.packageName == packageName && it.app.userSerial == userSerial }
            .forEach { memory.remove(it) }
        disk.invalidate(packageName, userSerial)
    }

    fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) memory.evictAll()
    }

    fun memoryBytes(): Int = memory.size()

    private fun render(key: IconKey): LoadedIcon = if (key.colored) {
        renderer.full(key)
    } else {
        disk.read(key) ?: renderer.mask(key).also { disk.write(key, it) }
    }

    private fun IconKey.normalized() = copy(sizePx = IconMath.bucketSize(sizePx))

    companion object {
        const val MAX_MEMORY_BYTES = 8 * 1024 * 1024
        private const val LETTER_COST = 64
    }
}
