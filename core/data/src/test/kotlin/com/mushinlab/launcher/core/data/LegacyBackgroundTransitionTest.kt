package com.mushinlab.launcher.core.data

import android.graphics.Bitmap
import android.graphics.Color
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.background.LegacyBackgroundTransition
import com.mushinlab.launcher.core.data.background.SpaceBackgroundStore
import com.mushinlab.launcher.core.data.db.toModel
import com.mushinlab.launcher.core.data.repo.SpaceLookRepository
import com.mushinlab.launcher.core.data.repo.SpaceRepository
import com.mushinlab.launcher.core.data.settings.SettingsRepository
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.SpaceType
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LegacyBackgroundTransitionTest {
    @get:Rule
    val db = DbTestRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val opacityKey = floatPreferencesKey("background_opacity")

    private class Setup(
        val legacy: File,
        val dir: File,
        val store: SpaceBackgroundStore,
        val looks: SpaceLookRepository,
        val settings: SettingsRepository,
        val transition: LegacyBackgroundTransition,
    )

    private suspend fun setup(scope: CoroutineScope, opacity: Float?, withLegacyFile: Boolean = true): Setup {
        val files = tmp.newFolder()
        val legacy = File(files, LegacyBackgroundTransition.LEGACY_FILE_NAME)
        if (withLegacyFile) {
            val bitmap = Bitmap.createBitmap(40, 80, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
            legacy.outputStream().use { bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 90, it) }
        }
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { File(files, "settings.preferences_pb") }
        if (opacity != null) dataStore.edit { it[opacityKey] = opacity }
        val spaces = SpaceRepository(db.db, scope, db.clock)
        val work = all().first { it.type == SpaceType.WORK }
        spaces.setPalette(work.id, PalettePreset.PAPER)
        val settings = SettingsRepository(dataStore, scope)
        val dir = File(files, "backgrounds")
        val store = SpaceBackgroundStore(dir, 40, 80)
        val transition = LegacyBackgroundTransition(legacy, store, db.db, settings)
        return Setup(legacy, dir, store, SpaceLookRepository(db.db), settings, transition)
    }

    private suspend fun all() = db.db.spaceDao().getAll().map { it.toModel() }

    @Test
    fun legacyBackgroundGoesToEverySpaceWithBrightnessFromTheOldVisibility() = runTest {
        val s = setup(backgroundScope, opacity = 0.75f)
        val bytes = s.legacy.readBytes()

        assertThat(s.transition.run()).isTrue()

        val spaces = all()
        spaces.forEach { space ->
            assertThat(File(s.dir, "space_${space.id}.webp").readBytes()).isEqualTo(bytes)
            assertThat(space.backgroundBlur).isEqualTo(0f)
        }
        assertThat(spaces.first { it.type == SpaceType.NORMAL }.backgroundBrightness).isEqualTo(-0.25f)
        assertThat(spaces.first { it.type == SpaceType.WORK }.backgroundBrightness).isEqualTo(0.25f)
        assertThat(s.store.versions.value.keys).containsExactlyElementsIn(spaces.map { it.id })
        assertThat(s.legacy.exists()).isFalse()
        assertThat(s.settings.legacyBackgroundOpacity()).isNull()
    }

    @Test
    fun runningAgainChangesNothing() = runTest {
        val s = setup(backgroundScope, opacity = 0.5f)
        s.transition.run()
        val normal = all().first { it.type == SpaceType.NORMAL }
        s.looks.setBackgroundLook(normal.id, brightness = 0.6f, blur = 0.4f)
        s.store.delete(normal.id)

        assertThat(s.transition.run()).isFalse()

        val after = all().first { it.id == normal.id }
        assertThat(after.backgroundBrightness).isEqualTo(0.6f)
        assertThat(after.backgroundBlur).isEqualTo(0.4f)
        assertThat(File(s.dir, "space_${normal.id}.webp").exists()).isFalse()
    }

    @Test
    fun missingOldVisibilityMeansAFullyVisibleImage() = runTest {
        val s = setup(backgroundScope, opacity = null)

        assertThat(s.transition.run()).isTrue()

        assertThat(all().map { it.backgroundBrightness }).containsExactly(0f, 0f)
    }

    @Test
    fun withoutALegacyFileOnlyTheOldSettingIsRemoved() = runTest {
        val s = setup(backgroundScope, opacity = 0.2f, withLegacyFile = false)

        assertThat(s.transition.run()).isFalse()

        assertThat(s.dir.exists()).isFalse()
        assertThat(all().all { it.backgroundBrightness == 0f }).isTrue()
        assertThat(s.settings.legacyBackgroundOpacity()).isNull()
        assertThat(s.settings.settings.first()).isNotNull()
    }
}
