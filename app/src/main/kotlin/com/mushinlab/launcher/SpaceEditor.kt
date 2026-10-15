package com.mushinlab.launcher

import com.mushinlab.launcher.core.data.repo.SpaceOutcome
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.SpaceType
import kotlinx.coroutines.launch

class SpaceEditor(
    private val container: AppContainer,
    private val items: (Long) -> List<AppKey>,
    private val capacity: () -> Int,
) {
    private val repository get() = container.spaces

    suspend fun create(name: String, type: SpaceType): String? {
        val preset = if (type == SpaceType.WORK) PalettePreset.NAVY else PalettePreset.MOSS
        return repository.create(name, type, preset).errorCode()
    }

    suspend fun rename(id: Long, name: String): String? = repository.rename(id, name).errorCode()

    fun delete(id: Long) = container.inBackground { repository.delete(id) }

    fun switchTo(id: Long) {
        container.scope.launch { container.settings.update { it.copy(currentSpaceId = id) } }
    }

    fun setItems(id: Long, keys: List<AppKey>) = container.inBackground { repository.setItems(id, keys) }

    fun addItem(id: Long, key: AppKey): Boolean {
        val limit = capacity().takeIf { it > 0 } ?: Int.MAX_VALUE
        if (items(id).size >= limit) return false
        container.inBackground { repository.addItems(id, listOf(key), limit) }
        return true
    }

    fun removeItem(id: Long, key: AppKey) = container.inBackground { repository.removeItem(id, key) }

    fun setPalette(id: Long, preset: PalettePreset) = container.inBackground { repository.setPalette(id, preset) }

    fun setColoredIcons(id: Long, enabled: Boolean) = container.inBackground { repository.setColoredIcons(id, enabled) }

    fun setListOffset(id: Long, dp: Int) = container.inBackground { repository.setListOffset(id, dp) }
}

private fun <T> SpaceOutcome<T>.errorCode(): String? = when (this) {
    is SpaceOutcome.Ok -> null
    is SpaceOutcome.Failed -> error.code()
}

private fun AppContainer.inBackground(block: suspend () -> Unit) {
    scope.launch { block() }
}
