package com.mushinlab.launcher

import androidx.compose.ui.geometry.Offset
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.search.KeyboardLayouts
import com.mushinlab.launcher.feature.search.HardwareKey
import com.mushinlab.launcher.feature.search.SearchMenu
import com.mushinlab.launcher.feature.search.SearchMode
import kotlinx.collections.immutable.persistentListOf

class SearchInteractions(
    private val model: LauncherModel,
    private val navigator: LauncherNavigator,
    private val launches: AppLaunches,
    private val emit: (LauncherEffect) -> Unit,
) {
    private val search get() = navigator.search

    fun launchTarget() {
        val target = search.target()
        if (target == null) emit(LauncherEffect.Reject) else launches.launch(target.key, fromSearch = true)
    }

    fun openMenu(key: AppKey, label: String, anchor: Offset) {
        val entry = model.entry(key) ?: return
        val override = model.override(key)
        search.openMenu(
            SearchMenu(
                key = key,
                label = label,
                anchor = anchor,
                inSpace = model.isInCurrentSpace(key),
                canUninstall = !entry.isSystem,
                hidden = override?.hidden == true,
                shortcut = override?.systemShortcut == true,
            ),
        )
    }

    fun toggleHidden(key: AppKey, hidden: Boolean) {
        val label = search.menu?.label ?: return
        search.closeMenu()
        model.setHidden(key, hidden)
        navigator.notice =
            Notice(if (hidden) R.string.notice_hidden else R.string.notice_unhidden, persistentListOf(label))
    }

    fun toggleInSpace(key: AppKey, add: Boolean) {
        search.closeMenu()
        val space = model.currentSpace ?: return
        val names = persistentListOf(space.name)
        navigator.notice = when {
            !add -> {
                model.removeFromCurrentSpace(key)
                Notice(R.string.notice_removed_from_space, names)
            }

            model.addToCurrentSpace(key) -> Notice(R.string.notice_added_to_space, names)

            else -> Notice(R.string.notice_space_full, names)
        }
    }

    fun switchLayout() {
        val settings = model.settings
        val enabled = settings.keyboardLayouts.ifEmpty { listOf(KeyboardLayouts.EN.id) }
        val index = enabled.indexOf(settings.activeKeyboardLayout)
        model.setActiveLayout(enabled[(index + 1).mod(enabled.size)])
    }

    fun accepts(key: HardwareKey): Boolean =
        navigator.acceptsInput && (navigator.overlay == Overlay.Search || key is HardwareKey.Text)

    fun onHardwareKey(key: HardwareKey) {
        if (navigator.overlay != Overlay.Search) {
            if (key is HardwareKey.Text) navigator.openSearch(SearchMode.BROWSING, key.char)
            return
        }
        when (key) {
            is HardwareKey.Text -> if (!search.type(key.char)) emit(LauncherEffect.Reject)
            HardwareKey.Backspace -> search.backspace()
            HardwareKey.Enter -> launchTarget()
            HardwareKey.Escape -> navigator.closeOverlays()
            HardwareKey.Up -> search.moveSelection(1)
            HardwareKey.Down -> search.moveSelection(-1)
            HardwareKey.SwitchLayout -> switchLayout()
        }
    }
}
