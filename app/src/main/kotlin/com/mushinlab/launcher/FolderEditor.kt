package com.mushinlab.launcher

import com.mushinlab.launcher.core.data.repo.SpaceOutcome
import com.mushinlab.launcher.core.model.AppKey
import kotlinx.coroutines.launch

class FolderEditor(private val container: AppContainer, private val capacity: () -> Int) {
    private val repository get() = container.folders

    suspend fun create(spaceId: Long): Long? {
        val limit = capacity().takeIf { it > 0 } ?: Int.MAX_VALUE
        return (repository.create(spaceId, limit) as? SpaceOutcome.Ok)?.value
    }

    fun rename(id: Long, name: String) = inBackground { repository.rename(id, name) }

    fun addApp(id: Long, key: AppKey) = inBackground { repository.addApp(id, key) }

    fun removeApp(id: Long, key: AppKey) = inBackground { repository.removeApp(id, key) }

    fun delete(id: Long) = inBackground { repository.delete(id) }

    fun dropIfEmpty(id: Long) = inBackground { repository.deleteIfEmpty(id) }

    private fun inBackground(block: suspend () -> Unit) {
        container.scope.launch { block() }
    }
}
