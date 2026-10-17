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

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
