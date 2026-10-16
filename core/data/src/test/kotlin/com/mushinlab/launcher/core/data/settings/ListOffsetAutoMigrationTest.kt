package com.mushinlab.launcher.core.data.settings

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ListOffsetAutoMigrationTest {
    private val offset = intPreferencesKey("home_list_offset")

    @Test
    fun storedZeroBecomesAutomaticOnce() = runTest {
        val migrated = ListOffsetAutoMigration.migrate(preferencesOf(offset to 0))

        assertThat(migrated[offset]).isNull()
        assertThat(ListOffsetAutoMigration.shouldMigrate(migrated)).isFalse()
    }

    @Test
    fun aMovedListKeepsItsOffset() = runTest {
        val migrated = ListOffsetAutoMigration.migrate(preferencesOf(offset to 120))

        assertThat(migrated[offset]).isEqualTo(120)
    }

    @Test
    fun freshSettingsAreOnlyMarked() = runTest {
        assertThat(ListOffsetAutoMigration.shouldMigrate(preferencesOf())).isTrue()

        val migrated = ListOffsetAutoMigration.migrate(preferencesOf())

        assertThat(migrated[offset]).isNull()
        assertThat(ListOffsetAutoMigration.shouldMigrate(migrated)).isFalse()
    }
}
