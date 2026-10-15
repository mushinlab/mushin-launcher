package com.mushinlab.launcher.core.data.background

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores the user's home background image as a single WebP file in [directory].
 *
 * Images are downscaled to just cover the screen and written atomically through a temporary file, so a failed
 * save keeps the previous image. Decoding and file I/O block: call [load], [save] and [clear] off the main
 * thread. [image] holds the current bitmap or null.
 */
class BackgroundStore(directory: File, private val screenWidth: Int, private val screenHeight: Int) {
    private val file = File(directory, FILE_NAME)
    private val temp = File(directory, "$FILE_NAME.tmp")
    private val current = MutableStateFlow<Bitmap?>(null)
    val image: StateFlow<Bitmap?> = current.asStateFlow()

    /** Reads the stored image into [image]; null when none is stored. */
    fun load() {
        current.value = if (file.exists()) BitmapFactory.decodeFile(file.path) else null
    }

    /** Decodes, downscales and stores [source]; false when decoding or writing failed. */
    fun save(source: ImageDecoder.Source): Boolean {
        val bitmap = runCatching {
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val width = info.size.width
                val height = info.size.height
                val scale = min(1f, max(screenWidth.toFloat() / width, screenHeight.toFloat() / height))
                decoder.setTargetSize(max(1, (width * scale).roundToInt()), max(1, (height * scale).roundToInt()))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }.getOrNull() ?: return false
        val written = runCatching {
            temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, QUALITY, it) }
        }.getOrDefault(false)
        if (!written || !temp.renameTo(file)) {
            temp.delete()
            return false
        }
        current.value = bitmap
        return true
    }

    /** Deletes the stored image. */
    fun clear() {
        file.delete()
        current.value = null
    }

    private companion object {
        const val FILE_NAME = "background.webp"
        const val QUALITY = 90
    }
}
