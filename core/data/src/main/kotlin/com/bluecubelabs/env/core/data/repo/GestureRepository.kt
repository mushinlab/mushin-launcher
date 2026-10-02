package com.bluecubelabs.env.core.data.repo

import com.bluecubelabs.env.core.data.db.EnvDatabase
import com.bluecubelabs.env.core.data.db.GestureCodec
import com.bluecubelabs.env.core.model.DefaultGestures
import com.bluecubelabs.env.core.model.GestureAction
import com.bluecubelabs.env.core.model.GestureBinding
import com.bluecubelabs.env.core.model.SwipeDirection
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class GestureRepository(db: EnvDatabase, scope: CoroutineScope) {
    private val dao = db.gestureBindingDao()

    val bindings: StateFlow<ImmutableMap<SwipeDirection, GestureAction>> = dao.observeAll()
        .map { list -> withDefaults(list.mapNotNull(GestureCodec::toModel)) }
        .stateIn(scope, SharingStarted.Eagerly, withDefaults(emptyList()))

    suspend fun set(direction: SwipeDirection, action: GestureAction) {
        dao.upsert(GestureCodec.toEntity(GestureBinding(direction, action)))
    }

    private fun withDefaults(stored: List<GestureBinding>): ImmutableMap<SwipeDirection, GestureAction> {
        val map = DefaultGestures.bindings.associate { it.direction to it.action }.toMutableMap()
        stored.forEach { map[it.direction] = it.action }
        return map.toImmutableMap()
    }
}
