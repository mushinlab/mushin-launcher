package com.bluecubelabs.env.core.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bluecubelabs.env.core.data.R
import com.bluecubelabs.env.core.model.DefaultGestures
import com.bluecubelabs.env.core.model.EnvType
import com.bluecubelabs.env.core.model.PalettePreset
import com.bluecubelabs.env.core.model.Palettes

@Database(
    entities = [
        EnvironmentEntity::class,
        EnvironmentItemEntity::class,
        AppOverrideEntity::class,
        SearchRecentEntity::class,
        AppUsageEntity::class,
        GestureBindingEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class EnvDatabase : RoomDatabase() {
    abstract fun environmentDao(): EnvironmentDao

    abstract fun environmentItemDao(): EnvironmentItemDao

    abstract fun appOverrideDao(): AppOverrideDao

    abstract fun searchRecentDao(): SearchRecentDao

    abstract fun appUsageDao(): AppUsageDao

    abstract fun gestureBindingDao(): GestureBindingDao

    companion object {
        const val NAME = "env.db"

        fun build(context: Context, inMemory: Boolean = false): EnvDatabase {
            val builder = if (inMemory) {
                Room.inMemoryDatabaseBuilder(context, EnvDatabase::class.java)
            } else {
                Room.databaseBuilder(context, EnvDatabase::class.java, NAME)
            }
            val names = SeedNames(
                normal = context.getString(R.string.env_default_normal),
                work = context.getString(R.string.env_default_work),
            )
            return builder.addCallback(SeedCallback(names)).build()
        }
    }

    data class SeedNames(val normal: String, val work: String)

    class SeedCallback(private val names: SeedNames) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            seed(db, createdAt = System.currentTimeMillis())
        }

        fun seed(db: SupportSQLiteDatabase, createdAt: Long) {
            insertEnvironment(db, names.normal, EnvType.NORMAL, 0, PalettePreset.GRAPHITE, createdAt)
            insertEnvironment(db, names.work, EnvType.WORK, 1, PalettePreset.NAVY, createdAt)
            DefaultGestures.bindings.forEach { binding ->
                val values = ContentValues().apply {
                    put("direction", binding.direction.name)
                    put("action", GestureCodec.actionName(binding.action))
                }
                db.insert("gesture_binding", SQLiteDatabase.CONFLICT_REPLACE, values)
            }
        }

        private fun insertEnvironment(
            db: SupportSQLiteDatabase,
            name: String,
            type: EnvType,
            sortOrder: Int,
            preset: PalettePreset,
            createdAt: Long,
        ) {
            val p = Palettes.of(preset)
            val values = ContentValues().apply {
                put("name", name)
                put("type", type.name)
                put("sort_order", sortOrder)
                put("palette_preset", preset.name)
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
    }
}
