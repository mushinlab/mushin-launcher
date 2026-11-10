package com.mushinlab.launcher.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.ui.AnchoredPanel
import com.mushinlab.launcher.core.ui.AppIcon
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.HorizontalDivider
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.PanelHeader
import com.mushinlab.launcher.core.ui.PanelItem
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class PopupSpace(val id: Long, val name: String, val current: Boolean)

@Immutable
data class PopupApp(
    val key: AppKey,
    val label: String,
    val canUninstall: Boolean,
)

interface PopupActions {
    fun onSelectSpace(id: Long)

    fun onOpenSpaceSettings(id: Long)

    fun onAbout(key: AppKey)

    fun onRename(key: AppKey)

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
    }
}

@Composable
fun FolderPanel(anchor: Offset, apps: ImmutableList<HomeRow>, onLaunch: (AppKey) -> Unit, onDismiss: () -> Unit) {
    AnchoredPanel(anchor = anchor, onDismiss = onDismiss, anchorDepth = FOLDER_ITEM / 2) {
        apps.forEachIndexed { index, app ->
            FolderItem(app, index) { onLaunch(app.key) }
        }
    }
}

@Composable
private fun FolderItem(app: HomeRow, index: Int, onClick: () -> Unit) {
    val colors = LocalMushinColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(FOLDER_ITEM)
            .pressable(onClick = onClick)
            .testTag("folder_item_$index")
            .padding(horizontal = 16.dp)
            .alpha(if (app.dimmed) DIMMED_ITEM else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.key, app.label, FOLDER_ICON)
        Spacer(Modifier.width(12.dp))
        SingleLineText(app.label, MushinType.style(MushinType.popupSize, colors.textPrimary))
    }
}

private val FOLDER_ITEM = 48.dp
private val FOLDER_ICON = 24.dp
private const val DIMMED_ITEM = 0.5f

@Composable
private fun AppSection(app: PopupApp, actions: PopupActions) {
    val tint = LocalMushinColors.current.textPrimary
    PanelHeader(app.label)
    Row(
        Modifier.padding(start = APP_ACTIONS_INSET, end = APP_ACTIONS_INSET, bottom = APP_ACTIONS_INSET),
        horizontalArrangement = Arrangement.spacedBy(APP_ACTIONS_GAP),
    ) {
        GlyphButton(Glyph.INFO, stringResource(R.string.popup_about), tint, tag = "popup_about") {
            actions.onAbout(app.key)
        }
        GlyphButton(Glyph.EDIT, stringResource(R.string.popup_rename), tint, tag = "popup_rename") {
            actions.onRename(app.key)
        }
        if (app.canUninstall) {
            GlyphButton(Glyph.DELETE, stringResource(R.string.popup_uninstall), tint, tag = "popup_uninstall") {
                actions.onUninstall(app.key)
            }
        }
    }
}

private val APP_ACTIONS_INSET = 12.dp
private val APP_ACTIONS_GAP = 8.dp

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
