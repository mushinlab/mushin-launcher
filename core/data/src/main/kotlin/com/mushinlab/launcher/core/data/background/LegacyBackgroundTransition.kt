package com.mushinlab.launcher.core.data.background

import com.mushinlab.launcher.core.data.db.MushinDatabase
import com.mushinlab.launcher.core.data.db.toModel
import com.mushinlab.launcher.core.data.settings.SettingsRepository
import java.io.File

/**
 * One-time move of the single home background of versions before ADR-0020 into every space.
 *
 * When [legacyFile] exists it is copied to each space, and each space gets a brightness from the old global
 * visibility o (1 when it was never stored): -(1 - o) on a dark palette, +(1 - o) on a light one, blur 0. Only
 * then are the legacy file and the old setting deleted, so an interrupted run is repeated in full on the next
 * start. Without a legacy file nothing changes except removing a leftover old setting. Blocks on file I/O.
 */
class LegacyBackgroundTransition(
    private val legacyFile: File,
    private val store: SpaceBackgroundStore,
    db: MushinDatabase,
    private val settings: SettingsRepository,
) {
    private val spaceDao = db.spaceDao()

    /** Runs the transition; true when a legacy background was moved. */
    suspend fun run(): Boolean {
        val opacity = settings.legacyBackgroundOpacity()
        val moved = legacyFile.exists() && moveToEverySpace(opacity ?: 1f)
        if (!legacyFile.exists() && opacity != null) settings.dropLegacyBackgroundOpacity()
        return moved
    }

    private suspend fun moveToEverySpace(opacity: Float): Boolean {
        val spaces = spaceDao.getAll().map { it.toModel() }
        val copied = spaces.all { store.copy(it.id, legacyFile) }
        if (copied) {
            spaces.forEach { space ->
                spaceDao.setBackgroundLook(space.id, legacyBrightness(opacity, space.palette.isDark), blur = 0f)
            }
            legacyFile.delete()
        }
        return copied
    }

    companion object {
        /** Name of the single background file of versions before ADR-0020, in the app files directory. */
        const val LEGACY_FILE_NAME = "background.webp"
    }
}

internal fun legacyBrightness(opacity: Float, isDark: Boolean): Float {
    val hidden = 1f - opacity.coerceIn(0f, 1f)
    return if (isDark) -hidden else hidden
}
