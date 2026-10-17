package com.mushinlab.launcher.core.data.repo

import com.mushinlab.launcher.core.data.db.MushinDatabase
import com.mushinlab.launcher.core.model.SpaceBackground

/**
 * Background look of spaces (ADR-0020), stored in the `environment` table and read through
 * [SpaceRepository.spaces]. The images themselves are files kept by the background store.
 */
class SpaceLookRepository(db: MushinDatabase) {
    private val spaceDao = db.spaceDao()

    /**
     * Sets the background look of a space, limited by [SpaceBackground]: brightness -1..1 and blur 0..1; NaN
     * becomes 0. Setting both to 0 resets the look.
     */
    suspend fun setBackgroundLook(id: Long, brightness: Float, blur: Float) {
        spaceDao.setBackgroundLook(id, SpaceBackground.brightness(brightness), SpaceBackground.blur(blur))
    }
}
