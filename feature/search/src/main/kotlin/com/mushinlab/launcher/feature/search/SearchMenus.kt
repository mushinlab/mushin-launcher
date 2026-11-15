package com.mushinlab.launcher.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.ui.AnchoredPanel
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.PanelHeader

interface SearchMenuActions {
    fun onRename(key: AppKey)

    fun onAppInfo(key: AppKey)

    fun onUninstall(key: AppKey)

    fun onToggleHidden(key: AppKey, hidden: Boolean)

    fun onDismissMenu()
}

interface SearchOptionsActions {
    fun onToggleShowHidden()

    fun onOpenSettings()
}

@Composable
internal fun SearchMenuPanel(menu: SearchMenu, actions: SearchMenuActions) {
    val colors = LocalMushinColors.current
    val tint = colors.textPrimary
    AnchoredPanel(anchor = menu.anchor, onDismiss = actions::onDismissMenu) {
        PanelHeader(menu.label)
        Row(
            Modifier.padding(start = MENU_ACTIONS_INSET, end = MENU_ACTIONS_INSET, bottom = MENU_ACTIONS_INSET),
            horizontalArrangement = Arrangement.spacedBy(MENU_ACTIONS_GAP),
        ) {
            if (menu.canUninstall) {
                GlyphButton(
                    Glyph.DELETE,
                    stringResource(R.string.menu_uninstall),
                    colors.danger,
                    tag = "menu_uninstall",
                ) { actions.onUninstall(menu.key) }
            }
            GlyphButton(Glyph.INFO, stringResource(R.string.menu_app_info), tint, tag = "menu_info") {
                actions.onAppInfo(menu.key)
            }
            GlyphButton(Glyph.EDIT, stringResource(R.string.menu_rename), tint, tag = "menu_rename") {
                actions.onRename(menu.key)
            }
            GlyphButton(
                if (menu.hidden) Glyph.SHOW else Glyph.HIDE,
                stringResource(if (menu.hidden) R.string.menu_unhide else R.string.menu_hide),
                tint,
                tag = "menu_hide",
            ) { actions.onToggleHidden(menu.key, !menu.hidden) }
        }
    }
}

private val MENU_ACTIONS_INSET = 12.dp
private val MENU_ACTIONS_GAP = 8.dp
