package com.mushinlab.launcher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.swipesByApp
import com.mushinlab.launcher.core.search.KeyboardLayouts
import com.mushinlab.launcher.core.ui.LocalIconProvider
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.feature.search.SearchActions
import com.mushinlab.launcher.feature.search.SearchChrome
import com.mushinlab.launcher.feature.search.SearchOption
import com.mushinlab.launcher.feature.search.SearchScreen
import kotlinx.coroutines.launch

@Composable
internal fun SearchLayer(vm: LauncherViewModel, settings: LauncherSettings, iconProvider: LauncherIconProvider) {
    var composed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        composed = true
    }
    SearchSourcesEffect(vm)
    val active = vm.navigator.overlay == Overlay.Search
    val progress = remember { Animatable(0f) }
    LaunchedEffect(active) { progress.animateTo(if (active) 1f else 0f, tween(Motion.TRANSITION_MS)) }
    val shown by remember { derivedStateOf { progress.value > 0f } }
    if (!composed && !active) return
    val slidePx = with(LocalDensity.current) { SEARCH_SLIDE.toPx() }
    val chrome = rememberSearchChrome(settings)
    val actions = remember(vm) { searchActions(vm) }
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
                if (active) vm.navigator.onSearchDrawn()
            },
    ) {
        val provider = LocalIconProvider.current
        val searchIcons = remember(provider, settings.searchColoredIcons) {
            if (settings.searchColoredIcons) iconProvider.withColors(true) else provider
        }
        CompositionLocalProvider(LocalIconProvider provides searchIcons) {
            SearchScreen(vm.navigator.search, vm.navigator.searchMode, chrome, actions, active = active)
        }
    }
}

@Composable
private fun SearchSourcesEffect(vm: LauncherViewModel) {
    val sources = vm.navigator.search.sources
    val index by vm.searchIndex.collectAsStateWithLifecycle()
    val usage by vm.usage.collectAsStateWithLifecycle()
    val engine by vm.searchEngine.collectAsStateWithLifecycle()
    val learned by vm.learnedGestures.collectAsStateWithLifecycle()
    val recents by vm.recents.collectAsStateWithLifecycle()
    LaunchedEffect(index, usage) { sources.update(index, usage) }
    LaunchedEffect(recents) { sources.setRecents(recents) }
    LaunchedEffect(learned) { sources.setLearned(learned) }
    val bindings by vm.gestureBindings.collectAsStateWithLifecycle()
    LaunchedEffect(bindings) { sources.setSwipes(bindings.swipesByApp()) }
    LaunchedEffect(engine) { sources.setEngine(engine) }
}

@Composable
private fun rememberSearchChrome(settings: LauncherSettings): SearchChrome {
    val enabledLayouts = settings.keyboardLayouts.ifEmpty { listOf(KeyboardLayouts.EN.id) }
    val layout = KeyboardLayouts.byId(settings.activeKeyboardLayout)?.takeIf { it.id in enabledLayouts }
        ?: KeyboardLayouts.byId(enabledLayouts.first())
        ?: KeyboardLayouts.EN
    val next =
        KeyboardLayouts.byId(enabledLayouts[(enabledLayouts.indexOf(layout.id) + 1).mod(enabledLayouts.size)]) ?: layout
    return remember(settings, layout, next) {
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
}

private fun Modifier.blockInput(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
    }
}

private fun searchActions(vm: LauncherViewModel) = object : SearchActions {
    override fun onLaunch(key: AppKey) {
        vm.launches.launch(key, fromSearch = true)
    }

    override fun onRename(key: AppKey) {
        vm.navigator.search.panels.closeMenu()
        vm.navigator.renameTarget = key
    }

    override fun onLongPress(key: AppKey, label: String, anchor: Offset) {
        vm.searchInput.openMenu(key, label, anchor)
    }

    override fun onToggleInSpace(key: AppKey, add: Boolean) {
        vm.searchInput.toggleInSpace(key, add)
    }

    override fun onAppInfo(key: AppKey) {
        vm.navigator.search.panels.closeMenu()
        vm.apps.openDetails(key)
    }

    override fun onUninstall(key: AppKey) {
        vm.navigator.search.panels.closeMenu()
        vm.apps.uninstall(key)
    }

    override fun onToggleHidden(key: AppKey, hidden: Boolean) {
        vm.searchInput.toggleHidden(key, hidden)
    }

    override fun onToggleShowHidden() {
        vm.navigator.search.toggleShowHidden()
    }

    override fun onDismissMenu() {
        vm.navigator.search.panels.closeMenu()
    }

    override fun onSearchOption(option: SearchOption, enabled: Boolean) {
        vm.preferences.update {
            when (option) {
                SearchOption.FUZZY -> it.copy(searchFuzzy = enabled)
                SearchOption.LAYOUT_SWAP -> it.copy(searchLayoutSwap = enabled)
                SearchOption.TRANSLIT -> it.copy(searchTranslit = enabled)
                SearchOption.COLORED_ICONS -> it.copy(searchColoredIcons = enabled)
            }
        }
    }

    override fun onOpenSettings() {
        vm.navigator.closeOverlays()
        vm.navigator.openSettings()
    }

    override fun onClose() {
        if (vm.navigator.overlay == Overlay.Search) vm.navigator.closeOverlays()
    }

    override fun onShowKeyboard() {
        vm.navigator.searchMode = com.mushinlab.launcher.feature.search.SearchMode.TYPING
    }

    override fun onChar(c: Char): Boolean = vm.navigator.search.type(c)

    override fun onBackspace(): Boolean = vm.navigator.search.backspace()

    override fun onClear() {
        vm.navigator.search.clear()
    }

    override fun onSwitchLayout() {
        vm.searchInput.switchLayout()
    }

    override fun onResize() {
        vm.navigator.search.panels.toggleResize()
    }

    override fun onKeyboardFit(width: Float, shift: Float) {
        vm.preferences.update { it.copy(keyboardWidth = width, keyboardShift = shift) }
    }
}

private val SEARCH_SLIDE = 24.dp
