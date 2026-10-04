package com.bluecubelabs.env.core.data.repo

import androidx.room.withTransaction
import com.bluecubelabs.env.core.data.db.EnvDatabase
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.EnvLimits
import com.bluecubelabs.env.core.model.KeyRemap

class PackageDataCleaner(
    private val db: EnvDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun onPackageRemoved(packageName: String, userSerial: Long) {
        db.withTransaction {
            db.environmentItemDao().deletePackage(packageName, userSerial)
            db.searchRecentDao().deletePackage(packageName, userSerial)
            db.appUsageDao().deletePackage(packageName, userSerial)
            db.spaceUsageDao().deletePackage(packageName, userSerial)
            db.gestureSampleDao().deletePackage(packageName, userSerial)
            db.appOverrideDao().markOrphaned(packageName, userSerial, clock())
        }
    }

    suspend fun onPackageAdded(packageName: String, userSerial: Long) {
        db.appOverrideDao().clearOrphaned(packageName, userSerial)
    }

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
                dao.moveEnvironmentItems(from.packageName, from.userSerial, from.activityName, to.activityName)
                dao.moveOverrides(from.packageName, from.userSerial, from.activityName, to.activityName)
                dao.moveRecents(from.packageName, from.userSerial, from.activityName, to.activityName)
                dao.moveUsage(from.packageName, from.userSerial, from.activityName, to.activityName)
                dao.moveSpaceUsage(from.packageName, from.userSerial, from.activityName, to.activityName)
                dao.moveSamples(from.packageName, from.userSerial, from.activityName, to.activityName)
                dao.deleteEnvironmentItems(from.packageName, from.userSerial, from.activityName)
                dao.deleteOverrides(from.packageName, from.userSerial, from.activityName)
                dao.deleteRecents(from.packageName, from.userSerial, from.activityName)
                dao.deleteUsage(from.packageName, from.userSerial, from.activityName)
                dao.deleteSpaceUsage(from.packageName, from.userSerial, from.activityName)
                dao.deleteSamples(from.packageName, from.userSerial, from.activityName)
            }
        }
    }

    suspend fun purgeExpiredOverrides(): Int = db.appOverrideDao().purgeOrphanedBefore(
        clock() - EnvLimits.ORPHAN_TTL_MS,
    )
}
