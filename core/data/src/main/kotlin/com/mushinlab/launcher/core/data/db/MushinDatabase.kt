package com.mushinlab.launcher.core.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mushinlab.launcher.core.data.R
import com.mushinlab.launcher.core.model.DefaultGestures
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.model.SpaceType

/**
 * Room database of spaces, home lists, app overrides, launch history, gesture bindings and learned drawings.
 *
 * The file and several tables keep the working name "env" (ADR-0015). Schemas are exported and every version
 * step is an auto-migration: v2 gesture_sample, v3 environment.colored_icons, v4 space_usage,
 * v5 app_override.system_shortcut and gesture_sample.shape, v6 environment.list_offset,
 * v7 drops app_override.system_shortcut ([DropSystemShortcut]), v8 environment.bg_brightness and bg_blur.
 * A new database is seeded with the normal and work spaces and the default gestures.
 */
@Database(
    entities = [
        SpaceEntity::class,
        SpaceItemEntity::class,
        AppOverrideEntity::class,
        SearchRecentEntity::class,
        AppUsageEntity::class,
        GestureBindingEntity::class,
        GestureSampleEntity::class,
        SpaceUsageEntity::class,
    ],
    version = 8,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7, spec = MushinDatabase.DropSystemShortcut::class),
        AutoMigration(from = 7, to = 8),
    ],
)
abstract class MushinDatabase : RoomDatabase() {
    abstract fun spaceDao(): SpaceDao

    abstract fun spaceItemDao(): SpaceItemDao

    abstract fun appOverrideDao(): AppOverrideDao

    abstract fun searchRecentDao(): SearchRecentDao

    abstract fun appUsageDao(): AppUsageDao

    abstract fun gestureBindingDao(): GestureBindingDao

    abstract fun gestureSampleDao(): GestureSampleDao

    abstract fun spaceUsageDao(): SpaceUsageDao

    abstract fun appKeyDao(): AppKeyDao

    companion object {
        const val NAME = "env.db"

        /** Opens [NAME] or, with [inMemory], a throwaway database for tests; seed names come from resources. */
        fun build(context: Context, inMemory: Boolean = false): MushinDatabase {
            val builder = if (inMemory) {
                Room.inMemoryDatabaseBuilder(context, MushinDatabase::class.java)
            } else {
                Room.databaseBuilder(context, MushinDatabase::class.java, NAME)
            }
            val names = SeedNames(
                normal = context.getString(R.string.space_default_normal),
                work = context.getString(R.string.space_default_work),
            )
            return builder.addCallback(SeedCallback(names)).build()
        }
    }

    /** Removes the "bound to a system gesture" mark (ADR-0017); other override columns are kept. */
    @DeleteColumn(tableName = "app_override", columnName = "system_shortcut")
    class DropSystemShortcut : AutoMigrationSpec

    /** Localized names of the spaces created with a new database. */
    data class SeedNames(val normal: String, val work: String)

    /** Fills a newly created database with the normal and work spaces and the default gesture bindings. */
    class SeedCallback(private val names: SeedNames) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            seed(db, createdAt = System.currentTimeMillis())
        }

        fun seed(db: SupportSQLiteDatabase, createdAt: Long) {
            insertSpace(db, SeedSpace(names.normal, SpaceType.NORMAL, 0, PalettePreset.GRAPHITE), createdAt)
            insertSpace(db, SeedSpace(names.work, SpaceType.WORK, 1, PalettePreset.NAVY), createdAt)
            DefaultGestures.bindings.forEach { binding ->
                val values = ContentValues().apply {
                    put("direction", binding.direction.name)
                    put("action", GestureCodec.actionName(binding.action))
                }
                db.insert("gesture_binding", SQLiteDatabase.CONFLICT_REPLACE, values)
            }
        }

        private fun insertSpace(db: SupportSQLiteDatabase, space: SeedSpace, createdAt: Long) {
            val p = Palettes.of(space.preset)
            val values = ContentValues().apply {
                put("name", space.name)
                put("type", space.type.name)
                put("sort_order", space.sortOrder)
                put("palette_preset", space.preset.name)
                put("c_background", p.background)
                put("c_zebra_a", p.zebraA)
                put("c_zebra_b", p.zebraB)
                put("c_text_primary", p.textPrimary)
                put("c_text_secondary", p.textSecondary)
                put("c_accent", p.accent)
                put("c_icon_tint", p.iconTint)
                put("c_divider", p.divider)
                put("c_surface", p.surface)
                put("c_key_background", p.keyBackground)
                put("c_key_pressed", p.keyPressed)
                put("is_dark", if (p.isDark) 1 else 0)
                put("created_at", createdAt)
            }
            db.insert("environment", SQLiteDatabase.CONFLICT_ABORT, values)
        }

        private class SeedSpace(val name: String, val type: SpaceType, val sortOrder: Int, val preset: PalettePreset)
    }
}
