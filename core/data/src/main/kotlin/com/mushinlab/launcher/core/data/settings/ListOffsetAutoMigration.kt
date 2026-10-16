package com.mushinlab.launcher.core.data.settings

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

/**
 * One-time cleanup for ADR-0018: versions up to 0.2.0 stored the global list offset as 0 on every settings save,
 * although users only move the list per space. Removing that 0 lets the automatic default position apply; any
 * other stored value is kept.
 */
internal object ListOffsetAutoMigration : DataMigration<Preferences> {
    private val listOffset = intPreferencesKey("home_list_offset")
    private val done = booleanPreferencesKey("list_offset_auto_migrated")

    override suspend fun shouldMigrate(currentData: Preferences): Boolean = currentData[done] != true

    override suspend fun migrate(currentData: Preferences): Preferences = currentData.toMutablePreferences().apply {
        if (this[listOffset] == 0) remove(listOffset)
        this[done] = true
    }.toPreferences()

    override suspend fun cleanUp() = Unit
}
