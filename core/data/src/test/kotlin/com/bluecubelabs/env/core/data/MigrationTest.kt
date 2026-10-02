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
            EnvDatabase.SeedCallback.seed(db, createdAt = 0L)
        }
        helper.runMigrationsAndValidate(DB_NAME, 1, true).use { db ->
            db.query("SELECT COUNT(*) FROM environment").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(2)
            }
        }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
