package com.mushinlab.launcher

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import com.mushinlab.launcher.core.data.repo.neighborSpace
import com.mushinlab.launcher.core.model.Palette
import com.mushinlab.launcher.core.model.Space
import com.mushinlab.launcher.core.model.SpaceTransition
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.feature.home.HomeUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class SpacePreview(
    val id: Long,
    val name: String,
    val palette: Palette,
    val background: Bitmap?,
    val brightness: Float,
    val rows: Int,
)

@Immutable
data class SpaceNeighbors(val previous: SpacePreview? = null, val next: SpacePreview? = null)

@Immutable
data class SpaceSwitch(val target: SpacePreview, val transition: SpaceTransition, val direction: SwipeDirection)

class SpaceBitmap(val spaceId: Long?, val bitmap: Bitmap?)

@OptIn(ExperimentalCoroutinesApi::class)
class SpacePager(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    private val data: StateFlow<LauncherData?>,
    private val home: StateFlow<HomeUiState>,
    private val background: StateFlow<SpaceBitmap>,
) {
    private val requestFlow = MutableStateFlow<SpaceSwitch?>(null)
    val request: StateFlow<SpaceSwitch?> = requestFlow.asStateFlow()
    private var pending = false

    val neighbors: StateFlow<SpaceNeighbors> = data.filterNotNull()
        .map { d ->
            val previous = neighborSpace(d.spaces, d.current?.id, -1)
            val next = neighborSpace(d.spaces, d.current?.id, 1)
            NeighborKey(previous, next, listOfNotNull(previous, next).map { d.items[it.id]?.size ?: 0 })
        }
        .combine(container.backgrounds.versions) { key, versions -> key to versions }
        .distinctUntilChanged()
        .mapLatest { (key, _) ->
            val d = data.value
            SpaceNeighbors(key.previous?.let { preview(d, it) }, key.next?.let { preview(d, it) })
        }
        .flowOn(Dispatchers.IO)
        .stateIn(scope, SharingStarted.Eagerly, SpaceNeighbors())

    fun select(id: Long) {
        val d = data.value ?: return
        val current = d.current ?: return
        val target = d.spaces.firstOrNull { it.id == id } ?: return
        if (current.id == id || pending || requestFlow.value != null) return
        val transition = d.settings.spaceTransition
        if (current.type == SpaceType.PRIVATE || transition == SpaceTransition.NONE) {
            scope.launch { container.settings.update { it.copy(currentSpaceId = id) } }
            return
        }
        val from = d.spaces.indexOfFirst { it.id == current.id }
        val to = d.spaces.indexOf(target)
        val forward = Math.floorMod(to - from, d.spaces.size)
        val backward = Math.floorMod(from - to, d.spaces.size)
        val direction = if (forward <= backward) SwipeDirection.LEFT else SwipeDirection.RIGHT
        pending = true
        scope.launch {
            try {
                val preview = withContext(Dispatchers.IO) { preview(d, target) }
                requestFlow.value = SpaceSwitch(preview, transition, direction)
            } finally {
                pending = false
            }
        }
    }

    suspend fun commit(id: Long) {
        container.settings.update { it.copy(currentSpaceId = id) }
        combine(home, background, data) { state, image, d ->
            val shown = state.spaceId == id && image.spaceId == id
            shown || d?.spaces?.none { it.id == id } == true
        }.first { it }
    }

    fun done() {
        requestFlow.value = null
    }

    private fun preview(d: LauncherData?, space: Space): SpacePreview = SpacePreview(
        id = space.id,
        name = space.name,
        palette = space.palette,
        background = if (space.id in container.backgrounds.versions.value) {
            container.backgrounds.loadThumbnail(space.id, THUMBNAIL_SIDE)
        } else {
            null
        },
        brightness = space.backgroundBrightness,
        rows = d?.items?.get(space.id)?.size?.coerceAtMost(PREVIEW_ROWS) ?: 0,
    )

    private data class NeighborKey(val previous: Space?, val next: Space?, val rows: List<Int>)

    private companion object {
        const val THUMBNAIL_SIDE = 360
        const val PREVIEW_ROWS = 8
    }
}
