package com.mushinlab.launcher.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "environment",
    indices = [Index(value = ["name"], unique = true)],
)
data class SpaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name", collate = ColumnInfo.NOCASE) val name: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "palette_preset") val palettePreset: String,
    @ColumnInfo(name = "c_background") val background: Int,
    @ColumnInfo(name = "c_zebra_a") val zebraA: Int,
    @ColumnInfo(name = "c_zebra_b") val zebraB: Int,
    @ColumnInfo(name = "c_text_primary") val textPrimary: Int,
    @ColumnInfo(name = "c_text_secondary") val textSecondary: Int,
    @ColumnInfo(name = "c_accent") val accent: Int,
    @ColumnInfo(name = "c_icon_tint") val iconTint: Int,
    @ColumnInfo(name = "c_divider") val divider: Int,
    @ColumnInfo(name = "c_surface") val surface: Int,
    @ColumnInfo(name = "c_key_background") val keyBackground: Int,
    @ColumnInfo(name = "c_key_pressed") val keyPressed: Int,
    @ColumnInfo(name = "is_dark") val isDark: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "colored_icons", defaultValue = "0") val coloredIcons: Boolean = false,
    @ColumnInfo(name = "list_offset") val listOffset: Int? = null,
    @ColumnInfo(name = "bg_brightness", defaultValue = "0") val backgroundBrightness: Float = 0f,
    @ColumnInfo(name = "bg_blur", defaultValue = "0") val backgroundBlur: Float = 0f,
    @ColumnInfo(name = "hint_at") val hintAt: Long? = null,
)

@Entity(
    tableName = "environment_item",
    primaryKeys = ["env_id", "package_name", "activity_name", "user_serial"],
    foreignKeys = [
        ForeignKey(
            entity = SpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["env_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["env_id", "position"], name = "idx_env_item_order")],
)
data class SpaceItemEntity(
    @ColumnInfo(name = "env_id") val spaceId: Long,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String,
    @ColumnInfo(name = "user_serial") val userSerial: Long,
    @ColumnInfo(name = "position") val position: Int,
)

@Entity(
    tableName = "app_override",
    primaryKeys = ["package_name", "activity_name", "user_serial"],
)
data class AppOverrideEntity(
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String,
    @ColumnInfo(name = "user_serial") val userSerial: Long,
    @ColumnInfo(name = "custom_label") val customLabel: String?,
    @ColumnInfo(name = "hidden", defaultValue = "0") val hidden: Boolean,
    @ColumnInfo(name = "orphaned_at") val orphanedAt: Long?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "search_recent",
    primaryKeys = ["env_id", "package_name", "activity_name", "user_serial"],
    foreignKeys = [
        ForeignKey(
            entity = SpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["env_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["env_id", "last_launched_at"], name = "idx_recent_env_time")],
)
data class SearchRecentEntity(
    @ColumnInfo(name = "env_id") val spaceId: Long,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String,
    @ColumnInfo(name = "user_serial") val userSerial: Long,
    @ColumnInfo(name = "last_launched_at") val lastLaunchedAt: Long,
    @ColumnInfo(name = "launch_count", defaultValue = "1") val launchCount: Int,
)

@Entity(
    tableName = "app_usage",
    primaryKeys = ["package_name", "activity_name", "user_serial"],
)
data class AppUsageEntity(
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String,
    @ColumnInfo(name = "user_serial") val userSerial: Long,
    @ColumnInfo(name = "launch_count") val launchCount: Int,
    @ColumnInfo(name = "last_launched_at") val lastLaunchedAt: Long,
    @ColumnInfo(name = "decayed_score") val decayedScore: Double,
)

@Entity(
    tableName = "space_usage",
    primaryKeys = ["env_id", "package_name", "activity_name", "user_serial"],
    foreignKeys = [
        ForeignKey(
            entity = SpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["env_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SpaceUsageEntity(
    @ColumnInfo(name = "env_id") val spaceId: Long,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String,
    @ColumnInfo(name = "user_serial") val userSerial: Long,
    @ColumnInfo(name = "launch_count") val launchCount: Int,
    @ColumnInfo(name = "last_launched_at") val lastLaunchedAt: Long,
    @ColumnInfo(name = "decayed_score") val decayedScore: Double,
    @ColumnInfo(name = "dismissed_at") val dismissedAt: Long?,
)

@Entity(
    tableName = "space_launch_day",
    primaryKeys = ["space_id", "package_name", "activity_name", "user_serial", "day"],
    foreignKeys = [
        ForeignKey(
            entity = SpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["space_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["space_id", "day"])],
)
data class SpaceLaunchDayEntity(
    @ColumnInfo(name = "space_id") val spaceId: Long,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String,
    @ColumnInfo(name = "user_serial") val userSerial: Long,
    @ColumnInfo(name = "day") val day: Long,
)

@Entity(tableName = "gesture_binding")
data class GestureBindingEntity(
    @PrimaryKey @ColumnInfo(name = "direction") val direction: String,
    @ColumnInfo(name = "action") val action: String,
    @ColumnInfo(name = "package_name") val packageName: String?,
    @ColumnInfo(name = "activity_name") val activityName: String?,
    @ColumnInfo(name = "user_serial") val userSerial: Long?,
)

@Entity(
    tableName = "gesture_sample",
    primaryKeys = ["path", "package_name", "activity_name", "user_serial"],
)
data class GestureSampleEntity(
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String,
    @ColumnInfo(name = "user_serial") val userSerial: Long,
    @ColumnInfo(name = "weight") val weight: Double,
    @ColumnInfo(name = "shape") val shape: String? = null,
)
