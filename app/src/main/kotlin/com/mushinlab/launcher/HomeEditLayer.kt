package com.mushinlab.launcher

import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mushinlab.launcher.feature.settings.EditItemSheet
import com.mushinlab.launcher.feature.settings.EditTipsSheet
import com.mushinlab.launcher.feature.settings.SettingsRoute
import com.mushinlab.launcher.feature.settings.WidgetLabel
import com.mushinlab.launcher.feature.settings.WidgetPickerSheet
import com.mushinlab.launcher.feature.settings.WidgetsSheet
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun HomeEditLayer(vm: LauncherViewModel) {
    val navigator = vm.navigator
    when (val overlay = navigator.overlay) {
        Overlay.Widgets -> {
            val home by vm.home.collectAsStateWithLifecycle()
            val panel = home.widgetPanels.top
            WidgetsSheet(
                panel.widgets.map { WidgetLabel(it.id, it.label, clock = it.isClock) }.toImmutableList(),
                panel.freeCells.isNotEmpty(),
                { vm.widgets.add() },
                vm.widgets.panels::remove,
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
    vm.widgets.choices?.let { WidgetPicker(vm, it) }
}

@Composable
private fun WidgetPicker(vm: LauncherViewModel, choices: List<WidgetChoice>) {
    val manager = LocalContext.current.packageManager
    BackHandler(onBack = vm.widgets::dismissChoices)
    WidgetPickerSheet(
        choices.mapIndexed { index, choice ->
            when (choice) {
                WidgetChoice.Clock -> WidgetLabel(index, "", app = stringResource(R.string.app_name), clock = true)

                is WidgetChoice.Hosted -> WidgetLabel(
                    index,
                    choice.info.loadLabel(manager),
                    app = appLabel(manager, choice),
                )
            }
        }.toImmutableList(),
        { index ->
            val choice = choices.getOrNull(index)
            if (choice != null) vm.widgets.choose(choice)
            if (choice == WidgetChoice.Clock) vm.navigator.openSettings(SettingsRoute.ClockSheet)
        },
        vm.widgets::dismissChoices,
    )
}

private fun appLabel(manager: PackageManager, choice: WidgetChoice.Hosted): String? = runCatching {
    manager.getApplicationLabel(manager.getApplicationInfo(choice.info.provider.packageName, 0)).toString()
}.getOrNull()
