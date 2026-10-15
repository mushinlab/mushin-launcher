package com.mushinlab.launcher.core.data.repo

import androidx.room.withTransaction
import com.mushinlab.launcher.core.data.db.MushinDatabase
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.KeyRemap
import com.mushinlab.launcher.core.model.OverrideLimits

/** Keeps stored data consistent with installed packages. Each method runs as one transaction. */
class PackageDataCleaner(
    private val db: MushinDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /**
     * Deletes home items, recents, usage and learned drawings of a removed package; its overrides are only marked
     * orphaned so that a reinstall within the retention period restores them.
     */
    suspend fun onPackageRemoved(packageName: String, userSerial: Long) {
        db.withTransaction {
            db.spaceItemDao().deletePackage(packageName, userSerial)
            db.searchRecentDao().deletePackage(packageName, userSerial)
            db.appUsageDao().deletePackage(packageName, userSerial)
            db.spaceUsageDao().deletePackage(packageName, userSerial)
            db.gestureSampleDao().deletePackage(packageName, userSerial)
            db.appOverrideDao().markOrphaned(packageName, userSerial, clock())
        }
    }

    /** Restores orphaned overrides of a reinstalled package. */
    suspend fun onPackageAdded(packageName: String, userSerial: Long) {
        db.appOverrideDao().clearOrphaned(packageName, userSerial)
    }

    /**
     * Moves data from stored keys that no longer exist to the single remaining activity of the same package
     * (see [com.mushinlab.launcher.core.model.KeyRemap]). [available] is the full current app list.
     */
    suspend fun reconcile(available: Collection<AppKey>) {
        val dao = db.appKeyDao()
        val stored = dao.stored().map { AppKey(it.packageName, it.activityName, it.userSerial) }
        val moves = KeyRemap.moves(stored, available)
        if (moves.isEmpty()) return
        db.withTransaction {
            moves.forEach { (from, to) ->
                dao.mergeRecents(from.packageName, from.userSerial, from.activityName, to.activityName)
                dao.mergeUsage(from.packageName, from.userSerial, from.activityName, to.activityName)
                dao.mergeSpaceUsage(from.packageName, from.userSerial, from.activityName, to.activityName)
                dao.mergeSamples(from.packageName, from.userSerial, from.activityName, to.activityName)
                dao.moveBindings(from.packageName, from.userSerial, from.activityName, to.activityName)
                dao.moveSpaceItems(from.packageName, from.userSerial, from.activityName, to.activityName)
                dao.moveOverrides(from.packageName, from.userSerial, from.activityName, to.activityName)
                dao.moveRecents(from.packageName, from.userSerial, from.activityName, to.activityName)
                dao.moveUsage(from.packageName, from.userSerial, from.activityName, to.activityName)
                dao.moveSpaceUsage(from.packageName, from.userSerial, from.activityName, to.activityName)
                dao.moveSamples(from.packageName, from.userSerial, from.activityName, to.activityName)
                dao.deleteSpaceItems(from.packageName, from.userSerial, from.activityName)
                dao.deleteOverrides(from.packageName, from.userSerial, from.activityName)
                dao.deleteRecents(from.packageName, from.userSerial, from.activityName)
                dao.deleteUsage(from.packageName, from.userSerial, from.activityName)
                dao.deleteSpaceUsage(from.packageName, from.userSerial, from.activityName)
                dao.deleteSamples(from.packageName, from.userSerial, from.activityName)
            }
        }
    }

    /** Deletes overrides orphaned longer than the retention period; returns how many were deleted. */
    suspend fun purgeExpiredOverrides(): Int = db.appOverrideDao().purgeOrphanedBefore(
        clock() - OverrideLimits.ORPHAN_TTL_MS,
    )
}
