package com.mushinlab.launcher.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.ui.GlyphWrapper
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.PopupStyle
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
            PlusIcon()
            Spacer(Modifier.width(HomeDimens.iconGap))
        }
        SingleLineText(
            stringResource(if (iconMode == IconMode.NONE) R.string.home_add_apps_inline else R.string.home_add_apps),
            rowStyle.text,
            Modifier.weight(1f).testTag("home_add_label"),
        )
        if (iconMode == IconMode.RIGHT) {
            Spacer(Modifier.width(HomeDimens.iconGap))
            PlusIcon()
        }
    }
}

@Composable
private fun PlusIcon() {
    val colors = LocalMushinColors.current
    Canvas(
        Modifier
            .size(HomeDimens.iconSize)
            .background(colors.surface, CircleShape)
            .border(GlyphWrapper.outline, PopupStyle.outline(colors), CircleShape)
            .testTag("home_add_icon"),
    ) {
        val arm = size.minDimension * PLUS_ARM
        val stroke = size.minDimension * PLUS_STROKE
        drawLine(
            colors.iconTint,
            center.copy(x = center.x - arm),
            center.copy(x = center.x + arm),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            colors.iconTint,
            center.copy(y = center.y - arm),
            center.copy(y = center.y + arm),
            stroke,
            StrokeCap.Round,
        )
    }
}

private const val PLUS_ARM = 0.22f
private const val PLUS_STROKE = 0.08f

private const val HOME_ADD = "home_add"
