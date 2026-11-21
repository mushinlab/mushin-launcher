package com.mushinlab.launcher.core.data.repo

import androidx.room.withTransaction
import com.mushinlab.launcher.core.data.db.MushinDatabase
import com.mushinlab.launcher.core.data.db.SpaceEntity
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.model.SpaceType

/** Creates and finds the single private space. */
class PrivateSpaceRepository(
    private val db: MushinDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** Returns the id of the private space, creating it with [preset] and [name] when absent. */
    suspend fun ensure(name: String, preset: PalettePreset): Long = db.withTransaction {
        val dao = db.spaceDao()
        dao.getAll().firstOrNull { it.type == SpaceType.PRIVATE.name }?.id ?: run {
            val palette = Palettes.of(preset)
            dao.insert(
                SpaceEntity(
                    name = name,
                    type = SpaceType.PRIVATE.name,
                    sortOrder = dao.maxSortOrder() + 1,
                    palettePreset = preset.name,
                    background = palette.background,
                    zebraA = palette.zebraA,
                    zebraB = palette.zebraB,
                    textPrimary = palette.textPrimary,
                    textSecondary = palette.textSecondary,
                    accent = palette.accent,
                    iconTint = palette.iconTint,
                    divider = palette.divider,
                    surface = palette.surface,
                    keyBackground = palette.keyBackground,
                    keyPressed = palette.keyPressed,
                    isDark = palette.isDark,
                    createdAt = clock(),
                    coloredIcons = true,
                ),
            )
        }
    }
}
