package com.bluecubelabs.env.core.apps

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bluecubelabs.env.core.apps.icons.IconKey
import com.bluecubelabs.env.core.apps.icons.IconLoader
import com.bluecubelabs.env.core.apps.icons.LauncherIconSource
import com.bluecubelabs.env.core.apps.icons.LoadedIcon
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IconLoaderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val gateway = AndroidLauncherAppsGateway(context)
    private lateinit var records: List<ActivityRecord>

    @Before
    fun setUp() {
        records = gateway.profiles().flatMap { gateway.activities(it.serial) }
        context.cacheDir.resolve("icons").deleteRecursively()
    }

    @Test
    fun T3_1_iconsOfFiveSystemAppsBecomeAlphaMasksOrLetters() = runTest {
        val loader = IconLoader(context, LauncherIconSource(context, gateway))
        val system = records.filter { it.isSystem }.take(5).ifEmpty { records.take(5) }
        assertThat(system).isNotEmpty()
        system.forEach { record ->
            when (val icon = loader.load(IconKey(record.key, record.lastUpdateTime, 74))) {
                is LoadedIcon.Mask -> {
                    assertThat(icon.bitmap.config).isEqualTo(Bitmap.Config.ALPHA_8)
                    assertThat(icon.bitmap.width).isEqualTo(80)
                }

                LoadedIcon.Letter -> Unit
            }
        }
    }

    @Test
    fun T3_2_secondLoadIsMemoryHitAndDiskSurvivesEviction() = runTest {
        val loader = IconLoader(context, LauncherIconSource(context, gateway))
        val record = records.first()
        val key = IconKey(record.key, record.lastUpdateTime, 74)
        assertThat(loader.peek(key)).isNull()
        val first = loader.load(key)
        assertThat(loader.peek(key)).isSameInstanceAs(first)
        loader.onTrimMemory(android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        assertThat(loader.peek(key)).isNull()
        val fromDisk = loader.load(key)
        assertThat(fromDisk::class).isEqualTo(first::class)
    }

    @Test
    fun T3_2_memoryCacheRespectsByteLimit() = runTest {
        val limit = 20_000
        val loader = IconLoader(context, LauncherIconSource(context, gateway), maxMemoryBytes = limit)
        records.take(20).forEach { loader.load(IconKey(it.key, it.lastUpdateTime, 74)) }
        assertThat(loader.memoryBytes()).isAtMost(limit)
        assertThat(IconLoader.MAX_MEMORY_BYTES).isEqualTo(8 * 1024 * 1024)
    }

    @Test
    fun invalidateDropsPackageFromMemoryAndDisk() = runTest {
        val loader = IconLoader(context, LauncherIconSource(context, gateway))
        val record = records.first()
        val key = IconKey(record.key, record.lastUpdateTime, 74)
        loader.load(key)
        loader.invalidate(record.key.packageName, record.key.userSerial)
        assertThat(loader.peek(key)).isNull()
        val files = context.cacheDir.resolve("icons").listFiles().orEmpty()
        assertThat(files).isEmpty()
    }
}
