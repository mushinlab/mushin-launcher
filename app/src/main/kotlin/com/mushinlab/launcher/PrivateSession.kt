package com.mushinlab.launcher

import com.mushinlab.launcher.core.model.SpaceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PrivateSession(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    private val data: StateFlow<LauncherData?>,
    private val navigator: LauncherNavigator,
) {
    private var lock: Job? = null

    init {
        scope.launch {
            data.map { it?.current?.type }.distinctUntilChanged().collect { type ->
                if (type == SpaceType.PRIVATE) schedule() else lock?.cancel()
            }
        }
    }

    fun touched() {
        if (data.value?.current?.type == SpaceType.PRIVATE) schedule()
    }

    suspend fun enter() {
        val privateSpace = data.value?.spaces?.firstOrNull { it.type == SpaceType.PRIVATE } ?: return
        container.settings.update { it.copy(currentSpaceId = privateSpace.id) }
        navigator.closeOverlays()
        schedule()
    }

    fun stop() {
        lock?.cancel()
    }

    private fun schedule() {
        lock?.cancel()
        lock = scope.launch {
            delay(LOCK_MS)
            leave()
        }
    }

    private suspend fun leave() {
        val normal = data.value?.spaces?.firstOrNull { it.type == SpaceType.NORMAL } ?: return
        container.settings.update { it.copy(currentSpaceId = normal.id) }
        navigator.closeOverlays()
    }

    private companion object {
        const val LOCK_MS = 120_000L
    }
}
