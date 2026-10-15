package com.mushinlab.launcher

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.ShapeVertex
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.search.GesturePoint
import com.mushinlab.launcher.core.search.KeyboardLayouts
import com.mushinlab.launcher.core.search.ShapeMatcher
import com.mushinlab.launcher.core.search.Shapes
import com.mushinlab.launcher.feature.search.KeyboardDimens
import com.mushinlab.launcher.feature.search.SearchMode
import com.mushinlab.launcher.feature.search.glideCenters
import com.mushinlab.launcher.feature.settings.SettingsRoute
import kotlinx.collections.immutable.persistentListOf

@Stable
class HomeInteractions(
    private val model: LauncherModel,
    private val navigator: LauncherNavigator,
    private val launches: AppLaunches,
    private val emit: (LauncherEffect) -> Unit,
) {
    var exit by mutableStateOf<SwipeDirection?>(null)
        private set
    var learnHint by mutableStateOf(false)
    private var pendingSwipeLaunch: AppKey? = null
    private var shadeWarned = false
    private var missedDrawings = emptyList<List<GesturePoint>>()
    private var learnDraft: List<GesturePoint>? = null

    val gesturesEnabled: Boolean
        get() = navigator.overlay == Overlay.None && !navigator.settings.isOpen && exit == null

    fun onSwipe(direction: SwipeDirection, fromShadeZone: Boolean) {
        val action = if (fromShadeZone) GestureAction.Notifications else model.gestureBindings[direction]
        when (action) {
            null, GestureAction.None -> Unit

            GestureAction.Search -> navigator.openSearch(SearchMode.TYPING)

            GestureAction.AllApps -> navigator.openSearch(SearchMode.BROWSING)

            GestureAction.Notifications -> expandShade()

            is GestureAction.LaunchApp -> if (model.entry(action.key) != null) {
                pendingSwipeLaunch = action.key
                exit = direction
            }
        }
    }

    fun completeSwipeLaunch() {
        val key = pendingSwipeLaunch ?: return
        pendingSwipeLaunch = null
        if (!launches.launch(key, fromSearch = false, animate = false)) exit = null
    }

    fun onDrawing(points: List<GesturePoint>, width: Float, density: Float) {
        if (navigator.overlay != Overlay.None || points.size < 2 || width <= 0f) return
        val keyboardWidth = width * model.settings.keyboardWidth
        val rowHeight = KeyboardDimens.rowHeight.value * density
        val centers = glideCenters(
            keyboardWidth,
            rowHeight,
            KeyboardDimens.verticalPadding.value * density,
            KeyboardDimens.gap.value * density,
        )
        val matches = ShapeMatcher(centers, keyboardWidth, rowHeight * KeyboardLayouts.LETTER_ROWS)
            .match(points, model.learnedGestures)
            .filter { model.entry(it) != null }
        if (matches.isEmpty()) {
            onMissedDrawing(points)
        } else {
            missedDrawings = emptyList()
            navigator.openSearch(SearchMode.TYPING)
            navigator.search.suggest(matches)
        }
    }

    fun openLearning() {
        learnHint = false
        navigator.closeOverlays()
        navigator.settings.open(SettingsRoute.Memory)
        navigator.settings.draft = learnDraft?.map { ShapeVertex(it.x, it.y) }
        learnDraft = null
        navigator.settings.push(SettingsRoute.MemoryPickApp)
    }

    fun editCurrentSpace() {
        val space = model.currentSpace ?: return
        navigator.openSettings(SettingsRoute.SpaceEditor(space.id))
    }

    fun onStop() {
        exit = null
        pendingSwipeLaunch = null
        if (navigator.overlay != Overlay.None) navigator.closeOverlays()
    }

    private fun expandShade() {
        if (!model.expandShade() && !shadeWarned) {
            shadeWarned = true
            navigator.notice = Notice(R.string.notice_shade_unavailable, persistentListOf())
        }
    }

    private fun onMissedDrawing(points: List<GesturePoint>) {
        emit(LauncherEffect.Reject)
        missedDrawings += listOf(points)
        if (missedDrawings.size >= LEARN_HINT_MISSES) {
            learnDraft = Shapes.average(missedDrawings)
            missedDrawings = emptyList()
            learnHint = true
        }
    }

    companion object {
        const val LEARN_HINT_MISSES = 5
    }
}
