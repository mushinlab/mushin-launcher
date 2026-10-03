package com.bluecubelabs.env.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.ui.AnchoredPanel
import com.bluecubelabs.env.core.ui.EnvType
import com.bluecubelabs.env.core.ui.HorizontalDivider
import com.bluecubelabs.env.core.ui.LocalEnvColors
import com.bluecubelabs.env.core.ui.SingleLineText
import com.bluecubelabs.env.core.ui.pressable
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class PopupEnv(val id: Long, val name: String, val current: Boolean)

@Immutable
data class PopupApp(
    val key: AppKey,
    val label: String,
    val hidden: Boolean,
    val inEnv: Boolean,
    val canUninstall: Boolean,
)

interface PopupActions {
    fun onSelectEnv(id: Long)

    fun onOpenSettings()

    fun onAbout(key: AppKey)

    fun onRename(key: AppKey)

    fun onToggleHidden(key: AppKey, hidden: Boolean)

    fun onRemoveFromEnv(key: AppKey)

    fun onUninstall(key: AppKey)

    fun onDismiss()
}

@Composable
fun HomePopup(
    anchor: Offset,
    app: PopupApp?,
    envs: ImmutableList<PopupEnv>,
    actions: PopupActions,
) {
    AnchoredPanel(anchor = anchor, onDismiss = actions::onDismiss) {
        if (app != null) {
            AppSection(app, actions)
            HorizontalDivider()
        }
        envs.forEach { env ->
            PopupItem(
                text = if (env.current) "✓  ${env.name}" else env.name,
                bold = env.current,
                tag = "popup_env_${env.id}",
                onClick = { actions.onSelectEnv(env.id) },
            )
        }
        HorizontalDivider()
        PopupItem(stringResource(R.string.popup_settings), tag = "popup_settings", onClick = actions::onOpenSettings)
    }
}

@Composable
private fun AppSection(app: PopupApp, actions: PopupActions) {
    val colors = LocalEnvColors.current
    Box(Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
        SingleLineText(app.label, EnvType.style(EnvType.popupHeaderSize, colors.textSecondary))
    }
    PopupItem(stringResource(R.string.popup_about), tag = "popup_about") { actions.onAbout(app.key) }
    PopupItem(stringResource(R.string.popup_rename), tag = "popup_rename") { actions.onRename(app.key) }
    PopupItem(
        stringResource(if (app.hidden) R.string.popup_show else R.string.popup_hide),
        tag = "popup_hide",
    ) { actions.onToggleHidden(app.key, !app.hidden) }
    if (app.inEnv) {
        PopupItem(stringResource(R.string.popup_remove_from_env), tag = "popup_remove") {
            actions.onRemoveFromEnv(app.key)
        }
    }
    if (app.canUninstall) {
        PopupItem(stringResource(R.string.popup_uninstall), tag = "popup_uninstall") { actions.onUninstall(app.key) }
    }
}

@Composable
private fun PopupItem(text: String, tag: String, bold: Boolean = false, onClick: () -> Unit) {
    val colors = LocalEnvColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .pressable(onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        SingleLineText(
            text,
            EnvType.style(EnvType.popupSize, colors.textPrimary, if (bold) FontWeight.Bold else FontWeight.Normal),
        )
    }
}
