package com.bluecubelabs.env.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvironmentDao {
    @Query("SELECT * FROM environment ORDER BY sort_order, id")
    fun observeAll(): Flow<List<EnvironmentEntity>>

    @Query("SELECT * FROM environment ORDER BY sort_order, id")
    suspend fun getAll(): List<EnvironmentEntity>

    @Query("SELECT * FROM environment WHERE id = :id")
    suspend fun get(id: Long): EnvironmentEntity?

    @Insert
    suspend fun insert(entity: EnvironmentEntity): Long

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

    @Query("DELETE FROM environment WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM environment")
    suspend fun maxSortOrder(): Int
}

@Dao
interface EnvironmentItemDao {
    @Query("SELECT * FROM environment_item ORDER BY env_id, position")
    fun observeAll(): Flow<List<EnvironmentItemEntity>>

    @Query("SELECT * FROM environment_item WHERE env_id = :envId ORDER BY position")
    suspend fun get(envId: Long): List<EnvironmentItemEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<EnvironmentItemEntity>)

    @Query("DELETE FROM environment_item WHERE env_id = :envId")
    suspend fun deleteForEnv(envId: Long)

    @Query("DELETE FROM environment_item WHERE package_name = :packageName AND user_serial = :userSerial")
    suspend fun deletePackage(packageName: String, userSerial: Long)
}

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

    @Query(
        "UPDATE app_override SET orphaned_at = :at WHERE package_name = :packageName " +
            "AND user_serial = :userSerial AND orphaned_at IS NULL",
    )
    suspend fun markOrphaned(packageName: String, userSerial: Long, at: Long)

    @Query("UPDATE app_override SET orphaned_at = NULL WHERE package_name = :packageName AND user_serial = :userSerial")
    suspend fun clearOrphaned(packageName: String, userSerial: Long)

    @Query("DELETE FROM app_override WHERE orphaned_at IS NOT NULL AND orphaned_at < :before")
    suspend fun purgeOrphanedBefore(before: Long): Int
}

@Dao
interface SearchRecentDao {
    @Query("SELECT * FROM search_recent WHERE env_id = :envId ORDER BY last_launched_at DESC")
    fun observe(envId: Long): Flow<List<SearchRecentEntity>>

    @Query("SELECT * FROM search_recent ORDER BY env_id, last_launched_at DESC")
    fun observeAll(): Flow<List<SearchRecentEntity>>

    @Query(
        "SELECT * FROM search_recent WHERE env_id = :envId AND package_name = :packageName " +
            "AND activity_name = :activityName AND user_serial = :userSerial",
    )
    suspend fun get(envId: Long, packageName: String, activityName: String, userSerial: Long): SearchRecentEntity?

    @Upsert
    suspend fun upsert(entity: SearchRecentEntity)

    @Query(
        """
        DELETE FROM search_recent WHERE env_id = :envId AND rowid NOT IN (
            SELECT rowid FROM search_recent WHERE env_id = :envId ORDER BY last_launched_at DESC LIMIT :keep
        )
        """,
    )
    suspend fun trim(envId: Long, keep: Int)

    @Query("DELETE FROM search_recent WHERE env_id = :envId")
    suspend fun clear(envId: Long)

    @Query("DELETE FROM search_recent WHERE package_name = :packageName AND user_serial = :userSerial")
    suspend fun deletePackage(packageName: String, userSerial: Long)
}

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
}

@Dao
interface GestureBindingDao {
    @Query("SELECT * FROM gesture_binding")
    fun observeAll(): Flow<List<GestureBindingEntity>>

    @Upsert
    suspend fun upsert(entity: GestureBindingEntity)
}
