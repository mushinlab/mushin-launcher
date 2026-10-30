package com.mushinlab.launcher.core.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.db.MushinDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MushinDatabase::class.java)

    @Test
    fun schemaVersion1IsExportedAndValid() {
        helper.createDatabase(DB_NAME, 1).use { db ->
            MushinDatabase.SeedCallback(MushinDatabase.SeedNames("Обычный", "Работа")).seed(db, createdAt = 0L)
        }
        helper.runMigrationsAndValidate(DB_NAME, 1, true).use { db ->
            db.query("SELECT COUNT(*) FROM environment").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(2)
            }
        }
    }

    @Test
    fun migrationTo2KeepsDataAndAddsGestureSamples() {
        helper.createDatabase(DB_NAME, 1).use { db ->
            MushinDatabase.SeedCallback(MushinDatabase.SeedNames("Обычный", "Работа")).seed(db, createdAt = 0L)
        }
        helper.runMigrationsAndValidate(DB_NAME, 2, true).use { db ->
            db.query("SELECT COUNT(*) FROM environment").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(2)
            }
            db.query("SELECT COUNT(*) FROM gesture_sample").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }
        }
    }

    @Test
    fun migrationTo3KeepsSpacesWithMonochromeIcons() {
        helper.createDatabase(DB_NAME, 1).use { db ->
            MushinDatabase.SeedCallback(MushinDatabase.SeedNames("Обычный", "Работа")).seed(db, createdAt = 0L)
        }
        helper.runMigrationsAndValidate(DB_NAME, 3, true).use { db ->
            db.query("SELECT COUNT(*), SUM(colored_icons) FROM environment").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(2)
                assertThat(cursor.getInt(1)).isEqualTo(0)
            }
        }
    }

    @Test
    fun migrationTo4KeepsSpacesAndAddsEmptySpaceUsage() {
        helper.createDatabase(DB_NAME, 1).use { db ->
            MushinDatabase.SeedCallback(MushinDatabase.SeedNames("Обычный", "Работа")).seed(db, createdAt = 0L)
        }
        helper.runMigrationsAndValidate(DB_NAME, 4, true).use { db ->
            db.query("SELECT COUNT(*) FROM environment").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(2)
            }
            db.query("SELECT COUNT(*) FROM space_usage").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }
        }
    }

    @Test
    fun migrationTo5KeepsGestureSamplesWithoutShapeAndOverridesWithoutShortcut() {
        helper.createDatabase(DB_NAME, 4).use { db ->
            db.execSQL(
                "INSERT INTO gesture_sample (path, package_name, activity_name, user_serial, weight) " +
                    "VALUES ('sdfg', 'p', 'a', 0, 2.0)",
            )
            db.execSQL(
                "INSERT INTO app_override (package_name, activity_name, user_serial, custom_label, hidden, " +
                    "orphaned_at, updated_at) VALUES ('p', 'a', 0, 'Name', 0, NULL, 1)",
            )
        }
        helper.runMigrationsAndValidate(DB_NAME, 5, true).use { db ->
            db.query("SELECT weight, shape FROM gesture_sample").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getDouble(0)).isEqualTo(2.0)
                assertThat(cursor.isNull(1)).isTrue()
            }
            db.query("SELECT custom_label, system_shortcut FROM app_override").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getString(0)).isEqualTo("Name")
                assertThat(cursor.getInt(1)).isEqualTo(0)
            }
        }
    }

    @Test
    fun migrationTo6KeepsSpacesWithoutTheirOwnListPosition() {
        helper.createDatabase(DB_NAME, 1).use { db ->
            MushinDatabase.SeedCallback(MushinDatabase.SeedNames("Обычный", "Работа")).seed(db, createdAt = 0L)
        }
        helper.runMigrationsAndValidate(DB_NAME, 6, true).use { db ->
            db.query("SELECT COUNT(*), COUNT(list_offset) FROM environment").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(2)
                assertThat(cursor.getInt(1)).isEqualTo(0)
            }
        }
    }

    @Test
    fun migrationTo7DropsTheSystemShortcutAndKeepsOverrides() {
        helper.createDatabase(DB_NAME, 6).use { db ->
            db.execSQL(
                "INSERT INTO app_override (package_name, activity_name, user_serial, custom_label, hidden, " +
                    "orphaned_at, updated_at, system_shortcut) VALUES ('p', 'a', 0, 'Name', 1, 5, 7, 1)",
            )
            db.execSQL(
                "INSERT INTO app_override (package_name, activity_name, user_serial, custom_label, hidden, " +
                    "orphaned_at, updated_at, system_shortcut) VALUES ('q', 'b', 10, NULL, 0, NULL, 9, 1)",
            )
        }
        helper.runMigrationsAndValidate(DB_NAME, 7, true).use { db ->
            db.query(
                "SELECT package_name, activity_name, user_serial, custom_label, hidden, orphaned_at, updated_at " +
                    "FROM app_override ORDER BY package_name",
            ).use { cursor ->
                assertThat(cursor.count).isEqualTo(2)
                cursor.moveToFirst()
                assertThat(cursor.getString(0)).isEqualTo("p")
                assertThat(cursor.getString(1)).isEqualTo("a")
                assertThat(cursor.getLong(2)).isEqualTo(0L)
                assertThat(cursor.getString(3)).isEqualTo("Name")
                assertThat(cursor.getInt(4)).isEqualTo(1)
                assertThat(cursor.getLong(5)).isEqualTo(5L)
                assertThat(cursor.getLong(6)).isEqualTo(7L)
                cursor.moveToNext()
                assertThat(cursor.getString(0)).isEqualTo("q")
                assertThat(cursor.getLong(2)).isEqualTo(10L)
                assertThat(cursor.isNull(3)).isTrue()
                assertThat(cursor.isNull(5)).isTrue()
                assertThat(cursor.getLong(6)).isEqualTo(9L)
            }
            db.query("PRAGMA table_info(app_override)").use { cursor ->
                val columns = buildList { while (cursor.moveToNext()) add(cursor.getString(1)) }
                assertThat(columns).doesNotContain("system_shortcut")
            }
        }
    }

    @Test
    fun migrationTo8KeepsSpacesWithANeutralBackgroundLook() {
        helper.createDatabase(DB_NAME, 7).use { db ->
            MushinDatabase.SeedCallback(MushinDatabase.SeedNames("Обычный", "Работа")).seed(db, createdAt = 0L)
            db.execSQL("UPDATE environment SET list_offset = 40, colored_icons = 1 WHERE type = 'WORK'")
        }
        helper.runMigrationsAndValidate(DB_NAME, 8, true).use { db ->
            db.query(
                "SELECT name, list_offset, colored_icons, bg_brightness, bg_blur FROM environment ORDER BY sort_order",
            ).use { cursor ->
                assertThat(cursor.count).isEqualTo(2)
                cursor.moveToFirst()
                assertThat(cursor.getString(0)).isEqualTo("Обычный")
                assertThat(cursor.getFloat(3)).isEqualTo(0f)
                assertThat(cursor.getFloat(4)).isEqualTo(0f)
                cursor.moveToNext()
                assertThat(cursor.getString(0)).isEqualTo("Работа")
                assertThat(cursor.getInt(1)).isEqualTo(40)
                assertThat(cursor.getInt(2)).isEqualTo(1)
                assertThat(cursor.getFloat(3)).isEqualTo(0f)
                assertThat(cursor.getFloat(4)).isEqualTo(0f)
            }
        }
    }

    @Test
    fun migrationTo9KeepsSpacesWithoutAHintAndAddsEmptyLaunchDays() {
        helper.createDatabase(DB_NAME, 8).use { db ->
            MushinDatabase.SeedCallback(MushinDatabase.SeedNames("Обычный", "Работа")).seed(db, createdAt = 42L)
            db.execSQL("UPDATE environment SET bg_brightness = 0.5 WHERE type = 'WORK'")
            db.execSQL(
                "INSERT INTO space_usage (env_id, package_name, activity_name, user_serial, launch_count, " +
                    "last_launched_at, decayed_score, dismissed_at) " +
                    "SELECT id, 'p', 'a', 0, 3, 7, 2.5, NULL FROM environment WHERE type = 'NORMAL'",
            )
        }
        helper.runMigrationsAndValidate(DB_NAME, 9, true).use { db ->
            db.query("SELECT name, created_at, bg_brightness, hint_at FROM environment ORDER BY sort_order").use {
                assertThat(it.count).isEqualTo(2)
                it.moveToFirst()
                assertThat(it.getString(0)).isEqualTo("Обычный")
                assertThat(it.getLong(1)).isEqualTo(42L)
                assertThat(it.isNull(3)).isTrue()
                it.moveToNext()
                assertThat(it.getString(0)).isEqualTo("Работа")
                assertThat(it.getFloat(2)).isEqualTo(0.5f)
                assertThat(it.isNull(3)).isTrue()
            }
            db.query("SELECT launch_count, decayed_score FROM space_usage").use {
                assertThat(it.count).isEqualTo(1)
                it.moveToFirst()
                assertThat(it.getInt(0)).isEqualTo(3)
                assertThat(it.getDouble(1)).isEqualTo(2.5)
            }
            db.query("SELECT COUNT(*) FROM space_launch_day").use {
                it.moveToFirst()
                assertThat(it.getInt(0)).isEqualTo(0)
            }
            db.execSQL("INSERT INTO space_launch_day SELECT id, 'p', 'a', 0, 20000 FROM environment")
            db.execSQL("PRAGMA foreign_keys = ON")
            db.execSQL("DELETE FROM environment WHERE type = 'WORK'")
            db.query("SELECT COUNT(*) FROM space_launch_day").use {
                it.moveToFirst()
                assertThat(it.getInt(0)).isEqualTo(1)
            }
        }
    }

    @Test
    fun migrationTo10KeepsTheBackgroundLookAndGivesRowsTheDefaultOpacity() {
        helper.createDatabase(DB_NAME, 9).use { db ->
            MushinDatabase.SeedCallback(MushinDatabase.SeedNames("Обычный", "Работа")).seed(db, createdAt = 42L)
            db.execSQL("UPDATE environment SET bg_brightness = -0.5, bg_blur = 0.25, hint_at = 7 WHERE type = 'WORK'")
        }
        helper.runMigrationsAndValidate(DB_NAME, 10, true).use { db ->
            db.query(
                "SELECT name, bg_brightness, bg_blur, hint_at, bg_row_alpha FROM environment ORDER BY sort_order",
            ).use {
                assertThat(it.count).isEqualTo(2)
                it.moveToFirst()
                assertThat(it.getString(0)).isEqualTo("Обычный")
                assertThat(it.getFloat(1)).isEqualTo(0f)
                assertThat(it.getFloat(4)).isEqualTo(0.9f)
                it.moveToNext()
                assertThat(it.getString(0)).isEqualTo("Работа")
                assertThat(it.getFloat(1)).isEqualTo(-0.5f)
                assertThat(it.getFloat(2)).isEqualTo(0.25f)
                assertThat(it.getLong(3)).isEqualTo(7L)
                assertThat(it.getFloat(4)).isEqualTo(0.9f)
            }
        }
    }

    @Test
    fun migrationTo11KeepsHomeListsAndAddsEmptyFolders() {
        helper.createDatabase(DB_NAME, 10).use { db ->
            MushinDatabase.SeedCallback(MushinDatabase.SeedNames("Обычный", "Работа")).seed(db, createdAt = 42L)
            db.execSQL(
                "INSERT INTO environment_item (env_id, package_name, activity_name, user_serial, position) " +
                    "VALUES (1, 'com.slack', 'Main', 0, 0)",
            )
        }
        helper.runMigrationsAndValidate(DB_NAME, 11, true).use { db ->
            db.query("SELECT package_name FROM environment_item").use {
                assertThat(it.count).isEqualTo(1)
                it.moveToFirst()
                assertThat(it.getString(0)).isEqualTo("com.slack")
            }
            db.query("SELECT COUNT(*) FROM folder").use {
                it.moveToFirst()
                assertThat(it.getInt(0)).isEqualTo(0)
            }
        }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
