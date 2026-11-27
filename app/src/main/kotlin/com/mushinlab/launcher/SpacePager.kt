package com.mushinlab.launcher

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import com.mushinlab.launcher.core.apps.icons.IconKey
import com.mushinlab.launcher.core.data.repo.neighborSpace
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.model.Palette
import com.mushinlab.launcher.core.model.Space
import com.mushinlab.launcher.core.model.SpaceTransition
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.model.WidgetLayout
import com.mushinlab.launcher.core.model.WidgetState
import com.mushinlab.launcher.core.model.swipesByApp
import com.mushinlab.launcher.feature.home.HomeUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class SpacePage(
    val id: Long,
    val state: HomeUiState,
    val palette: Palette,
    val background: Bitmap?,
    val coloredIcons: Boolean,
)

@Immutable
data class SpaceNeighbors(val previous: SpacePage? = null, val next: SpacePage? = null)

@Immutable
data class SpaceSwitch(val target: SpacePage, val transition: SpaceTransition, val direction: SwipeDirection)

class SpaceBitmap(val spaceId: Long?, val bitmap: Bitmap?)

class SlideFacts(val roleHeld: StateFlow<Boolean>, val capacity: StateFlow<Int>)

@OptIn(ExperimentalCoroutinesApi::class)
class SpacePager(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    private val data: StateFlow<LauncherData?>,
    private val home: StateFlow<HomeUiState>,
    private val background: StateFlow<SpaceBitmap>,
    private val facts: SlideFacts,
) {
    private val requestFlow = MutableStateFlow<SpaceSwitch?>(null)
    val request: StateFlow<SpaceSwitch?> = requestFlow.asStateFlow()
    private var pending = false
    private val wallpapers = MutableStateFlow<Map<Long, Wallpaper>>(emptyMap())

    private val pages = combine(
        data.filterNotNull(),
        container.widgets.loaded,
        container.gestures.bindings,
    ) { d, widgets, bindings -> PageInputs(d, widgets, bindings) }
        .mapLatest { inputs ->
            val d = inputs.data
            listOf(-1, 1).map { step -> neighborSpace(d.spaces, d.current?.id, step)?.let { page(inputs, it) } }
        }
        .flowOn(Dispatchers.IO)

    val neighbors: StateFlow<SpaceNeighbors> = combine(pages, wallpapers) { (previous, next), images ->
        SpaceNeighbors(previous?.dressed(images), next?.dressed(images))
    }.stateIn(scope, SharingStarted.Eagerly, SpaceNeighbors())

    fun warm() {
        val versions = container.backgrounds.versions.value
        val ids = listOfNotNull(neighbors.value.previous?.id, neighbors.value.next?.id)
            .filter { id -> versions[id] != null && wallpapers.value[id]?.version != versions[id] }
        if (ids.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            ids.forEach { id ->
                val version = versions[id] ?: return@forEach
                val bitmap = container.backgrounds.load(id) ?: return@forEach
                wallpapers.update { it + (id to Wallpaper(version, bitmap)) }
            }
        }
    }

    fun cool() {
        wallpapers.value = emptyMap()
    }

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
                val page = withContext(Dispatchers.IO) {
                    val inputs = PageInputs(d, container.widgets.loaded.value, container.gestures.bindings.value)
                    page(inputs, target).copy(background = loadBackground(target.id))
                }
                requestFlow.value = SpaceSwitch(page, transition, direction)
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

    private fun loadBackground(id: Long): Bitmap? =
        if (id in container.backgrounds.versions.value) container.backgrounds.load(id) else null

    private fun SpacePage.dressed(images: Map<Long, Wallpaper>): SpacePage =
        images[id]?.let { copy(background = it.bitmap) } ?: this

    private suspend fun page(inputs: PageInputs, space: Space): SpacePage {
        val pageData = inputs.data.copy(current = space)
        val swipes = inputs.bindings.swipesByApp()
        if (pageData.settings.iconMode != IconMode.NONE) {
            val sizePx = (HOME_ICON_DP * container.densityScale()).toInt()
            pageData.currentItems().forEach { key ->
                pageData.apps[key]?.let {
                    container.icons.load(IconKey(key, it.lastUpdateTime, sizePx, space.coloredIcons))
                }
            }
        }
        val layout = inputs.widgets?.layouts?.get(space.id) ?: WidgetLayout()
        val stats = SpaceStats(container.history.spaceUsage.value, container.history.launchDays.value)
        val state = homeState(pageData, facts.roleHeld.value, container.notificationCounts.value, swipes).copy(
            widgetPanels = widgetPanels(layout),
            suggestion = suggestionFor(pageData, stats, swipes.keys, facts.capacity.value, System.currentTimeMillis()),
        )
        return SpacePage(space.id, state, space.palette, null, space.coloredIcons)
    }

    private class PageInputs(
        val data: LauncherData,
        val widgets: WidgetState?,
        val bindings: Map<SwipeDirection, GestureAction>,
    )

    private class Wallpaper(val version: Int, val bitmap: Bitmap)

    private companion object {
        const val HOME_ICON_DP = 28f
    }
}
