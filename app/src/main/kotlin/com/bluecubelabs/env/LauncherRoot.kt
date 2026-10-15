package com.bluecubelabs.env

import androidx.activity.compose.BackHandler
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.ClockPosition
import com.bluecubelabs.env.core.model.EnvType
import com.bluecubelabs.env.core.model.GestureAction
import com.bluecubelabs.env.core.model.LauncherSettings
import com.bluecubelabs.env.core.model.PalettePreset
import com.bluecubelabs.env.core.model.SwipeDirection
import com.bluecubelabs.env.core.model.swipesByApp
import com.bluecubelabs.env.core.search.GestureSample
import com.bluecubelabs.env.core.search.KeyboardLayouts
import com.bluecubelabs.env.core.ui.EnvFont
import com.bluecubelabs.env.core.ui.EnvStroke
import com.bluecubelabs.env.core.ui.EnvTheme
import com.bluecubelabs.env.core.ui.EnvType as Type
import com.bluecubelabs.env.core.ui.LocalEnvColors
import com.bluecubelabs.env.core.ui.LocalIconProvider
import com.bluecubelabs.env.core.ui.LocalPressFeedback
import com.bluecubelabs.env.core.ui.Motion
import com.bluecubelabs.env.core.ui.pressable
import com.bluecubelabs.env.feature.home.HomeActions
import com.bluecubelabs.env.feature.home.HomePopup
import com.bluecubelabs.env.feature.home.HomeRow
import com.bluecubelabs.env.feature.home.HomeScreen
import com.bluecubelabs.env.feature.home.HomeSuggestion
import com.bluecubelabs.env.feature.home.PopupActions
import com.bluecubelabs.env.feature.home.PopupApp
import com.bluecubelabs.env.feature.home.SuggestionActions
import com.bluecubelabs.env.feature.home.SuggestionPanel
import com.bluecubelabs.env.feature.search.SearchActions
import com.bluecubelabs.env.feature.search.SearchChrome
import com.bluecubelabs.env.feature.search.SearchOption
import com.bluecubelabs.env.feature.search.SearchScreen
import com.bluecubelabs.env.feature.settings.SettingsActions
import com.bluecubelabs.env.feature.settings.SettingsHost
import com.bluecubelabs.env.feature.settings.SettingsRoute
import com.bluecubelabs.env.feature.settings.TextInputDialog
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LauncherRoot(activity: MainActivity, vm: LauncherViewModel, iconProvider: LauncherIconProvider) {
    val palette by vm.palette.collectAsStateWithLifecycle()
    val home by vm.home.collectAsStateWithLifecycle()
    val ready by vm.ready.collectAsStateWithLifecycle()
    val iconsReady by vm.iconsReady.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    LaunchedEffect(palette) { activity.applySystemBars(palette) }
    ReportDrawnWhen { ready && iconsReady }
    BackHandler(enabled = true) { activity.handleBack() }

    val colored by vm.coloredIcons.collectAsStateWithLifecycle()
    val backgroundBitmap by (activity.application as EnvApp).container.background.image.collectAsStateWithLifecycle()
    val background = remember(backgroundBitmap) { backgroundBitmap?.asImageBitmap() }
    val icons = remember(iconProvider, colored) { iconProvider.withColors(colored) }
    val font = remember(settings.font, settings.fontWeight) { EnvFont.of(settings.font, settings.fontWeight) }
    val stroke = remember(settings.strokeWidth, settings.strokeColor, settings.strokeDots) {
        EnvStroke(settings.strokeWidth.dp, settings.strokeColor, settings.strokeDots)
    }
    EnvTheme(palette, font, stroke) {
        CompositionLocalProvider(
            LocalIconProvider provides icons,
            LocalPressFeedback provides settings.pressFeedback,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(LocalEnvColors.current.background)
                    .semantics { testTagsAsResourceId = true },
            ) {
                val homeActions = remember(activity) { homeActions(activity, vm) }
                val homeShift = remember { Animatable(0f) }
                LaunchedEffect(activity.homeExit) {
                    if (activity.homeExit == null) {
                        homeShift.snapTo(0f)
                    } else {
                        homeShift.animateTo(1f, tween(Motion.TRANSITION_MS))
                        activity.completeSwipeLaunch()
                    }
                }
                HomeScreen(
                    background = background,
                    state = home,
                    minute = activity.minute,
                    actions = homeActions,
                    gesturesEnabled = {
                        activity.overlay == Overlay.None && !activity.settingsStack.isOpen && activity.homeExit == null
                    },
                    modifier = Modifier.zIndex(0f).graphicsLayer { slideOut(activity.homeExit, homeShift.value) },
                )
                SearchLayer(activity, vm, settings)
                PopupLayer(activity, vm)
                SuggestionLayer(activity, vm, home.suggestion)
                Box(Modifier.zIndex(3f)) { SettingsLayer(activity, vm) }
                activity.renameTarget?.let { key ->
                    Box(Modifier.zIndex(4f)) { RenameDialog(activity, vm, key) }
                }
                if (activity.learnHint) {
                    Box(Modifier.zIndex(5f).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        LearnHint(onOpen = activity::openLearning, onDismiss = { activity.learnHint = false })
                    }
                }
                activity.notice?.let { notice ->
                    val top = activity.overlay == Overlay.Search
                    Box(
                        Modifier.zIndex(5f).fillMaxSize(),
                        contentAlignment = if (top) Alignment.TopCenter else Alignment.BottomCenter,
                    ) {
                        NoticeBar(notice, top) { activity.notice = null }
                    }
                }
            }
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

private fun homeActions(activity: MainActivity, vm: LauncherViewModel) = object : HomeActions {
    override fun onRowTap(row: HomeRow, index: Int) {
        activity.launch(row.key, fromSearch = false)
    }

    override fun onLongPress(position: Offset, row: HomeRow?) {
        activity.overlay = Overlay.Popup(position, row?.key)
    }

    override fun onDoubleTap() {
        activity.openSearch(com.bluecubelabs.env.feature.search.SearchMode.TYPING)
    }

    override fun onSwipe(direction: SwipeDirection, fromShadeZone: Boolean) {
        activity.onSwipe(direction, fromShadeZone)
    }

    override fun onRequestRole() {
        activity.requestRole()
    }

    override fun onCapacityChanged(capacity: Int) {
        vm.setCapacity(capacity)
    }

    override fun onClockTap() {
        activity.openClock()
    }

    override fun onClockMoved(position: ClockPosition) {
        vm.updateSettings { it.copy(clockPosition = position) }
    }

    override fun onEdit() {
        activity.editCurrentSpace()
    }

    override fun onSuggestion(anchor: Offset) {
        activity.overlay = Overlay.Suggestion(anchor)
    }

    override fun onListOffset(dp: Int) {
        vm.currentEnvId()?.let { vm.spaces.setListOffset(it, dp) }
    }

    override fun onDraw(points: List<Offset>, width: Float) {
        activity.onHomeDrawing(points, width)
    }
}

@Composable
private fun SuggestionLayer(activity: MainActivity, vm: LauncherViewModel, suggestion: HomeSuggestion?) {
    val overlay = activity.overlay as? Overlay.Suggestion ?: return
    if (suggestion == null) {
        LaunchedEffect(Unit) { activity.overlay = Overlay.None }
        return
    }
    val actions = remember(activity) {
        object : SuggestionActions {
            override fun onReplace(suggestion: HomeSuggestion) {
                activity.overlay = Overlay.None
                vm.currentEnvId()?.let { vm.suggestions.accept(it, suggestion, replace = true) }
            }

            override fun onAdd(suggestion: HomeSuggestion) {
                activity.overlay = Overlay.None
                vm.currentEnvId()?.let { vm.suggestions.accept(it, suggestion, replace = false) }
            }

            override fun onDismissSuggestion(suggestion: HomeSuggestion) {
                activity.overlay = Overlay.None
                vm.currentEnvId()?.let { vm.suggestions.dismiss(it, suggestion) }
            }

            override fun onClose() {
                activity.overlay = Overlay.None
            }
        }
    }
    Box(Modifier.zIndex(2f).fillMaxSize()) {
        SuggestionPanel(overlay.anchor, suggestion, actions)
    }
}

@Composable
private fun SearchLayer(activity: MainActivity, vm: LauncherViewModel, settings: LauncherSettings) {
    var composed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        composed = true
    }
    val index by vm.searchIndex.collectAsStateWithLifecycle()
    val usage by vm.usage.collectAsStateWithLifecycle()
    val engine by vm.searchEngine.collectAsStateWithLifecycle()
    val recents by vm.recents.collectAsStateWithLifecycle()
    val learned by vm.learnedGestures.collectAsStateWithLifecycle()
    LaunchedEffect(index, usage) { activity.search.update(index, usage) }
    LaunchedEffect(learned) { activity.search.setLearned(learned) }
    val bindings by vm.gestureBindings.collectAsStateWithLifecycle()
    LaunchedEffect(bindings) { activity.search.setSwipes(bindings.swipesByApp()) }
    val overrides by (activity.application as EnvApp).container.overrides.overrides.collectAsStateWithLifecycle()
    LaunchedEffect(overrides) {
        activity.search.setShortcuts(overrides.values.filter { it.systemShortcut }.map { it.key }.toSet())
    }
    LaunchedEffect(engine) { activity.search.setEngine(engine) }
    val active = activity.overlay == Overlay.Search
    val progress = remember { Animatable(0f) }
    LaunchedEffect(active) { progress.animateTo(if (active) 1f else 0f, tween(Motion.TRANSITION_MS)) }
    val shown by remember { derivedStateOf { progress.value > 0f } }
    if (!composed && !active) return
    val slidePx = with(LocalDensity.current) { SEARCH_SLIDE.toPx() }
    val enabledLayouts = settings.keyboardLayouts.ifEmpty { listOf(KeyboardLayouts.EN.id) }
    val layout = KeyboardLayouts.byId(settings.activeKeyboardLayout)?.takeIf { it.id in enabledLayouts }
        ?: KeyboardLayouts.byId(enabledLayouts.first())
        ?: KeyboardLayouts.EN
    val next =
        KeyboardLayouts.byId(enabledLayouts[(enabledLayouts.indexOf(layout.id) + 1).mod(enabledLayouts.size)]) ?: layout
    val chrome = remember(settings, layout, next) {
        SearchChrome(
            iconMode = settings.iconMode,
            layout = layout,
            nextLayoutLabel = next.label,
            digitHints = settings.keyboardDigitHints,
            haptics = settings.keyboardHaptics,
            gestureShiftPenalty = settings.gestureShiftPenalty,
            gestureSuggestMargin = settings.gestureSuggestMargin,
            fuzzy = settings.searchFuzzy,
            layoutSwap = settings.searchLayoutSwap,
            translit = settings.searchTranslit,
            coloredIcons = settings.searchColoredIcons,
            keyboardWidth = settings.keyboardWidth,
            keyboardShift = settings.keyboardShift,
        )
    }
    val actions = remember(activity) { searchActions(activity, vm) }
    Box(
        Modifier
            .zIndex(if (active || shown) 1f else -1f)
            .graphicsLayer {
                alpha = progress.value
                translationY = (1f - progress.value) * slidePx
            }
            .then(if (active) Modifier else Modifier.clearAndSetSemantics { }.blockInput())
            .drawWithContent {
                drawContent()
                if (active) activity.onSearchDrawn()
            },
    ) {
        val provider = LocalIconProvider.current
        val searchIcons = remember(provider, settings.searchColoredIcons) {
            if (settings.searchColoredIcons) activity.iconProvider.withColors(true) else provider
        }
        CompositionLocalProvider(LocalIconProvider provides searchIcons) {
            SearchScreen(activity.search, activity.searchMode, recents, chrome, actions)
        }
    }
}

private fun Modifier.blockInput(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
    }
}

private fun searchActions(activity: MainActivity, vm: LauncherViewModel) = object : SearchActions {
    private val container get() = (activity.application as EnvApp).container

    override fun onLaunch(key: AppKey) {
        activity.launch(key, fromSearch = true)
    }

    override fun onRename(key: AppKey) {
        activity.search.closeMenu()
        activity.renameTarget = key
    }

    override fun onLongPress(key: AppKey, label: String, anchor: Offset) {
        activity.openSearchMenu(key, label, anchor)
    }

    override fun onToggleInEnv(key: AppKey, add: Boolean) {
        activity.toggleInEnv(key, add)
    }

    override fun onAppInfo(key: AppKey) {
        activity.search.closeMenu()
        container.launcher.openDetails(key)
    }

    override fun onUninstall(key: AppKey) {
        activity.search.closeMenu()
        container.launcher.uninstall(key)
    }

    override fun onToggleHidden(key: AppKey, hidden: Boolean) {
        val label = activity.search.menu?.label ?: return
        activity.toggleHidden(key, label, hidden)
    }

    override fun onToggleShortcut(key: AppKey, bound: Boolean) {
        activity.search.closeMenu()
        vm.setSystemShortcut(key, bound)
    }

    override fun onToggleShowHidden() {
        activity.search.toggleShowHidden()
    }

    override fun onDismissMenu() {
        activity.search.closeMenu()
    }

    override fun onSearchOption(option: SearchOption, enabled: Boolean) {
        vm.updateSettings {
            when (option) {
                SearchOption.FUZZY -> it.copy(searchFuzzy = enabled)
                SearchOption.LAYOUT_SWAP -> it.copy(searchLayoutSwap = enabled)
                SearchOption.TRANSLIT -> it.copy(searchTranslit = enabled)
                SearchOption.COLORED_ICONS -> it.copy(searchColoredIcons = enabled)
            }
        }
    }

    override fun onClearHistory() {
        vm.currentEnvId()?.let(vm::clearHistory)
    }

    override fun onClose() {
        if (activity.overlay == Overlay.Search) activity.closeOverlays()
    }

    override fun onShowKeyboard() {
        activity.searchMode = com.bluecubelabs.env.feature.search.SearchMode.TYPING
    }

    override fun onChar(c: Char): Boolean = activity.search.type(c)

    override fun onBackspace(): Boolean = activity.search.backspace()

    override fun onClear() {
        activity.search.clear()
    }

    override fun onSwitchLayout() {
        activity.switchLayout()
    }

    override fun onResize() {
        activity.search.toggleResize()
    }

    override fun onKeyboardFit(width: Float, shift: Float) {
        vm.updateSettings { it.copy(keyboardWidth = width, keyboardShift = shift) }
    }
}

@Composable
private fun PopupLayer(activity: MainActivity, vm: LauncherViewModel) {
    val popup = activity.overlay as? Overlay.Popup ?: return
    val envs by vm.popupEnvs.collectAsStateWithLifecycle()
    val app = popup.app?.let { key ->
        val entry = vm.entry(key) ?: return@let null
        PopupApp(
            key = key,
            label = vm.override(key)?.customLabel ?: entry.label,
            hidden = vm.override(key)?.hidden == true,
            inEnv = vm.isInCurrentEnv(key),
            canUninstall = !entry.isSystem,
        )
    }
    val actions = remember(activity) { popupActions(activity, vm) }
    Box(Modifier.zIndex(2f).fillMaxSize()) {
        HomePopup(popup.anchor, app, envs, actions)
    }
}

private fun popupActions(activity: MainActivity, vm: LauncherViewModel) = object : PopupActions {
    private val container get() = (activity.application as EnvApp).container

    override fun onSelectEnv(id: Long) {
        vm.switchEnv(id)
        activity.overlay = Overlay.None
    }

    override fun onOpenSettings() {
        activity.overlay = Overlay.None
        activity.settingsStack.open()
    }

    override fun onOpenEnvSettings(id: Long) {
        activity.overlay = Overlay.None
        activity.settingsStack.open(SettingsRoute.EnvEditor(id))
    }

    override fun onAbout(key: AppKey) {
        activity.overlay = Overlay.None
        container.launcher.openDetails(key)
    }

    override fun onRename(key: AppKey) {
        activity.overlay = Overlay.None
        activity.renameTarget = key
    }

    override fun onToggleHidden(key: AppKey, hidden: Boolean) {
        activity.overlay = Overlay.None
        vm.setHidden(key, hidden)
    }

    override fun onRemoveFromEnv(key: AppKey) {
        activity.overlay = Overlay.None
        vm.removeFromCurrentEnv(key)
    }

    override fun onUninstall(key: AppKey) {
        activity.overlay = Overlay.None
        container.launcher.uninstall(key)
    }

    override fun onDismiss() {
        activity.overlay = Overlay.None
    }
}

@Composable
private fun SettingsLayer(activity: MainActivity, vm: LauncherViewModel) {
    val state by vm.settingsState.collectAsStateWithLifecycle()
    val actions = remember(activity) {
        object : SettingsActions {
            override fun updateSettings(
                transform: (LauncherSettings) -> LauncherSettings,
            ) = vm.updateSettings(transform)

            override suspend fun createEnv(name: String, type: EnvType): String? = vm.createEnv(name, type)

            override suspend fun renameEnv(id: Long, name: String): String? = vm.renameEnv(id, name)

            override fun deleteEnv(id: Long) = vm.deleteEnv(id)

            override fun setPalette(id: Long, preset: PalettePreset) = vm.spaces.setPalette(id, preset)

            override fun setColoredIcons(id: Long, enabled: Boolean) = vm.spaces.setColoredIcons(id, enabled)

            override fun setEnvItems(id: Long, keys: List<AppKey>) = vm.setEnvItems(id, keys)

            override fun addEnvItem(id: Long, key: AppKey) {
                vm.addEnvItem(id, key)
            }

            override fun removeEnvItem(id: Long, key: AppKey) = vm.removeEnvItem(id, key)

            override fun setGesture(direction: SwipeDirection, action: GestureAction) = vm.setGesture(direction, action)

            override fun setHidden(key: AppKey, hidden: Boolean) = vm.setHidden(key, hidden)

            override fun rename(key: AppKey, label: String?) = vm.rename(key, label)

            override fun switchEnv(id: Long) = vm.switchEnv(id)

            override fun requestDefaultLauncher() = activity.requestRole()

            override fun openNotificationAccess() = activity.openNotificationAccess()

            override fun openAssistantSettings() = activity.openAssistantSettings()

            override fun requestSearchTile() = activity.requestSearchTile()

            override fun pickBackground() = activity.pickBackground()

            override fun clearBackground() {
                val container = (activity.application as EnvApp).container
                container.scope.launch(Dispatchers.IO) { container.background.clear() }
            }

            override fun forgetDrawing(key: AppKey, path: String) = vm.memory.forget {
                it.key == key && it.path == path
            }

            override fun forgetApp(key: AppKey) = vm.memory.forget { it.key == key }

            override fun addDrawing(key: AppKey, sample: GestureSample) = vm.memory.add(key, sample)

            override fun forgetAllGestures() = vm.memory.forget { true }

            override fun clearUsage() = vm.memory.clearUsage()
        }
    }
    val background by (activity.application as EnvApp).container.background.image.collectAsStateWithLifecycle()
    SettingsHost(activity.settingsStack, state.copy(hasBackground = background != null), actions)
}

@Composable
private fun RenameDialog(activity: MainActivity, vm: LauncherViewModel, key: AppKey) {
    val entry = vm.entry(key)
    val current = vm.override(key)?.customLabel ?: entry?.label.orEmpty()
    TextInputDialog(
        title = stringResource(R.string.rename_title, entry?.label.orEmpty()),
        initial = current,
        maxLength = com.bluecubelabs.env.core.model.EnvLimits.LABEL_MAX_LENGTH,
        error = null,
        confirm = stringResource(R.string.rename_save),
        extra = stringResource(R.string.rename_reset) to {
            vm.rename(key, null)
            activity.renameTarget = null
        },
        onConfirm = { text ->
            vm.rename(key, text)
            activity.renameTarget = null
        },
        onDismiss = { activity.renameTarget = null },
    )
}

@Composable
private fun LearnHint(onOpen: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalEnvColors.current
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dismissPx = with(LocalDensity.current) { LEARN_HINT_SWIPE.toPx() }
    LaunchedEffect(Unit) {
        delay(LEARN_HINT_MS)
        onDismiss()
    }
    Column(
        Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(16.dp)
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offset.value
                alpha = 1f - (abs(offset.value) / (size.width.takeIf { it > 0f } ?: 1f)).coerceIn(0f, 1f)
            }
            .clip(RoundedCornerShape(LEARN_HINT_RADIUS))
            .background(colors.surface)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (abs(offset.value) > dismissPx) {
                                onDismiss()
                            } else {
                                offset.animateTo(0f, tween(Motion.TRANSITION_MS))
                            }
                        }
                    },
                ) { change, amount ->
                    change.consume()
                    scope.launch { offset.snapTo(offset.value + amount) }
                }
            }
            .pressable(onClick = onOpen)
            .semantics(mergeDescendants = true) { }
            .testTag("learn_hint")
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        BasicText(
            stringResource(R.string.learn_hint_title),
            style = Type.style(17.sp, colors.textPrimary, FontWeight.Medium),
        )
        BasicText(stringResource(R.string.learn_hint_subtitle), style = Type.style(15.sp, colors.accent))
    }
}

@Composable
private fun NoticeBar(notice: Notice, top: Boolean, onDismiss: () -> Unit) {
    val colors = LocalEnvColors.current
    LaunchedEffect(notice.id) {
        delay(NOTICE_MS)
        onDismiss()
    }
    Row(
        Modifier
            .windowInsetsPadding(if (top) WindowInsets.statusBars else WindowInsets.navigationBars)
            .padding(16.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(colors.surface)
            .testTag("notice")
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(notice.text, style = Type.style(15.sp, colors.textPrimary), modifier = Modifier.weight(1f))
        val label = notice.actionLabel
        val action = notice.action
        if (label != null && action != null) {
            Box(
                Modifier.heightIn(min = 48.dp).pressable {
                    action()
                    onDismiss()
                }.padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) { BasicText(label, style = Type.style(15.sp, colors.accent)) }
        }
    }
}

private const val NOTICE_MS = 4000L
private val SEARCH_SLIDE = 24.dp
private const val LEARN_HINT_MS = 8000L
private val LEARN_HINT_SWIPE = 96.dp
private val LEARN_HINT_RADIUS = 12.dp
