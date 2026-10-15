package com.mushinlab.launcher.core.data.repo

import androidx.room.withTransaction
import com.mushinlab.launcher.core.data.db.EnvDatabase
import com.mushinlab.launcher.core.data.db.toEntity
import com.mushinlab.launcher.core.data.db.toModel
import com.mushinlab.launcher.core.model.LearnedGesture
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class GestureLearningRepository(private val db: EnvDatabase, scope: CoroutineScope) {
    private val dao = db.gestureSampleDao()

    val samples: StateFlow<ImmutableList<LearnedGesture>> = dao.observeAll()
        .map { list -> list.map { it.toModel() }.toImmutableList() }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    suspend fun update(transform: (List<LearnedGesture>) -> List<LearnedGesture>) {
        db.withTransaction {
            dao.replaceAll(transform(dao.getAll().map { it.toModel() }).map { it.toEntity() })
        }
    }
}
