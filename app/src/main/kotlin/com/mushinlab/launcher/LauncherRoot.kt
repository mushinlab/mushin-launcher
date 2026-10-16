package com.mushinlab.launcher

import androidx.activity.compose.BackHandler
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.search.GesturePoint
import com.mushinlab.launcher.core.ui.LocalIconProvider
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.LocalPressFeedback
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.core.ui.MushinFont
import com.mushinlab.launcher.core.ui.MushinStroke
import com.mushinlab.launcher.core.ui.MushinTheme
import com.mushinlab.launcher.feature.home.HomeActions
import com.mushinlab.launcher.feature.home.HomeRow
import com.mushinlab.launcher.feature.home.HomeScreen
import com.mushinlab.launcher.feature.search.SearchMode

@Composable
fun LauncherRoot(vm: LauncherViewModel, iconProvider: LauncherIconProvider) {
    val palette by vm.palette.collectAsStateWithLifecycle()
    val ready by vm.ready.collectAsStateWithLifecycle()
    val iconsReady by vm.iconsReady.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val colored by vm.coloredIcons.collectAsStateWithLifecycle()
    ReportDrawnWhen { ready && iconsReady }
    BackHandler(enabled = true) { vm.navigator.handleBack() }
    val icons = remember(iconProvider, colored) { iconProvider.withColors(colored) }
    val font = remember(settings.font, settings.fontWeight) { MushinFont.of(settings.font, settings.fontWeight) }
    val stroke = remember(settings.strokeWidth, settings.strokeColor, settings.strokeDots) {
        MushinStroke(settings.strokeWidth.dp, settings.strokeColor, settings.strokeDots)
    }
    MushinTheme(palette, font, stroke) {
        CompositionLocalProvider(
            LocalIconProvider provides icons,
            LocalPressFeedback provides settings.pressFeedback,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(LocalMushinColors.current.background)
                    .semantics { testTagsAsResourceId = true },
            ) {
                HomeLayer(vm)
                SearchLayer(vm, settings, iconProvider)
                PopupLayer(vm)
                SuggestionLayer(vm)
                Box(Modifier.zIndex(3f)) { SettingsLayer(vm) }
                MessageLayer(vm)
            }
        }
    }
}

@Composable
private fun HomeLayer(vm: LauncherViewModel) {
    val home by vm.home.collectAsStateWithLifecycle()
    val backgroundBitmap by vm.background.collectAsStateWithLifecycle()
    val background = remember(backgroundBitmap) { backgroundBitmap?.asImageBitmap() }
    val density = LocalDensity.current
    val actions = remember(vm, density) { homeActions(vm, density) }
    val input = vm.homeInput
    val shift = remember { Animatable(0f) }
    LaunchedEffect(input.exit) {
        if (input.exit == null) {
            shift.snapTo(0f)
        } else {
            shift.animateTo(1f, tween(Motion.TRANSITION_MS))
            input.completeSwipeLaunch()
        }
    }
    HomeScreen(
        background = background,
        state = home,
        minute = vm.minute,
        actions = actions,
        gesturesEnabled = { input.gesturesEnabled },
        modifier = Modifier.zIndex(0f).graphicsLayer { slideOut(input.exit, shift.value) },
    )
}

@Composable
private fun MessageLayer(vm: LauncherViewModel) {
    val navigator = vm.navigator
    navigator.renameTarget?.let { key ->
        Box(Modifier.zIndex(4f)) { RenameDialog(vm, key) }
    }
    if (vm.homeInput.learnHint) {
        Box(Modifier.zIndex(5f).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LearnHint(onOpen = vm.homeInput::openLearning, onDismiss = { vm.homeInput.learnHint = false })
        }
    }
    navigator.notice?.let { notice ->
        val top = navigator.overlay == Overlay.Search
        Box(
            Modifier.zIndex(5f).fillMaxSize(),
            contentAlignment = if (top) Alignment.TopCenter else Alignment.BottomCenter,
        ) {
            NoticeBar(notice, top) { navigator.notice = null }
        }
    }
}

private fun GraphicsLayerScope.slideOut(direction: SwipeDirection?, progress: Float) {
    when (direction) {
        SwipeDirection.RIGHT -> translationX = size.width * progress
        SwipeDirection.LEFT -> translationX = -size.width * progress
        SwipeDirection.UP -> translationY = -size.height * progress
        SwipeDirection.DOWN -> translationY = size.height * progress
        null -> Unit
    }
}

private fun homeActions(vm: LauncherViewModel, density: Density) = object : HomeActions {
    private val navigator get() = vm.navigator

    override fun onRowTap(row: HomeRow, index: Int) {
        vm.launches.launch(row.key, fromSearch = false)
    }

    override fun onLongPress(position: Offset, row: HomeRow?) {
        navigator.overlay = Overlay.Popup(position, row?.key)
    }

    override fun onDoubleTap() = navigator.openSearch(SearchMode.TYPING)

    override fun onSwipe(direction: SwipeDirection, fromShadeZone: Boolean) =
        vm.homeInput.onSwipe(direction, fromShadeZone)

    override fun onRequestRole() = vm.request(LauncherEffect.RequestHomeRole)

    override fun onCapacityChanged(capacity: Int) = vm.setCapacity(capacity)

    override fun onClockTap() = vm.request(LauncherEffect.OpenClock)

    override fun onClockMoved(position: ClockPosition) = vm.preferences.update { it.copy(clockPosition = position) }

    override fun onEdit() = vm.homeInput.editCurrentSpace()

    override fun onAddApps() = vm.homeInput.addApps()

    override fun onSuggestion(anchor: Offset) {
        navigator.overlay = Overlay.Suggestion(anchor)
    }

    override fun onListOffset(dp: Int) {
        vm.model.currentSpace?.let { vm.spaces.setListOffset(it.id, dp) }
    }

    override fun onDraw(points: List<Offset>, width: Float) =
        vm.homeInput.onDrawing(points.map { GesturePoint(it.x, it.y) }, width, density.density)
}
