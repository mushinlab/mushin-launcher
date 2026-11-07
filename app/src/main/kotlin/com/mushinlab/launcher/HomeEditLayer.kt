package com.mushinlab.launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mushinlab.launcher.core.model.WidgetPanelPosition
import com.mushinlab.launcher.feature.settings.EditItemSheet
import com.mushinlab.launcher.feature.settings.EditTipsSheet
import com.mushinlab.launcher.feature.settings.SettingsRoute
import com.mushinlab.launcher.feature.settings.WidgetLabel
import com.mushinlab.launcher.feature.settings.WidgetPanelUi
import com.mushinlab.launcher.feature.settings.WidgetsSheet
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun HomeEditLayer(vm: LauncherViewModel) {
    val navigator = vm.navigator
    when (val overlay = navigator.overlay) {
        Overlay.Widgets -> {
            val home by vm.home.collectAsStateWithLifecycle()
            val panels = listOf(
                WidgetPanelPosition.TOP to home.widgetPanels.top,
                WidgetPanelPosition.BOTTOM to home.widgetPanels.bottom,
            )
                .map { (position, panel) ->
                    WidgetPanelUi(
                        position,
                        panel.size,
                        panel.clock && home.showClock,
                        panel.widgets.map { WidgetLabel(it.id, it.label) }.toImmutableList(),
                    )
                }.toImmutableList()
            WidgetsSheet(
                panels,
                vm.widgets::resize,
                vm.widgets::add,
                vm.widgets::remove,
                { navigator.openSettings(SettingsRoute.ClockSheet) },
                { navigator.overlay = Overlay.None },
            )
        }

        Overlay.EditTips -> EditTipsSheet { navigator.overlay = Overlay.None }

        is Overlay.EditItem -> EditItemSheet(
            label = overlay.row.label,
            onClose = { navigator.overlay = Overlay.None },
            onRemove = {
                val folder = overlay.row.folderId
                if (folder != null) vm.folders.delete(folder) else vm.model.removeFromCurrentSpace(overlay.row.key)
                navigator.overlay = Overlay.None
            },
            onReplace = {
                val space = vm.model.currentSpace
                val folder = overlay.row.folderId
                if (space != null) {
                    navigator.openSettings(
                        if (folder != null) {
                            SettingsRoute.HomeFolder(folder)
                        } else {
                            SettingsRoute.ReplaceApp(space.id, overlay.row.key)
                        },
                    )
                }
            },
        )

        else -> Unit
    }
}
