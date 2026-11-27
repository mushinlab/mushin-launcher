package com.mushinlab.launcher

import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mushinlab.launcher.feature.settings.EditTipsSheet
import com.mushinlab.launcher.feature.settings.SettingsRoute
import com.mushinlab.launcher.feature.settings.WidgetLabel
import com.mushinlab.launcher.feature.settings.WidgetPickerSheet
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun HomeEditLayer(vm: LauncherViewModel) {
    val navigator = vm.navigator
    if (navigator.overlay == Overlay.EditTips) EditTipsSheet { navigator.overlay = Overlay.None }
    vm.widgets.choices?.let { WidgetPicker(vm, it) }
}

@Composable
private fun WidgetPicker(vm: LauncherViewModel, choices: List<WidgetChoice>) {
    val context = LocalContext.current
    val manager = context.packageManager
    val art = remember(choices) { WidgetArtLoader(context, choices) }
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
        art,
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
