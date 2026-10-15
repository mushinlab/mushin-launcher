package com.bluecubelabs.env.core.data

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import com.bluecubelabs.env.core.data.background.BackgroundStore
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackgroundStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun image(width: Int, height: Int, format: Bitmap.CompressFormat): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        return tmp.newFile().apply { outputStream().use { bitmap.compress(format, 90, it) } }
    }

    @Test
    fun pickedImageIsKeptScaledToCoverTheScreenAndSurvivesRestart() {
        val dir = tmp.newFolder()
        val store = BackgroundStore(dir, 100, 200)
        assertThat(store.image.value).isNull()
        assertThat(store.save(ImageDecoder.createSource(image(800, 800, Bitmap.CompressFormat.PNG)))).isTrue()
        assertThat(store.image.value?.width).isEqualTo(200)
        assertThat(store.image.value?.height).isEqualTo(200)
        val restarted = BackgroundStore(dir, 100, 200)
        restarted.load()
        assertThat(restarted.image.value?.height).isEqualTo(200)
    }

    @Test
    fun smallImagesAreNotEnlargedAndOtherFormatsAreAccepted() {
        val store = BackgroundStore(tmp.newFolder(), 1080, 2400)
        assertThat(store.save(ImageDecoder.createSource(image(30, 40, Bitmap.CompressFormat.JPEG)))).isTrue()
        assertThat(store.image.value?.width).isEqualTo(30)
        assertThat(store.save(ImageDecoder.createSource(image(30, 40, Bitmap.CompressFormat.WEBP_LOSSLESS)))).isTrue()
    }

    @Test
    fun notAnImageIsRejectedAndClearRemovesTheBackground() {
        val dir = tmp.newFolder()
        val store = BackgroundStore(dir, 100, 200)
        val text = tmp.newFile().apply { writeText("not an image") }
        assertThat(store.save(ImageDecoder.createSource(text))).isFalse()
        assertThat(store.image.value).isNull()
        store.save(ImageDecoder.createSource(image(50, 50, Bitmap.CompressFormat.PNG)))
        store.clear()
        assertThat(store.image.value).isNull()
        val restarted = BackgroundStore(dir, 100, 200)
        restarted.load()
        assertThat(restarted.image.value).isNull()
        assertThat(dir.listFiles().orEmpty()).isEmpty()
    }
}
