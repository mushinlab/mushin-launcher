package com.mushinlab.launcher.core.data

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.background.SpaceBackgroundStore
import com.mushinlab.launcher.core.model.CropRect
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SpaceBackgroundStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun bitmap(width: Int, height: Int, color: Int = Color.RED): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    private fun image(width: Int, height: Int, format: Bitmap.CompressFormat): File =
        tmp.newFile().apply { outputStream().use { bitmap(width, height).compress(format, 90, it) } }

    private fun assertNear(actual: Int?, expected: Int) {
        assertThat(actual).isNotNull()
        val pixel = actual ?: 0
        assertThat(Color.red(pixel)).isWithin(TOLERANCE).of(Color.red(expected))
        assertThat(Color.green(pixel)).isWithin(TOLERANCE).of(Color.green(expected))
        assertThat(Color.blue(pixel)).isWithin(TOLERANCE).of(Color.blue(expected))
    }

    @Test
    fun eachSpaceKeepsItsOwnImageAcrossRestarts() {
        val dir = tmp.newFolder()
        val store = SpaceBackgroundStore(dir, 100, 200)
        assertThat(store.versions.value).isEmpty()
        assertThat(store.save(1, bitmap(300, 600, Color.RED), CropRect(0f, 0f, 300f, 600f), 100, 200)).isTrue()
        assertThat(store.save(2, bitmap(300, 600, Color.BLUE), CropRect(0f, 0f, 300f, 600f), 100, 200)).isTrue()
        assertThat(File(dir, "space_1.webp").exists()).isTrue()
        assertThat(store.versions.value.keys).containsExactly(1L, 2L)
        val restarted = SpaceBackgroundStore(dir, 100, 200)
        restarted.scan()
        assertThat(restarted.versions.value.keys).containsExactly(1L, 2L)
        assertNear(restarted.load(1)?.getPixel(50, 100), Color.RED)
        assertNear(restarted.load(2)?.getPixel(50, 100), Color.BLUE)
        assertThat(restarted.load(3)).isNull()
    }

    @Test
    fun savedImageIsTheCroppedRegionAtScreenSizeAndNeverLarger() {
        val store = SpaceBackgroundStore(tmp.newFolder(), 100, 200)
        val source = bitmap(400, 400, Color.BLUE).apply {
            for (x in 200 until 400) for (y in 0 until 400) setPixel(x, y, Color.GREEN)
        }
        assertThat(store.save(1, source, CropRect(200f, 0f, 200f, 400f), 100, 200)).isTrue()
        val saved = store.load(1)
        assertThat(saved?.width).isEqualTo(100)
        assertThat(saved?.height).isEqualTo(200)
        assertNear(saved?.getPixel(50, 100), Color.GREEN)
        assertThat(store.save(2, source, CropRect(0f, 0f, 50f, 100f), 100, 200)).isTrue()
        assertThat(store.load(2)?.width).isEqualTo(50)
        assertThat(store.load(2)?.height).isEqualTo(100)
    }

    @Test
    fun savingAgainChangesTheVersionAndDeleteRemovesTheFile() {
        val dir = tmp.newFolder()
        val store = SpaceBackgroundStore(dir, 100, 200)
        val region = CropRect(0f, 0f, 100f, 200f)
        store.save(5, bitmap(100, 200), region, 100, 200)
        val first = store.versions.value.getValue(5)
        store.save(5, bitmap(100, 200), region, 100, 200)
        assertThat(store.versions.value.getValue(5)).isNotEqualTo(first)
        store.delete(5)
        assertThat(store.versions.value).isEmpty()
        assertThat(dir.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun pickedImagesAreDecodedReducedButNeverEnlarged() {
        val store = SpaceBackgroundStore(tmp.newFolder(), 100, 200)
        val big = store.decode(ImageDecoder.createSource(image(2000, 2000, Bitmap.CompressFormat.PNG)))
        assertThat(big?.width).isEqualTo(600)
        val small = store.decode(ImageDecoder.createSource(image(30, 40, Bitmap.CompressFormat.JPEG)))
        assertThat(small?.width).isEqualTo(30)
        val text = tmp.newFile().apply { writeText("not an image") }
        assertThat(store.decode(ImageDecoder.createSource(text))).isNull()
    }

    @Test
    fun copiedFileBecomesTheImageOfASpaceAndThumbnailIsReduced() {
        val store = SpaceBackgroundStore(tmp.newFolder(), 100, 200)
        assertThat(store.copy(4, image(800, 400, Bitmap.CompressFormat.WEBP_LOSSLESS))).isTrue()
        assertThat(store.versions.value.keys).containsExactly(4L)
        assertThat(store.load(4)?.width).isEqualTo(800)
        assertThat(store.loadThumbnail(4, 200)?.width).isEqualTo(200)
        assertThat(store.loadThumbnail(9, 200)).isNull()
    }

    private companion object {
        const val TOLERANCE = 8
    }
}
