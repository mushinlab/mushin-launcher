package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.feature.settings.NavRow
import com.mushinlab.launcher.feature.settings.R
import com.mushinlab.launcher.feature.settings.SettingsPage
import com.mushinlab.launcher.feature.settings.SettingsRoute

@Composable
internal fun TipsScreen(push: (SettingsRoute) -> Unit, back: () -> Unit) {
    SettingsPage(stringResource(R.string.settings_tips), back) {
        Tip.entries.forEach { tip ->
            NavRow(
                stringResource(tip.title),
                summary = stringResource(tip.description),
                tag = "tip_${tip.id}",
            ) { push(SettingsRoute.Tip(tip.id)) }
        }
    }
}

@Composable
internal fun TipScreen(tip: Tip, back: () -> Unit) {
    SettingsPage(stringResource(tip.title), back, scrollable = false) {
        TipAnimation(
            tip,
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .testTag("tip_player_${tip.id}"),
        )
        BasicText(
            stringResource(tip.description),
            style = MushinType.style(16.sp, LocalMushinColors.current.textSecondary),
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
        )
    }
}
