package com.bluecubelabs.env.core.data.repo

import androidx.room.withTransaction
import com.bluecubelabs.env.core.data.db.EnvDatabase
import com.bluecubelabs.env.core.model.EnvLimits

class PackageDataCleaner(
    private val db: EnvDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun onPackageRemoved(packageName: String, userSerial: Long) {
        db.withTransaction {
            db.environmentItemDao().deletePackage(packageName, userSerial)
            db.searchRecentDao().deletePackage(packageName, userSerial)
            db.appUsageDao().deletePackage(packageName, userSerial)
            db.gestureSampleDao().deletePackage(packageName, userSerial)
            db.appOverrideDao().markOrphaned(packageName, userSerial, clock())
        }
    }

    suspend fun onPackageAdded(packageName: String, userSerial: Long) {
        db.appOverrideDao().clearOrphaned(packageName, userSerial)
    }

    suspend fun purgeExpiredOverrides(): Int = db.appOverrideDao().purgeOrphanedBefore(
        clock() - EnvLimits.ORPHAN_TTL_MS,
    )
}
