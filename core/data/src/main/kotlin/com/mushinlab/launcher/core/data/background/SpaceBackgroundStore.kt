package com.mushinlab.launcher.core.data.background

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.util.Size
import com.mushinlab.launcher.core.model.BackgroundCrop
import com.mushinlab.launcher.core.model.CropRect
import java.io.File
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Background images of spaces (ADR-0020): one WebP file per space, `space_<id>.webp` in [directory].
 *
 * Files are written atomically through a temporary file, so a failed save keeps the previous image. [versions]
 * maps the id of every space with an image to a number that changes on each save; [scan] fills it from disk.
 * Decoding and file I/O block: call everything except [versions] off the main thread.
 *
 * An image picked for editing is decoded no larger than needed to cover a [screenWidth] × [screenHeight] screen
 * at [BackgroundCrop.MAX_ZOOM] and no larger than [MAX_EDIT_PIXELS]; it is never enlarged.
 */
class SpaceBackgroundStore(private val directory: File, private val screenWidth: Int, private val screenHeight: Int) {
    private val stored = MutableStateFlow<Map<Long, Int>>(emptyMap())
    private val counter = AtomicInteger()
    val versions: StateFlow<Map<Long, Int>> = stored.asStateFlow()

    /** Reads which spaces have an image. */
    fun scan() {
        val ids = directory.listFiles().orEmpty().mapNotNull { idOf(it.name) }
        stored.update { current -> ids.associateWith { current[it] ?: counter.incrementAndGet() } }
    }

    /** The stored image of a space, or null. */
    fun load(spaceId: Long): Bitmap? = file(spaceId).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }

    /** The stored image of a space reduced by a power of two to about [maxSide] pixels on its longer side. */
    fun loadThumbnail(spaceId: Long, maxSide: Int): Bitmap? {
        val path = file(spaceId).takeIf { it.exists() }?.path ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    /** Decodes a picked image for the editor; null when it cannot be decoded. */
    fun decode(source: ImageDecoder.Source): Bitmap? = runCatching {
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            val scale = editScale(width, height, screenWidth, screenHeight)
            decoder.setTargetSize(max(1, (width * scale).roundToInt()), max(1, (height * scale).roundToInt()))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }.getOrNull()

    /**
     * Stores the [region] of [source] as the image of a space at the size of a [frameWidth] × [frameHeight]
     * frame, or smaller when the region has fewer pixels; false when writing failed.
     */
    fun save(spaceId: Long, source: Bitmap, region: CropRect, frameWidth: Int, frameHeight: Int): Boolean {
        val bitmap = crop(source, region, outputSize(region, frameWidth, frameHeight))
        return write(spaceId) { out -> bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, QUALITY, out) }
    }

    /** Copies the image file [from] as the image of a space; false when copying failed. */
    fun copy(spaceId: Long, from: File): Boolean = write(spaceId) { out ->
        from.inputStream().use { it.copyTo(out) }
        true
    }

    /** Deletes the image of a space. */
    fun delete(spaceId: Long) {
        file(spaceId).delete()
        stored.update { it - spaceId }
    }

    internal fun file(spaceId: Long): File = File(directory, "$PREFIX$spaceId$SUFFIX")

    private fun write(spaceId: Long, body: (OutputStream) -> Boolean): Boolean {
        directory.mkdirs()
        val target = file(spaceId)
        val temp = File(directory, "${target.name}.tmp")
        val written = runCatching { temp.outputStream().use(body) }.getOrDefault(false)
        if (!written || !temp.renameTo(target)) {
            temp.delete()
            return false
        }
        stored.update { it + (spaceId to counter.incrementAndGet()) }
        return true
    }

    companion object {
        const val MAX_EDIT_PIXELS = 16_000_000f
        private const val QUALITY = 90
    }
}

private const val PREFIX = "space_"
private const val SUFFIX = ".webp"

private fun idOf(name: String): Long? =
    name.takeIf { it.startsWith(PREFIX) && it.endsWith(SUFFIX) }?.removeSurrounding(PREFIX, SUFFIX)?.toLongOrNull()

internal fun editScale(width: Int, height: Int, screenWidth: Int, screenHeight: Int): Float {
    val cover = max(screenWidth.toFloat() / width, screenHeight.toFloat() / height)
    val pixels = sqrt(SpaceBackgroundStore.MAX_EDIT_PIXELS / (width.toFloat() * height))
    return min(1f, min(cover * BackgroundCrop.MAX_ZOOM, pixels))
}

internal fun outputSize(region: CropRect, frameWidth: Int, frameHeight: Int): Size {
    val width = min(frameWidth, region.width.roundToInt()).coerceAtLeast(1)
    val height = (width.toFloat() * frameHeight / frameWidth).roundToInt().coerceAtLeast(1)
    return Size(width, height)
}

internal fun crop(source: Bitmap, region: CropRect, size: Size): Bitmap {
    val out = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val left = region.left.roundToInt().coerceIn(0, source.width - 1)
    val top = region.top.roundToInt().coerceIn(0, source.height - 1)
    val src = Rect(
        left,
        top,
        (region.left + region.width).roundToInt().coerceIn(left + 1, source.width),
        (region.top + region.height).roundToInt().coerceIn(top + 1, source.height),
    )
    Canvas(out).drawBitmap(source, src, Rect(0, 0, size.width, size.height), Paint(Paint.FILTER_BITMAP_FLAG))
    return out
}
