package com.bluecubelabs.env.core.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.bluecubelabs.env.core.data.db.EnvDatabase
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), EnvDatabase::class.java)

    @Test
    fun schemaVersion1IsExportedAndValid() {
        helper.createDatabase(DB_NAME, 1).use { db ->
            EnvDatabase.SeedCallback(EnvDatabase.SeedNames("Обычный", "Работа")).seed(db, createdAt = 0L)
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
            EnvDatabase.SeedCallback(EnvDatabase.SeedNames("Обычный", "Работа")).seed(db, createdAt = 0L)
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
            EnvDatabase.SeedCallback(EnvDatabase.SeedNames("Обычный", "Работа")).seed(db, createdAt = 0L)
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
            EnvDatabase.SeedCallback(EnvDatabase.SeedNames("Обычный", "Работа")).seed(db, createdAt = 0L)
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
            EnvDatabase.SeedCallback(EnvDatabase.SeedNames("Обычный", "Работа")).seed(db, createdAt = 0L)
        }
        helper.runMigrationsAndValidate(DB_NAME, 6, true).use { db ->
            db.query("SELECT COUNT(*), COUNT(list_offset) FROM environment").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(2)
                assertThat(cursor.getInt(1)).isEqualTo(0)
            }
        }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
