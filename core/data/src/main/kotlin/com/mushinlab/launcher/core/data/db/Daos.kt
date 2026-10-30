package com.mushinlab.launcher.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Spaces, stored in the `environment` table. */
@Dao
interface SpaceDao {
    @Query("SELECT * FROM environment ORDER BY sort_order, id")
    fun observeAll(): Flow<List<SpaceEntity>>

    @Query("SELECT * FROM environment ORDER BY sort_order, id")
    suspend fun getAll(): List<SpaceEntity>

    @Query("SELECT * FROM environment WHERE id = :id")
    suspend fun get(id: Long): SpaceEntity?

    @Insert
    suspend fun insert(entity: SpaceEntity): Long

    @Query("UPDATE environment SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query(
        """
        UPDATE environment SET palette_preset = :preset,
            c_background = :background, c_zebra_a = :zebraA, c_zebra_b = :zebraB,
            c_text_primary = :textPrimary, c_text_secondary = :textSecondary, c_accent = :accent,
            c_icon_tint = :iconTint, c_divider = :divider, c_surface = :surface,
            c_key_background = :keyBackground, c_key_pressed = :keyPressed, is_dark = :isDark
        WHERE id = :id
        """,
    )
    suspend fun setPalette(
        id: Long,
        preset: String,
        background: Int,
        zebraA: Int,
        zebraB: Int,
        textPrimary: Int,
        textSecondary: Int,
        accent: Int,
        iconTint: Int,
        divider: Int,
        surface: Int,
        keyBackground: Int,
        keyPressed: Int,
        isDark: Boolean,
    )

    @Query("UPDATE environment SET colored_icons = :enabled WHERE id = :id")
    suspend fun setColoredIcons(id: Long, enabled: Boolean)

    @Query("UPDATE environment SET list_offset = :dp WHERE id = :id")
    suspend fun setListOffset(id: Long, dp: Int?)

    @Query(
        "UPDATE environment SET bg_brightness = :brightness, bg_blur = :blur, bg_row_alpha = :rowAlpha WHERE id = :id",
    )
    suspend fun setBackgroundLook(id: Long, brightness: Float, blur: Float, rowAlpha: Float)

    @Query("UPDATE environment SET hint_at = :at WHERE id = :id")
    suspend fun setHintAt(id: Long, at: Long?)

    @Query("UPDATE environment SET hint_at = NULL")
    suspend fun clearHintTimes()

    @Query("DELETE FROM environment WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM environment")
    suspend fun maxSortOrder(): Int
}

/**
 * Home lists of spaces, stored in `environment_item` with a position per row, and home folders with their apps
 * in `folder` and `folder_item` (ADR-0035).
 */
@Dao
interface SpaceItemDao {
    @Query("SELECT * FROM environment_item ORDER BY env_id, position")
    fun observeAll(): Flow<List<SpaceItemEntity>>

    @Query("SELECT * FROM environment_item WHERE env_id = :spaceId ORDER BY position")
    suspend fun get(spaceId: Long): List<SpaceItemEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<SpaceItemEntity>)

    @Query("DELETE FROM environment_item WHERE env_id = :spaceId")
    suspend fun deleteForSpace(spaceId: Long)

    @Query("DELETE FROM environment_item WHERE package_name = :packageName AND user_serial = :userSerial")
    suspend fun deletePackage(packageName: String, userSerial: Long)

    @Query("SELECT * FROM folder ORDER BY id")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folder_item ORDER BY folder_id, position")
    fun observeFolderItems(): Flow<List<FolderItemEntity>>

    @Query("SELECT * FROM folder WHERE id = :id")
    suspend fun getFolder(id: Long): FolderEntity?

    @Insert
    suspend fun insertFolder(entity: FolderEntity): Long

    @Query("UPDATE folder SET name = :name WHERE id = :id")
    suspend fun renameFolder(id: Long, name: String)

    @Query("DELETE FROM folder WHERE id = :id")
    suspend fun deleteFolder(id: Long)

    @Query("SELECT * FROM folder_item WHERE folder_id = :folderId ORDER BY position")
    suspend fun folderItems(folderId: Long): List<FolderItemEntity>

    @Query("DELETE FROM folder_item WHERE folder_id = :folderId")
    suspend fun deleteFolderItems(folderId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolderItems(items: List<FolderItemEntity>)

    @Query("DELETE FROM folder_item WHERE package_name = :packageName AND user_serial = :userSerial")
    suspend fun deleteFolderPackage(packageName: String, userSerial: Long)
}

/** Per-app user overrides; rows of removed packages are marked orphaned and purged later. */
@Dao
interface AppOverrideDao {
    @Query("SELECT * FROM app_override")
    fun observeAll(): Flow<List<AppOverrideEntity>>

    @Query(
        "SELECT * FROM app_override WHERE package_name = :packageName AND activity_name = :activityName " +
            "AND user_serial = :userSerial",
    )
    suspend fun get(packageName: String, activityName: String, userSerial: Long): AppOverrideEntity?

    @Upsert
    suspend fun upsert(entity: AppOverrideEntity)

    @Query(
        "DELETE FROM app_override WHERE package_name = :packageName AND activity_name = :activityName " +
            "AND user_serial = :userSerial",
    )
    suspend fun delete(packageName: String, activityName: String, userSerial: Long)

    /** Marks overrides of a removed package with the removal time [at], keeping an earlier mark. */
    @Query(
        "UPDATE app_override SET orphaned_at = :at WHERE package_name = :packageName " +
            "AND user_serial = :userSerial AND orphaned_at IS NULL",
    )
    suspend fun markOrphaned(packageName: String, userSerial: Long, at: Long)

    @Query("UPDATE app_override SET orphaned_at = NULL WHERE package_name = :packageName AND user_serial = :userSerial")
    suspend fun clearOrphaned(packageName: String, userSerial: Long)

    /** Deletes overrides orphaned before [before] (epoch ms); returns the number of rows deleted. */
    @Query("DELETE FROM app_override WHERE orphaned_at IS NOT NULL AND orphaned_at < :before")
    suspend fun purgeOrphanedBefore(before: Long): Int
}

/** Apps launched from search, per space. */
@Dao
interface SearchRecentDao {
    @Query("SELECT * FROM search_recent WHERE env_id = :spaceId ORDER BY last_launched_at DESC")
    fun observe(spaceId: Long): Flow<List<SearchRecentEntity>>

    @Query("SELECT * FROM search_recent ORDER BY env_id, last_launched_at DESC")
    fun observeAll(): Flow<List<SearchRecentEntity>>

    @Query(
        "SELECT * FROM search_recent WHERE env_id = :spaceId AND package_name = :packageName " +
            "AND activity_name = :activityName AND user_serial = :userSerial",
    )
    suspend fun get(spaceId: Long, packageName: String, activityName: String, userSerial: Long): SearchRecentEntity?

    @Upsert
    suspend fun upsert(entity: SearchRecentEntity)

    /** Keeps only the [keep] most recent rows of a space. */
    @Query(
        """
        DELETE FROM search_recent WHERE env_id = :spaceId AND rowid NOT IN (
            SELECT rowid FROM search_recent WHERE env_id = :spaceId ORDER BY last_launched_at DESC LIMIT :keep
        )
        """,
    )
    suspend fun trim(spaceId: Long, keep: Int)

    @Query("DELETE FROM search_recent WHERE env_id = :spaceId")
    suspend fun clear(spaceId: Long)

    @Query("DELETE FROM search_recent")
    suspend fun clearAll()

    @Query("DELETE FROM search_recent WHERE package_name = :packageName AND user_serial = :userSerial")
    suspend fun deletePackage(packageName: String, userSerial: Long)
}

/** Global launch statistics per app. */
@Dao
interface AppUsageDao {
    @Query("SELECT * FROM app_usage")
    fun observeAll(): Flow<List<AppUsageEntity>>

    @Query(
        "SELECT * FROM app_usage WHERE package_name = :packageName AND activity_name = :activityName " +
            "AND user_serial = :userSerial",
    )
    suspend fun get(packageName: String, activityName: String, userSerial: Long): AppUsageEntity?

    @Upsert
    suspend fun upsert(entity: AppUsageEntity)

    @Query("DELETE FROM app_usage WHERE package_name = :packageName AND user_serial = :userSerial")
    suspend fun deletePackage(packageName: String, userSerial: Long)

    @Query("DELETE FROM app_usage")
    suspend fun deleteAll()
}

/** Launch statistics per app and space, with the time a suggestion was dismissed. */
@Dao
interface SpaceUsageDao {
    @Query("SELECT * FROM space_usage")
    fun observeAll(): Flow<List<SpaceUsageEntity>>

    @Query(
        "SELECT * FROM space_usage WHERE env_id = :spaceId AND package_name = :packageName " +
            "AND activity_name = :activityName AND user_serial = :userSerial",
    )
    suspend fun get(spaceId: Long, packageName: String, activityName: String, userSerial: Long): SpaceUsageEntity?

    @Upsert
    suspend fun upsert(entity: SpaceUsageEntity)

    /** Records that a suggestion to add the app to the space was rejected at [at]. */
    @Query(
        "UPDATE space_usage SET dismissed_at = :at WHERE env_id = :spaceId AND package_name = :packageName " +
            "AND activity_name = :activityName AND user_serial = :userSerial",
    )
    suspend fun dismiss(spaceId: Long, packageName: String, activityName: String, userSerial: Long, at: Long)

    @Query("DELETE FROM space_usage WHERE package_name = :packageName AND user_serial = :userSerial")
    suspend fun deletePackage(packageName: String, userSerial: Long)

    @Query("DELETE FROM space_usage")
    suspend fun deleteAll()
}

/** Local calendar days (epoch days) on which an app was launched in a space. */
@Dao
interface SpaceLaunchDayDao {
    @Query("SELECT * FROM space_launch_day")
    fun observeAll(): Flow<List<SpaceLaunchDayEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: SpaceLaunchDayEntity)

    /** Deletes days before [day] in every space. */
    @Query("DELETE FROM space_launch_day WHERE day < :day")
    suspend fun deleteBefore(day: Long)

    @Query("DELETE FROM space_launch_day WHERE package_name = :packageName AND user_serial = :userSerial")
    suspend fun deletePackage(packageName: String, userSerial: Long)

    @Query("DELETE FROM space_launch_day")
    suspend fun deleteAll()
}

/** Home-screen swipe bindings, one row per direction. */
@Dao
interface GestureBindingDao {
    @Query("SELECT * FROM gesture_binding")
    fun observeAll(): Flow<List<GestureBindingEntity>>

    @Upsert
    suspend fun upsert(entity: GestureBindingEntity)
}

/** Learned keyboard drawings ([com.mushinlab.launcher.core.model.LearnedGesture]). */
@Dao
interface GestureSampleDao {
    @Query("SELECT * FROM gesture_sample")
    fun observeAll(): Flow<List<GestureSampleEntity>>

    @Query("SELECT * FROM gesture_sample")
    suspend fun getAll(): List<GestureSampleEntity>

    @Insert
    suspend fun insertAll(samples: List<GestureSampleEntity>)

    @Query("DELETE FROM gesture_sample")
    suspend fun deleteAll()

    /** Replaces all samples atomically. */
    @Transaction
    suspend fun replaceAll(samples: List<GestureSampleEntity>) {
        deleteAll()
        insertAll(samples)
    }

    @Query("DELETE FROM gesture_sample WHERE package_name = :packageName AND user_serial = :userSerial")
    suspend fun deletePackage(packageName: String, userSerial: Long)
}

private const val FROM_KEY = "WHERE package_name = :packageName AND activity_name = :from AND user_serial = :userSerial"
private const val TO_KEY = "WHERE package_name = :packageName AND activity_name = :to AND user_serial = :userSerial"
private const val OLD_ROW = "o.package_name = :packageName AND o.activity_name = :from AND o.user_serial = :userSerial"

/** An app key referenced by any table, as returned by [AppKeyDao.stored]. */
data class StoredAppKey(
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String,
    @ColumnInfo(name = "user_serial") val userSerial: Long,
)

/**
 * Moves stored data from one activity of a package to another when an app renames its launcher activity.
 *
 * `merge*` adds the old row into an existing new row, `move*` re-keys old rows where no new row exists and
 * `delete*` removes what is left of the old key. Run them in this order inside one transaction.
 */
@Dao
interface AppKeyDao {
    /** Every distinct app key referenced by any table. */
    @Query(
        "SELECT package_name, activity_name, user_serial FROM environment_item " +
            "UNION SELECT package_name, activity_name, user_serial FROM app_override " +
            "UNION SELECT package_name, activity_name, user_serial FROM search_recent " +
            "UNION SELECT package_name, activity_name, user_serial FROM app_usage " +
            "UNION SELECT package_name, activity_name, user_serial FROM space_usage " +
            "UNION SELECT package_name, activity_name, user_serial FROM space_launch_day " +
            "UNION SELECT package_name, activity_name, user_serial FROM gesture_sample " +
            "UNION SELECT package_name, activity_name, user_serial FROM folder_item " +
            "UNION SELECT package_name, activity_name, user_serial FROM gesture_binding " +
            "WHERE package_name IS NOT NULL AND activity_name IS NOT NULL AND user_serial IS NOT NULL",
    )
    suspend fun stored(): List<StoredAppKey>

    @Query(
        "UPDATE search_recent SET " +
            "launch_count = launch_count + (SELECT o.launch_count FROM search_recent AS o " +
            "WHERE o.env_id = search_recent.env_id AND " + OLD_ROW + "), " +
            "last_launched_at = MAX(last_launched_at, (SELECT o.last_launched_at FROM search_recent AS o " +
            "WHERE o.env_id = search_recent.env_id AND " + OLD_ROW + ")) " +
            TO_KEY + " AND env_id IN (SELECT env_id FROM search_recent " + FROM_KEY + ")",
    )
    suspend fun mergeRecents(packageName: String, userSerial: Long, from: String, to: String)

    @Query(
        "UPDATE app_usage SET " +
            "launch_count = launch_count + (SELECT o.launch_count FROM app_usage AS o WHERE " + OLD_ROW + "), " +
            "last_launched_at = MAX(last_launched_at, " +
            "(SELECT o.last_launched_at FROM app_usage AS o WHERE " + OLD_ROW + ")), " +
            "decayed_score = MAX(decayed_score, " +
            "(SELECT o.decayed_score FROM app_usage AS o WHERE " + OLD_ROW + ")) " +
            TO_KEY + " AND EXISTS (SELECT 1 FROM app_usage " + FROM_KEY + ")",
    )
    suspend fun mergeUsage(packageName: String, userSerial: Long, from: String, to: String)

    @Query(
        "UPDATE space_usage SET " +
            "launch_count = launch_count + (SELECT o.launch_count FROM space_usage AS o " +
            "WHERE o.env_id = space_usage.env_id AND " + OLD_ROW + "), " +
            "last_launched_at = MAX(last_launched_at, (SELECT o.last_launched_at FROM space_usage AS o " +
            "WHERE o.env_id = space_usage.env_id AND " + OLD_ROW + ")), " +
            "decayed_score = MAX(decayed_score, (SELECT o.decayed_score FROM space_usage AS o " +
            "WHERE o.env_id = space_usage.env_id AND " + OLD_ROW + ")) " +
            TO_KEY + " AND env_id IN (SELECT env_id FROM space_usage " + FROM_KEY + ")",
    )
    suspend fun mergeSpaceUsage(packageName: String, userSerial: Long, from: String, to: String)

    @Query(
        "UPDATE gesture_sample SET weight = weight + " +
            "(SELECT o.weight FROM gesture_sample AS o WHERE o.path = gesture_sample.path AND " + OLD_ROW + ") " +
            TO_KEY + " AND path IN (SELECT path FROM gesture_sample " + FROM_KEY + ")",
    )
    suspend fun mergeSamples(packageName: String, userSerial: Long, from: String, to: String)

    @Query("UPDATE gesture_binding SET activity_name = :to " + FROM_KEY)
    suspend fun moveBindings(packageName: String, userSerial: Long, from: String, to: String)

    @Query("UPDATE OR IGNORE environment_item SET activity_name = :to " + FROM_KEY)
    suspend fun moveSpaceItems(packageName: String, userSerial: Long, from: String, to: String)

    @Query("DELETE FROM environment_item " + FROM_KEY)
    suspend fun deleteSpaceItems(packageName: String, userSerial: Long, from: String)

    @Query("UPDATE OR IGNORE folder_item SET activity_name = :to " + FROM_KEY)
    suspend fun moveFolderItems(packageName: String, userSerial: Long, from: String, to: String)

    @Query("DELETE FROM folder_item " + FROM_KEY)
    suspend fun deleteFolderItems(packageName: String, userSerial: Long, from: String)

    @Query("UPDATE OR IGNORE app_override SET activity_name = :to " + FROM_KEY)
    suspend fun moveOverrides(packageName: String, userSerial: Long, from: String, to: String)

    @Query("DELETE FROM app_override " + FROM_KEY)
    suspend fun deleteOverrides(packageName: String, userSerial: Long, from: String)

    @Query("UPDATE OR IGNORE search_recent SET activity_name = :to " + FROM_KEY)
    suspend fun moveRecents(packageName: String, userSerial: Long, from: String, to: String)

    @Query("DELETE FROM search_recent " + FROM_KEY)
    suspend fun deleteRecents(packageName: String, userSerial: Long, from: String)

    @Query("UPDATE OR IGNORE app_usage SET activity_name = :to " + FROM_KEY)
    suspend fun moveUsage(packageName: String, userSerial: Long, from: String, to: String)

    @Query("DELETE FROM app_usage " + FROM_KEY)
    suspend fun deleteUsage(packageName: String, userSerial: Long, from: String)

    @Query("UPDATE OR IGNORE space_usage SET activity_name = :to " + FROM_KEY)
    suspend fun moveSpaceUsage(packageName: String, userSerial: Long, from: String, to: String)

    @Query("DELETE FROM space_usage " + FROM_KEY)
    suspend fun deleteSpaceUsage(packageName: String, userSerial: Long, from: String)

    @Query("UPDATE OR IGNORE space_launch_day SET activity_name = :to " + FROM_KEY)
    suspend fun moveLaunchDays(packageName: String, userSerial: Long, from: String, to: String)

    @Query("DELETE FROM space_launch_day " + FROM_KEY)
    suspend fun deleteLaunchDays(packageName: String, userSerial: Long, from: String)

    @Query("UPDATE OR IGNORE gesture_sample SET activity_name = :to " + FROM_KEY)
    suspend fun moveSamples(packageName: String, userSerial: Long, from: String, to: String)

    @Query("DELETE FROM gesture_sample " + FROM_KEY)
    suspend fun deleteSamples(packageName: String, userSerial: Long, from: String)
}
