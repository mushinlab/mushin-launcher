package com.mushinlab.launcher.core.data.widgets

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.mushinlab.launcher.core.model.WidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Per-space widget layouts and pending system requests in a separate local DataStore. */
class WidgetRepository(private val store: DataStore<Preferences>, scope: CoroutineScope) {
    val loaded: StateFlow<WidgetState?> = store.data.map { WidgetStateCodec.decode(it[STATE]) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** Atomically stores [transform], preserving layouts of other spaces and any pending request. */
    suspend fun update(transform: (WidgetState) -> WidgetState) {
        store.edit { it[STATE] = WidgetStateCodec.encode(transform(WidgetStateCodec.decode(it[STATE]))) }
    }

    companion object {
        private val STATE = stringPreferencesKey("widget_state")

        /** Creates the local widget store without changing Room or existing preference keys. */
        fun create(context: Context, scope: CoroutineScope): WidgetRepository = WidgetRepository(
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                scope = scope,
                produceFile = { context.preferencesDataStoreFile("widgets") },
            ),
            scope,
        )
    }
}
