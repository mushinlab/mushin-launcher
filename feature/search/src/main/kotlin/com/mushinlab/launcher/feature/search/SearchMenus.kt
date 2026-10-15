package com.mushinlab.launcher.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.ui.AnchoredPanel
import com.mushinlab.launcher.core.ui.HorizontalDivider
import com.mushinlab.launcher.core.ui.PanelHeader
import com.mushinlab.launcher.core.ui.PanelItem

enum class SearchOption { FUZZY, LAYOUT_SWAP, TRANSLIT, COLORED_ICONS }

interface SearchMenuActions {
    fun onRename(key: AppKey)

    fun onToggleInSpace(key: AppKey, add: Boolean)

    fun onAppInfo(key: AppKey)

    fun onUninstall(key: AppKey)

    fun onToggleHidden(key: AppKey, hidden: Boolean)

    fun onToggleShortcut(key: AppKey, bound: Boolean)

    fun onDismissMenu()
}

interface SearchOptionsActions {
    fun onToggleShowHidden()

    fun onSearchOption(option: SearchOption, enabled: Boolean)

    fun onClearHistory()
}

@Composable
internal fun OptionsPanel(
    anchor: Offset,
    chrome: SearchChrome,
    showHidden: Boolean,
    actions: SearchOptionsActions,
    onDismiss: () -> Unit,
) {
    var confirm by remember { mutableStateOf(false) }
    AnchoredPanel(anchor = anchor, onDismiss = onDismiss) {
        if (confirm) {
            PanelHeader(stringResource(R.string.search_clear_confirm))
            PanelItem(stringResource(R.string.search_clear), tag = "search_clear_confirm") {
                actions.onClearHistory()
                onDismiss()
            }
            PanelItem(stringResource(R.string.search_cancel), tag = "search_clear_cancel") { confirm = false }
        } else {
            PanelHeader(stringResource(R.string.search_options))
            listOf(
                Triple(SearchOption.FUZZY, R.string.search_fuzzy, chrome.fuzzy),
                Triple(SearchOption.LAYOUT_SWAP, R.string.search_layout_swap, chrome.layoutSwap),
                Triple(SearchOption.TRANSLIT, R.string.search_translit, chrome.translit),
                Triple(SearchOption.COLORED_ICONS, R.string.search_colored_icons, chrome.coloredIcons),
            ).forEach { (option, label, on) ->
                PanelItem(
                    (if (on) "✓  " else "     ") + stringResource(label),
                    tag = "search_option_$option",
                ) { actions.onSearchOption(option, !on) }
            }
            PanelItem(
                (if (showHidden) "✓  " else "     ") + stringResource(R.string.search_show_hidden),
                tag = "search_option_show_hidden",
            ) { actions.onToggleShowHidden() }
            HorizontalDivider()
            PanelItem(stringResource(R.string.search_clear_history), tag = "search_clear_history") { confirm = true }
        }
    }
}

@Composable
internal fun SearchMenuPanel(menu: SearchMenu, actions: SearchMenuActions) {
    AnchoredPanel(anchor = menu.anchor, onDismiss = actions::onDismissMenu) {
        PanelHeader(menu.label)
        PanelItem(
            stringResource(if (menu.inSpace) R.string.menu_remove_from_space else R.string.menu_add_to_space),
            tag = "menu_env",
        ) { actions.onToggleInSpace(menu.key, add = !menu.inSpace) }
        PanelItem(
            stringResource(if (menu.hidden) R.string.menu_unhide else R.string.menu_hide),
            tag = "menu_hide",
        ) { actions.onToggleHidden(menu.key, !menu.hidden) }
        PanelItem(
            (if (menu.shortcut) "✓  " else "     ") + stringResource(R.string.menu_system_shortcut),
            tag = "menu_shortcut",
        ) { actions.onToggleShortcut(menu.key, !menu.shortcut) }
        PanelItem(stringResource(R.string.menu_rename), tag = "menu_rename") { actions.onRename(menu.key) }
        PanelItem(stringResource(R.string.menu_app_info), tag = "menu_info") { actions.onAppInfo(menu.key) }
        if (menu.canUninstall) {
            PanelItem(stringResource(R.string.menu_uninstall), tag = "menu_uninstall") { actions.onUninstall(menu.key) }
        }
    }
}
