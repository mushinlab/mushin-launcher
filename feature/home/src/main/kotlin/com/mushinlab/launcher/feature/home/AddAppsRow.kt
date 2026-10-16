package com.mushinlab.launcher.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable
import com.mushinlab.launcher.feature.home.gesture.RowRegistry

@Composable
internal fun AddAppsRow(
    iconMode: IconMode,
    rowStyle: RowStyle,
    background: Color,
    registry: RowRegistry,
    onClick: () -> Unit,
) {
    DisposableEffect(Unit) { onDispose { registry.block(HOME_ADD, null) } }
    Row(
        Modifier
            .fillMaxWidth()
            .height(rowStyle.height)
            .background(background)
            .onGloballyPositioned { registry.block(HOME_ADD, it.boundsInRoot()) }
            .pressable(onClick = onClick)
            .testTag(HOME_ADD)
            .padding(horizontal = HomeDimens.rowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconMode == IconMode.LEFT) {
            Spacer(Modifier.width(HomeDimens.iconSize + HomeDimens.iconGap))
        }
        SingleLineText(
            stringResource(R.string.home_add_apps),
            rowStyle.text,
            Modifier.weight(1f).testTag("home_add_label"),
        )
        if (iconMode == IconMode.RIGHT) {
            Spacer(Modifier.width(HomeDimens.iconGap + HomeDimens.iconSize))
        }
    }
}

private const val HOME_ADD = "home_add"
