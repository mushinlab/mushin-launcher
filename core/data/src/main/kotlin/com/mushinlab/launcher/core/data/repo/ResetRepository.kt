package com.mushinlab.launcher.core.data.repo

import androidx.room.withTransaction
import com.mushinlab.launcher.core.data.db.MushinDatabase

/** Deletes every space, list, override and learning record and seeds a fresh database. */
class ResetRepository(private val database: MushinDatabase, private val clock: () -> Long = System::currentTimeMillis) {
    suspend fun reset(names: MushinDatabase.SeedNames) {
        database.withTransaction {
            val db = database
            db.searchRecentDao().clearAll()
            db.appUsageDao().deleteAll()
            db.spaceUsageDao().deleteAll()
            db.spaceLaunchDayDao().deleteAll()
            db.gestureSampleDao().deleteAll()
            db.gestureBindingDao().clearAll()
            db.appOverrideDao().clearAll()
            db.spaceItemDao().clearAllItems()
            db.spaceItemDao().clearAllFolderItems()
            db.spaceItemDao().clearAllFolders()
            db.spaceDao().clearAll()
        }
        MushinDatabase.SeedCallback(names).seed(database.openHelper.writableDatabase, clock())
        database.invalidationTracker.refreshAsync()
    }
}
