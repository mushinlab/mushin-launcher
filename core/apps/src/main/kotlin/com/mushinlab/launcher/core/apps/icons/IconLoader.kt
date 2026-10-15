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

/**
 * A rendered icon: an alpha [Mask] tinted with the palette, a [Full] color bitmap, or [Letter] when the icon
 * is missing or too solid to read as a silhouette.
 */
sealed interface LoadedIcon {
    data class Mask(val bitmap: Bitmap) : LoadedIcon

    data class Full(val bitmap: Bitmap) : LoadedIcon

    data object Letter : LoadedIcon
}

/**
 * Cache key of a rendered icon. [lastUpdateTime] invalidates icons after app updates; [colored] requests a
 * [LoadedIcon.Full] bitmap instead of a mask.
 */
data class IconKey(
    val app: AppKey,
    val lastUpdateTime: Long,
    val sizePx: Int,
    val colored: Boolean = false,
)

/** Supplies the system icon drawable of an activity for a screen density; null when unavailable. */
fun interface IconSource {
    fun drawable(app: AppKey, densityDpi: Int): Drawable?
}

/** [IconSource] that reads activity icons through LauncherApps. */
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

/**
 * Loads app icons through a memory LRU cache, the [IconDiskCache] and [IconRenderer].
 *
 * Sizes are rounded up to a multiple of 8 px so nearby sizes share cache entries. Rendering runs on
 * [dispatcher] (two IO threads by default); the memory cache holds up to [maxMemoryBytes].
 */
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

    /** The icon if it is already in memory; never blocks. Safe to call during composition. */
    fun peek(key: IconKey): LoadedIcon? = memory.get(key.normalized())

    /** Returns the icon from memory, disk or a fresh render; never fails, falls back to [LoadedIcon.Letter]. */
    suspend fun load(key: IconKey): LoadedIcon {
        val normalized = key.normalized()
        memory.get(normalized)?.let { return it }
        return withContext(dispatcher) {
            memory.get(normalized) ?: render(normalized).also { memory.put(normalized, it) }
        }
    }

    /** Drops memory and disk entries of a package after it changed or was removed. */
    fun invalidate(packageName: String, userSerial: Long) {
        memory.snapshot().keys
            .filter { it.app.packageName == packageName && it.app.userSerial == userSerial }
            .forEach { memory.remove(it) }
        disk.invalidate(packageName, userSerial)
    }

    /** Clears the memory cache at trim levels from TRIM_MEMORY_BACKGROUND up. */
    fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) memory.evictAll()
    }

    /** Current memory cache size in bytes. */
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
