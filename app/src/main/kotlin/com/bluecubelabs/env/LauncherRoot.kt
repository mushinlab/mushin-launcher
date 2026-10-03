package com.bluecubelabs.env

import androidx.activity.compose.BackHandler
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.EnvType
import com.bluecubelabs.env.core.model.GestureAction
import com.bluecubelabs.env.core.model.LauncherSettings
import com.bluecubelabs.env.core.model.PalettePreset
import com.bluecubelabs.env.core.model.SwipeDirection
import com.bluecubelabs.env.core.search.KeyboardLayouts
import com.bluecubelabs.env.core.ui.EnvTheme
import com.bluecubelabs.env.core.ui.EnvType as Type
import com.bluecubelabs.env.core.ui.LocalEnvColors
import com.bluecubelabs.env.core.ui.LocalIconProvider
import com.bluecubelabs.env.core.ui.pressable
import com.bluecubelabs.env.feature.home.HomeActions
import com.bluecubelabs.env.feature.home.HomePopup
import com.bluecubelabs.env.feature.home.HomeRow
import com.bluecubelabs.env.feature.home.HomeScreen
import com.bluecubelabs.env.feature.home.PopupActions
import com.bluecubelabs.env.feature.home.PopupApp
import com.bluecubelabs.env.feature.search.SearchActions
import com.bluecubelabs.env.feature.search.SearchChrome
import com.bluecubelabs.env.feature.search.SearchScreen
import com.bluecubelabs.env.feature.settings.SettingsActions
import com.bluecubelabs.env.feature.settings.SettingsHost
import com.bluecubelabs.env.feature.settings.TextInputDialog
import kotlinx.coroutines.delay

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

    EnvTheme(palette) {
        CompositionLocalProvider(LocalIconProvider provides iconProvider) {
            Box(Modifier.fillMaxSize().background(LocalEnvColors.current.background)) {
                val homeActions = remember(activity) { homeActions(activity, vm) }
                HomeScreen(
                    state = home,
                    minute = activity.minute,
                    actions = homeActions,
                    gesturesEnabled = { activity.overlay == Overlay.None && !activity.settingsStack.isOpen },
                    modifier = Modifier.zIndex(0f),
                )
                SearchLayer(activity, vm, settings)
                PopupLayer(activity, vm)
                if (activity.settingsStack.isOpen) {
                    Box(Modifier.zIndex(3f)) { SettingsLayer(activity, vm) }
                }
                activity.renameTarget?.let { key ->
                    Box(Modifier.zIndex(4f)) { RenameDialog(activity, vm, key) }
                }
                activity.notice?.let { notice ->
                    Box(Modifier.zIndex(5f).fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        NoticeBar(notice) { activity.notice = null }
                    }
                }
            }
        }
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

    override fun onSwipe(direction: SwipeDirection) {
        activity.onSwipe(direction)
    }

    override fun onRequestRole() {
        activity.requestRole()
    }

    override fun onCapacityChanged(capacity: Int) {
        vm.setCapacity(capacity)
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
    LaunchedEffect(index, usage) { activity.search.update(index, usage) }
    LaunchedEffect(engine) { activity.search.setEngine(engine) }
    if (!composed && activity.overlay != Overlay.Search) return
    val active = activity.overlay == Overlay.Search
    val enabledLayouts = settings.keyboardLayouts.ifEmpty { listOf(KeyboardLayouts.EN.id) }
    val layout = KeyboardLayouts.byId(settings.activeKeyboardLayout)?.takeIf { it.id in enabledLayouts }
        ?: KeyboardLayouts.byId(enabledLayouts.first())
        ?: KeyboardLayouts.EN
    val next =
        KeyboardLayouts.byId(enabledLayouts[(enabledLayouts.indexOf(layout.id) + 1).mod(enabledLayouts.size)]) ?: layout
    val chrome = remember(settings.iconMode, layout, next, settings.keyboardDigitHints, settings.keyboardHaptics) {
        SearchChrome(settings.iconMode, layout, next.label, settings.keyboardDigitHints, settings.keyboardHaptics)
    }
    val actions = remember(activity) { searchActions(activity) }
    Box(
        Modifier
            .zIndex(if (active) 1f else -1f)
            .graphicsLayer { alpha = if (active) 1f else 0f }
            .then(if (active) Modifier else Modifier.clearAndSetSemantics { })
            .drawWithContent {
                drawContent()
                if (active) activity.onSearchDrawn()
            },
    ) {
        SearchScreen(activity.search, activity.searchMode, recents, chrome, actions)
    }
}

private fun searchActions(activity: MainActivity) = object : SearchActions {
    override fun onLaunch(key: AppKey) {
        activity.launch(key, fromSearch = true)
    }

    override fun onShowKeyboard() {
        activity.searchMode = com.bluecubelabs.env.feature.search.SearchMode.TYPING
    }

    override fun onChar(c: Char): Boolean = activity.search.type(c)

    override fun onBackspace(): Boolean = activity.search.backspace()

    override fun onClear() {
        activity.search.clear()
    }

    override fun onEnter() {
        activity.launchSearchTarget()
    }

    override fun onSwitchLayout() {
        activity.switchLayout()
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

            override fun setPalette(id: Long, preset: PalettePreset) = vm.setPalette(id, preset)

            override fun setEnvItems(id: Long, keys: List<AppKey>) = vm.setEnvItems(id, keys)

            override fun setGesture(direction: SwipeDirection, action: GestureAction) = vm.setGesture(direction, action)

            override fun setHidden(key: AppKey, hidden: Boolean) = vm.setHidden(key, hidden)

            override fun rename(key: AppKey, label: String?) = vm.rename(key, label)

            override fun clearHistory(envId: Long) = vm.clearHistory(envId)

            override fun requestDefaultLauncher() = activity.requestRole()
        }
    }
    SettingsHost(activity.settingsStack, state, actions)
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
private fun NoticeBar(notice: Notice, onDismiss: () -> Unit) {
    val colors = LocalEnvColors.current
    LaunchedEffect(notice.id) {
        delay(NOTICE_MS)
        onDismiss()
    }
    Row(
        Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
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
