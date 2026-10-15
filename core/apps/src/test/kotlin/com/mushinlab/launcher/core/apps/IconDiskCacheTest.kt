package com.mushinlab.launcher.core.apps

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.apps.icons.IconDiskCache
import com.mushinlab.launcher.core.apps.icons.IconKey
import com.mushinlab.launcher.core.apps.icons.LoadedIcon
import com.mushinlab.launcher.core.apps.icons.alphaMask
import com.mushinlab.launcher.core.model.AppKey
import java.io.DataOutputStream
import java.nio.file.Files
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IconDiskCacheTest {
    private val dir = Files.createTempDirectory("icons").toFile()
    private val cache = IconDiskCache(dir)
    private val slack = IconKey(AppKey("slack", "Main", 0), lastUpdateTime = 1, sizePx = 8)
    private val maps = IconKey(AppKey("maps", "Main", 0), lastUpdateTime = 1, sizePx = 8)

    @Test
    fun maskSurvivesTheRoundTrip() {
        val alpha = ByteArray(64) { (it * 4).toByte() }
        cache.write(slack, LoadedIcon.Mask(alphaMask(alpha, 8)))

        val read = cache.read(slack) as LoadedIcon.Mask

        assertThat(read.bitmap.width).isEqualTo(8)
        assertThat(read.bitmap.getPixel(7, 7) ushr 24).isEqualTo(alpha[63].toInt() and 0xFF)
    }

    @Test
    fun letterIsRemembered() {
        cache.write(slack, LoadedIcon.Letter)

        assertThat(cache.read(slack)).isEqualTo(LoadedIcon.Letter)
    }

    @Test
    fun aFileWithAWrongSizeIsIgnoredInsteadOfAllocated() {
        cache.write(slack, LoadedIcon.Letter)
        val file = dir.listFiles().orEmpty().single()
        DataOutputStream(file.outputStream()).use { it.writeInt(Int.MAX_VALUE) }

        assertThat(cache.read(slack)).isNull()
    }

    @Test
    fun invalidationRemovesOnlyThatPackage() {
        cache.write(slack, LoadedIcon.Letter)
        cache.write(maps, LoadedIcon.Letter)

        cache.invalidate("slack", 0)

        assertThat(cache.read(slack)).isNull()
        assertThat(cache.read(maps)).isEqualTo(LoadedIcon.Letter)
    }
}
