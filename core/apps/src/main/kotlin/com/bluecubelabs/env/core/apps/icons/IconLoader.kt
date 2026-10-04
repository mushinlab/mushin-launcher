package com.bluecubelabs.env.core.apps.icons

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import com.bluecubelabs.env.core.apps.AndroidLauncherAppsGateway
import com.bluecubelabs.env.core.model.AppKey
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.MessageDigest
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

interface IconSource {
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
    private val source: IconSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(2),
    maxMemoryBytes: Int = MAX_MEMORY_BYTES,
) {
    private val densityDpi = context.resources.displayMetrics.densityDpi
    private val diskDir = File(context.cacheDir, "icons")
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
            memory.get(normalized) ?: if (normalized.colored) {
                renderFull(normalized).also { memory.put(normalized, it) }
            } else {
                (readDisk(normalized) ?: render(normalized).also { writeDisk(normalized, it) })
                    .also { memory.put(normalized, it) }
            }
        }
    }

    fun invalidate(packageName: String, userSerial: Long) {
        memory.snapshot().keys
            .filter { it.app.packageName == packageName && it.app.userSerial == userSerial }
            .forEach { memory.remove(it) }
        val prefix = packagePrefix(packageName, userSerial)
        diskDir.listFiles { file -> file.name.startsWith(prefix) }?.forEach { it.delete() }
    }

    fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) memory.evictAll()
    }

    fun memoryBytes(): Int = memory.size()

    private fun IconKey.normalized() = copy(sizePx = IconMath.bucketSize(sizePx))

    private fun render(key: IconKey): LoadedIcon = runCatching { renderMask(key) }.getOrDefault(LoadedIcon.Letter)

    private fun renderFull(key: IconKey): LoadedIcon = runCatching {
        val drawable = source.drawable(key.app, densityDpi) ?: return@runCatching LoadedIcon.Letter
        val bitmap = Bitmap.createBitmap(key.sizePx, key.sizePx, Bitmap.Config.ARGB_8888)
        val oldBounds = drawable.copyBounds()
        drawable.setBounds(0, 0, key.sizePx, key.sizePx)
        drawable.draw(Canvas(bitmap))
        drawable.bounds = oldBounds
        LoadedIcon.Full(bitmap)
    }.getOrDefault(LoadedIcon.Letter)

    private fun renderMask(key: IconKey): LoadedIcon {
        val drawable = source.drawable(key.app, densityDpi) ?: return LoadedIcon.Letter
        val size = key.sizePx
        val mono = (drawable as? AdaptiveIconDrawable)?.monochrome
        val alpha = if (mono != null) {
            IconMath.alphaFromPixels(rasterize(mono, size, adaptiveLayer = true))
        } else {
            IconMath.brightnessToAlpha(rasterize(drawable, size, adaptiveLayer = false))
        }
        if (IconMath.coverage(alpha) > IconMath.COVERAGE_LIMIT) return LoadedIcon.Letter
        return LoadedIcon.Mask(alphaBitmap(alpha, size))
    }

    private fun rasterize(drawable: Drawable, size: Int, adaptiveLayer: Boolean): IntArray {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val inset = if (adaptiveLayer) (size * IconMath.ADAPTIVE_INSET_FRACTION).toInt() else 0
        val oldBounds = drawable.copyBounds()
        drawable.setBounds(-inset, -inset, size + inset, size + inset)
        drawable.draw(canvas)
        drawable.bounds = oldBounds
        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        bitmap.recycle()
        return pixels
    }

    private fun alphaBitmap(alpha: ByteArray, size: Int): Bitmap {
        val argb = IntArray(alpha.size) { i -> ((alpha[i].toInt() and 0xFF) shl 24) or 0xFFFFFF }
        val full = Bitmap.createBitmap(argb, size, size, Bitmap.Config.ARGB_8888)
        val mask = full.extractAlpha()
        full.recycle()
        return mask
    }

    private fun readDisk(key: IconKey): LoadedIcon? {
        val file = diskFile(key)
        if (!file.exists()) return null
        return runCatching {
            DataInputStream(file.inputStream().buffered()).use { input ->
                val size = input.readInt()
                if (size == 0) {
                    LoadedIcon.Letter
                } else {
                    val bytes = ByteArray(size * size)
                    input.readFully(bytes)
                    LoadedIcon.Mask(alphaBitmap(bytes, size))
                }
            }
        }.getOrNull()
    }

    private fun writeDisk(key: IconKey, icon: LoadedIcon) {
        runCatching {
            diskDir.mkdirs()
            DataOutputStream(diskFile(key).outputStream().buffered()).use { out ->
                when (icon) {
                    LoadedIcon.Letter -> out.writeInt(0)

                    is LoadedIcon.Mask -> {
                        val size = icon.bitmap.width
                        val buffer = java.nio.ByteBuffer.allocate(icon.bitmap.byteCount)
                        icon.bitmap.copyPixelsToBuffer(buffer)
                        out.writeInt(size)
                        val rowBytes = icon.bitmap.rowBytes
                        val raw = buffer.array()
                        for (row in 0 until size) out.write(raw, row * rowBytes, size)
                    }

                    is LoadedIcon.Full -> Unit
                }
            }
        }
    }

    private fun diskFile(key: IconKey): File {
        val rest = sha1("${key.app.activityName}|${key.lastUpdateTime}|${key.sizePx}|$FORMAT_VERSION")
        return File(diskDir, packagePrefix(key.app.packageName, key.app.userSerial) + rest)
    }

    private fun packagePrefix(packageName: String, userSerial: Long) = sha1("$packageName|$userSerial").take(16) + "_"

    private fun sha1(text: String): String = MessageDigest.getInstance("SHA-1")
        .digest(text.toByteArray())
        .joinToString("") { "%02x".format(it) }

    companion object {
        const val MAX_MEMORY_BYTES = 8 * 1024 * 1024
        private const val LETTER_COST = 64
        private const val FORMAT_VERSION = 1
    }
}
