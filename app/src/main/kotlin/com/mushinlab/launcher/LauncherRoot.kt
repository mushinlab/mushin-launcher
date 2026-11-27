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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.model.WidgetCell
import com.mushinlab.launcher.core.search.GesturePoint
import com.mushinlab.launcher.core.ui.LocalIconProvider
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.core.ui.MushinFont
import com.mushinlab.launcher.core.ui.MushinStroke
import com.mushinlab.launcher.core.ui.MushinTheme
import com.mushinlab.launcher.core.ui.popupBackdrop
import com.mushinlab.launcher.feature.home.HomeActions
import com.mushinlab.launcher.feature.home.HomeRow
import com.mushinlab.launcher.feature.home.HomeScreen
import com.mushinlab.launcher.feature.home.WidgetContent
import com.mushinlab.launcher.feature.settings.SettingsRoute

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
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(LocalMushinColors.current.background)
                    .semantics { testTagsAsResourceId = true }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Press) vm.privateSpace.touched()
                            }
                        }
                    },
            ) {
                HomeLayer(vm, iconProvider)
                SearchLayer(vm, settings, iconProvider)
                PopupLayer(vm)
                FolderLayer(vm)
                SuggestionLayer(vm)
                Box(Modifier.zIndex(3f)) { HomeEditLayer(vm) }
                Box(Modifier.zIndex(3f)) { SettingsLayer(vm) }
                OnboardingLayer(vm)
                MessageLayer(vm)
                PrivateHintLayer(vm)
                PrivateGateLayer(vm)
            }
        }
    }
}

@Composable
private fun HomeLayer(vm: LauncherViewModel, iconProvider: LauncherIconProvider) {
    val home by vm.home.collectAsStateWithLifecycle()
    val backgroundBitmap by vm.background.collectAsStateWithLifecycle()
    val background = remember(backgroundBitmap) { backgroundBitmap?.asImageBitmap() }
    val request by vm.pager.request.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val pager = remember(vm) { SpacePagerMotion(vm.pager, scope) }
    val actions = remember(vm, density, pager) { homeActions(vm, density, pager) }
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
    LaunchedEffect(request) { request?.let { pager.play(it) } }
    val widgets: WidgetContent =
        remember(vm) { { id, height, width -> HostedWidgetView(id, vm.widgetHost, height, width) } }
    Box(
        Modifier
            .zIndex(0f)
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { pager.width = it.width.toFloat() }
            .graphicsLayer { slideOut(input.exit, shift.value) }
            .popupBackdrop(
                vm.navigator.overlay.let { it is Overlay.Popup || it is Overlay.Suggestion || it is Overlay.Folder },
            ),
    ) {
        HomeScreen(
            background = background,
            widgets = widgets,
            state = home,
            minute = vm.minute,
            actions = actions,
            gesturesEnabled = { input.gesturesEnabled && !pager.busy },
            editing = vm.navigator.homeEditing,
            modifier = Modifier
                .drawWithContent { translate(left = pager.offset * size.width) { this@drawWithContent.drawContent() } }
                .graphicsLayer(),
        )
        PagerPages(pager, iconProvider, widgets, vm.minute, actions)
    }
}

@Composable
private fun PagerPages(
    pager: SpacePagerMotion,
    iconProvider: LauncherIconProvider,
    widgets: WidgetContent,
    minute: State<Long>,
    actions: HomeActions,
) {
    pager.side?.let { side ->
        SpacePageView(
            side,
            iconProvider,
            widgets,
            minute,
            actions,
            Modifier
                .drawWithContent {
                    val from = if (pager.offset < 0f) 1f else -1f
                    translate(left = (pager.offset + from) * size.width) { this@drawWithContent.drawContent() }
                }
                .graphicsLayer(),
        )
    }
    pager.cover?.let { cover ->
        SpacePageView(
            cover,
            iconProvider,
            widgets,
            minute,
            actions,
            Modifier.graphicsLayer { alpha = pager.coverAlpha },
        )
    }
}

@Composable
private fun MessageLayer(vm: LauncherViewModel) {
    val navigator = vm.navigator
    navigator.renameTarget?.let { key ->
        Box(Modifier.zIndex(4f)) { RenameDialog(vm, key) }
    }
    if (vm.homeInput.learnHint) {
        Box(Modifier.zIndex(5f).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LearnHint(
                onOpen = vm.homeInput::openLearning,
                onDismiss = { vm.homeInput.learnHint = false },
                onNever = {
                    vm.homeInput.learnHint = false
                    vm.preferences.update { it.copy(learnHints = false) }
                },
            )
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

private fun homeActions(vm: LauncherViewModel, density: Density, pager: SpacePagerMotion) = object : HomeActions {
    private val navigator get() = vm.navigator

    override fun onRowTap(row: HomeRow, index: Int, position: Offset) {
        val folder = row.folderId
        when {
            folder == null -> vm.launches.launch(row.key, fromSearch = false)
            vm.folderRows.value[folder].isNullOrEmpty() -> navigator.openSettings(SettingsRoute.HomeFolder(folder))
            else -> navigator.overlay = Overlay.Folder(folder, position)
        }
    }

    override fun onLongPress(position: Offset, row: HomeRow?) {
        navigator.overlay = Overlay.Popup(position, row?.key)
    }

    override fun onOpenSpaces(anchor: Offset) {
        navigator.overlay = Overlay.Popup(anchor, null)
    }

    override fun onSwipe(direction: SwipeDirection, fromShadeZone: Boolean) =
        vm.homeInput.onSwipe(direction, fromShadeZone)

    override fun onBarDrag(dx: Float) = pager.drag(dx)

    override fun onBarRelease(direction: SwipeDirection?) = pager.release(direction)

    override fun onRequestRole() = vm.request(LauncherEffect.RequestHomeRole)

    override fun onCapacityChanged(capacity: Int) = vm.setCapacity(capacity)

    override fun onClockTap() = vm.request(LauncherEffect.OpenClock)

    override fun onClockMoved(position: ClockPosition) = vm.preferences.update { it.copy(clockPosition = position) }

    override fun onEdit() = navigator.toggleHomeEditing()

    override fun onEditClock() = navigator.openSettings(SettingsRoute.ClockSheet)

    override fun onEditBackground() {
        vm.model.currentSpace?.let { vm.backgrounds.openFromHome(it.id) }
    }

    override fun onEditText() = navigator.openSettings(SettingsRoute.TextSheet)

    override fun onEditTheme() = navigator.openSettings(SettingsRoute.ThemeSheet)

    override fun onEditMotion() = navigator.openSettings(SettingsRoute.MotionSheet)

    override fun onAddApps() = vm.homeInput.addApps()

    override fun onAddFolder() = vm.addFolder()

    override fun onOpenSettings() = navigator.openSettings()

    override fun onPlaceWidget(id: Int, cell: WidgetCell) = vm.widgets.panels.place(id, cell)

    override fun onAddWidget(cell: WidgetCell) = vm.widgets.add(cell)

    override fun onRemoveWidget(id: Int) = vm.widgets.panels.remove(id)

    override fun onEditTips() {
        navigator.overlay = Overlay.EditTips
    }

    override fun onEditFolder(folderId: Long) {
        navigator.openSettings(SettingsRoute.HomeFolder(folderId))
    }

    override fun onRemoveRow(row: HomeRow) {
        val folder = row.folderId
        if (folder != null) vm.folders.delete(folder) else vm.model.removeFromCurrentSpace(row.key)
    }

    override fun onMoveRow(key: AppKey, target: AppKey) {
        val space = vm.model.currentSpace ?: return
        vm.spaces.moveItem(space.id, key, target)
    }

    override fun onSuggestion(anchor: Offset) {
        navigator.overlay = Overlay.Suggestion(anchor)
    }

    override fun onDraw(points: List<Offset>, width: Float) =
        vm.homeInput.onDrawing(points.map { GesturePoint(it.x, it.y) }, width, density.density)
}
