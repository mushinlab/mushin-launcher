package com.mushinlab.launcher.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.mushinlab.launcher.core.ui.EnvType
import com.mushinlab.launcher.core.ui.LocalEnvColors

@Composable
fun CountBadge(count: Int, diameter: Dp, modifier: Modifier = Modifier) {
    val colors = LocalEnvColors.current
    val textSize = with(LocalDensity.current) { (diameter * TEXT_TO_DIAMETER).toSp() }
    Box(
        modifier
            .height(diameter)
            .widthIn(min = diameter)
            .background(colors.accent, CircleShape)
            .padding(horizontal = diameter * PADDING_TO_DIAMETER),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            if (count > MAX_BADGE) "$MAX_BADGE+" else count.toString(),
            style = EnvType.style(textSize, colors.background, FontWeight.Medium),
        )
    }
}

private const val MAX_BADGE = 99
private const val TEXT_TO_DIAMETER = 0.62f
private const val PADDING_TO_DIAMETER = 0.18f
