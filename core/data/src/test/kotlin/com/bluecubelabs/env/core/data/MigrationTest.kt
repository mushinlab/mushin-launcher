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

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
