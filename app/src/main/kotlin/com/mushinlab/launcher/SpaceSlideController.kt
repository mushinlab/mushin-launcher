package com.mushinlab.launcher

import android.graphics.Bitmap
import com.mushinlab.launcher.core.apps.icons.IconKey
import com.mushinlab.launcher.core.data.repo.neighborSpace
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.model.Space
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.model.WidgetLayout
import com.mushinlab.launcher.core.model.swipesByApp
import com.mushinlab.launcher.feature.home.HomeUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SlideFacts(val roleHeld: StateFlow<Boolean>, val capacity: StateFlow<Int>)

class SpaceSlideController(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    private val data: StateFlow<LauncherData?>,
    private val home: StateFlow<HomeUiState>,
    private val background: StateFlow<Bitmap?>,
    private val facts: SlideFacts,
) {
    private val slideFlow = MutableStateFlow<SpaceSlide?>(null)
    val slide: StateFlow<SpaceSlide?> = slideFlow.asStateFlow()
    private var pending = false

    fun pageSpace(direction: SwipeDirection) {
        val d = data.value ?: return
        val current = d.current ?: return
        val step = when (direction) {
            SwipeDirection.LEFT -> 1
            SwipeDirection.RIGHT -> -1
            else -> return
        }
        val target = neighborSpace(d.spaces, current.id, step) ?: return
        start(d, target, direction)
    }

    fun select(id: Long) {
        val d = data.value ?: return
        val current = d.current ?: return
        if (current.id == id) return
        if (current.type == SpaceType.PRIVATE) {
            scope.launch { container.settings.update { it.copy(currentSpaceId = id) } }
            return
        }
        val from = d.spaces.indexOfFirst { it.id == current.id }
        val to = d.spaces.indexOfFirst { it.id == id }
        if (from < 0 || to < 0) return
        val forward = Math.floorMod(to - from, d.spaces.size)
        val backward = Math.floorMod(from - to, d.spaces.size)
        start(d, d.spaces[to], if (forward <= backward) SwipeDirection.LEFT else SwipeDirection.RIGHT)
    }

    fun finish() {
        val target = slideFlow.value?.to?.id ?: return
        scope.launch {
            data.map { it?.current?.id }.first { it == target }
            slideFlow.value = null
        }
    }

    private fun start(d: LauncherData, target: Space, direction: SwipeDirection) {
        val from = d.current ?: return
        if (pending || slideFlow.value != null) return
        pending = true
        scope.launch {
            try {
                val incoming = prepare(d, target)
                val outgoing = SpacePage(from.id, home.value, from.palette, background.value, from.coloredIcons)
                container.settings.update { it.copy(currentSpaceId = target.id) }
                slideFlow.value = SpaceSlide(outgoing, incoming, direction)
            } finally {
                pending = false
            }
        }
    }

    private suspend fun prepare(d: LauncherData, space: Space): SpacePage = withContext(Dispatchers.IO) {
        val pageData = d.copy(current = space)
        val bindings = container.gestures.bindings.value
        val swipes = bindings.swipesByApp()
        if (d.settings.iconMode != IconMode.NONE) {
            val sizePx = (HOME_ICON_DP * container.densityScale()).toInt()
            pageData.currentItems().forEach { key ->
                pageData.apps[key]?.let {
                    container.icons.load(IconKey(key, it.lastUpdateTime, sizePx, space.coloredIcons))
                }
            }
        }
        val layout = container.widgets.loaded.value?.layouts?.get(space.id) ?: WidgetLayout()
        SpacePage(
            id = space.id,
            state = homeState(pageData, facts.roleHeld.value, container.notificationCounts.value, swipes).copy(
                widgetPanels = widgetPanels(layout),
                suggestion = suggestionFor(
                    pageData,
                    SpaceStats(container.history.spaceUsage.value, container.history.launchDays.value),
                    swipes.keys,
                    facts.capacity.value,
                    System.currentTimeMillis(),
                ),
            ),
            palette = space.palette,
            background = if (space.id in container.backgrounds.versions.value) {
                container.backgrounds.load(space.id)
            } else {
                null
            },
            coloredIcons = space.coloredIcons,
        )
    }

    private companion object {
        const val HOME_ICON_DP = 28f
    }
}
