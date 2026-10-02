package com.bluecubelabs.env.core.data.repo

import androidx.room.withTransaction
import com.bluecubelabs.env.core.data.db.AppOverrideEntity
import com.bluecubelabs.env.core.data.db.EnvDatabase
import com.bluecubelabs.env.core.data.db.toModel
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.AppOverride
import com.bluecubelabs.env.core.model.EnvLimits
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class OverrideRepository(
    private val db: EnvDatabase,
    scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val dao = db.appOverrideDao()

    val overrides: StateFlow<ImmutableMap<AppKey, AppOverride>> = dao.observeAll()
        .map { list -> list.map { it.toModel() }.associateBy { it.key }.toImmutableMap() }
        .stateIn(scope, SharingStarted.Eagerly, persistentMapOf())

    suspend fun rename(key: AppKey, label: String?): Boolean {
        val trimmed = label?.trim()?.takeIf { it.isNotEmpty() }
        if (trimmed != null && trimmed.length > EnvLimits.LABEL_MAX_LENGTH) return false
        update(key) { it.copy(customLabel = trimmed) }
        return true
    }

    suspend fun setHidden(key: AppKey, hidden: Boolean) {
        update(key) { it.copy(hidden = hidden) }
    }

    private suspend fun update(key: AppKey, change: (AppOverrideEntity) -> AppOverrideEntity) {
        db.withTransaction {
            val current = dao.get(key.packageName, key.activityName, key.userSerial)
                ?: AppOverrideEntity(key.packageName, key.activityName, key.userSerial, null, false, null, clock())
            val next = change(current).copy(updatedAt = clock())
            if (next.customLabel == null && !next.hidden) {
                dao.delete(key.packageName, key.activityName, key.userSerial)
            } else {
                dao.upsert(next)
            }
        }
    }
}
