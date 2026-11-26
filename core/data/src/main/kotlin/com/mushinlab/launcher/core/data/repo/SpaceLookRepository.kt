package com.mushinlab.launcher.core.data.repo

import androidx.room.withTransaction
import com.mushinlab.launcher.core.data.db.MushinDatabase
import com.mushinlab.launcher.core.model.SpaceBackground
import com.mushinlab.launcher.core.model.SpaceType

/**
 * Background look (ADR-0020, ADR-0026) and shared theme (ADR-0054) of spaces, stored in the `environment` table
 * and read through [SpaceRepository.spaces]. The images themselves are files kept by the background store.
 */
class SpaceLookRepository(private val db: MushinDatabase) {
    private val spaceDao = db.spaceDao()

    /**
     * Sets the background look of a space, limited by [SpaceBackground]: brightness -1..1 and blur 0..1 (NaN
     * becomes 0), row opacity 0.2..1 (NaN becomes [SpaceBackground.ROW_ALPHA_DEFAULT]). Brightness and blur 0
     * with the default row opacity reset the look.
     */
    suspend fun setBackgroundLook(
        id: Long,
        brightness: Float,
        blur: Float,
        rowAlpha: Float = SpaceBackground.ROW_ALPHA_DEFAULT,
    ) {
        spaceDao.setBackgroundLook(
            id,
            SpaceBackground.brightness(brightness),
            SpaceBackground.blur(blur),
            SpaceBackground.rowAlpha(rowAlpha),
        )
    }

    /**
     * Gives every space except the private one the palette and colored icons of space [fromId] (ADR-0054);
     * nothing changes when [fromId] is missing or private.
     */
    suspend fun shareTheme(fromId: Long) {
        db.withTransaction {
            val source = spaceDao.get(fromId)?.takeIf { it.type != SpaceType.PRIVATE.name } ?: return@withTransaction
            spaceDao.getAll()
                .filter { it.id != source.id && it.type != SpaceType.PRIVATE.name }
                .forEach { target ->
                    with(source) {
                        spaceDao.setPalette(
                            target.id, palettePreset, background, zebraA, zebraB, textPrimary, textSecondary, accent,
                            iconTint, divider, surface, keyBackground, keyPressed, isDark,
                        )
                    }
                    spaceDao.setColoredIcons(target.id, source.coloredIcons)
                }
        }
    }
}
