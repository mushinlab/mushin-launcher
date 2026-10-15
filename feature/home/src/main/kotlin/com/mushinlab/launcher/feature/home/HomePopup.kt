package com.mushinlab.launcher.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.ui.AnchoredPanel
import com.mushinlab.launcher.core.ui.HorizontalDivider
import com.mushinlab.launcher.core.ui.PanelHeader
import com.mushinlab.launcher.core.ui.PanelItem
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class PopupSpace(val id: Long, val name: String, val current: Boolean)

@Immutable
data class PopupApp(
    val key: AppKey,
    val label: String,
    val hidden: Boolean,
    val inSpace: Boolean,
    val canUninstall: Boolean,
)

interface PopupActions {
    fun onSelectSpace(id: Long)

    fun onOpenSettings()

    fun onOpenSpaceSettings(id: Long)

    fun onAbout(key: AppKey)

    fun onRename(key: AppKey)

    fun onToggleHidden(key: AppKey, hidden: Boolean)

    fun onRemoveFromSpace(key: AppKey)

    fun onUninstall(key: AppKey)

    fun onDismiss()
}

@Composable
fun HomePopup(
    anchor: Offset,
    app: PopupApp?,
    spaces: ImmutableList<PopupSpace>,
    actions: PopupActions,
) {
    AnchoredPanel(anchor = anchor, onDismiss = actions::onDismiss) {
        if (app != null) {
            AppSection(app, actions)
            HorizontalDivider()
        }
        spaces.forEach { space ->
            PanelItem(
                text = space.name,
                radio = space.current,
                tag = "popup_env_${space.id}",
                onHold = { actions.onOpenSpaceSettings(space.id) },
                onClick = { actions.onSelectSpace(space.id) },
            )
        }
        HorizontalDivider()
        PanelItem(stringResource(R.string.popup_settings), tag = "popup_settings", onClick = actions::onOpenSettings)
    }
}

@Composable
private fun AppSection(app: PopupApp, actions: PopupActions) {
    PanelHeader(app.label)
    PanelItem(stringResource(R.string.popup_about), tag = "popup_about") { actions.onAbout(app.key) }
    PanelItem(stringResource(R.string.popup_rename), tag = "popup_rename") { actions.onRename(app.key) }
    PanelItem(
        stringResource(if (app.hidden) R.string.popup_show else R.string.popup_hide),
        tag = "popup_hide",
    ) { actions.onToggleHidden(app.key, !app.hidden) }
    if (app.inSpace) {
        PanelItem(stringResource(R.string.popup_remove_from_space), tag = "popup_remove") {
            actions.onRemoveFromSpace(app.key)
        }
    }
    if (app.canUninstall) {
        PanelItem(stringResource(R.string.popup_uninstall), tag = "popup_uninstall") { actions.onUninstall(app.key) }
    }
}

interface SuggestionActions {
    fun onReplace(suggestion: HomeSuggestion)

    fun onAdd(suggestion: HomeSuggestion)

    fun onDismissSuggestion(suggestion: HomeSuggestion)

    fun onClose()
}

@Composable
fun SuggestionPanel(anchor: Offset, suggestion: HomeSuggestion, actions: SuggestionActions) {
    AnchoredPanel(anchor = anchor, onDismiss = actions::onClose) {
        PanelHeader(stringResource(R.string.suggest_header, suggestion.addLabel))
        PanelItem(
            stringResource(R.string.suggest_replace, suggestion.replaceLabel),
            tag = "suggest_replace",
        ) { actions.onReplace(suggestion) }
        if (suggestion.canAdd) {
            PanelItem(stringResource(R.string.suggest_add), tag = "suggest_add") { actions.onAdd(suggestion) }
        }
        PanelItem(stringResource(R.string.suggest_dismiss), tag = "suggest_dismiss") {
            actions.onDismissSuggestion(suggestion)
        }
    }
}
