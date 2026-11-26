package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.pressable
import com.mushinlab.launcher.feature.settings.R

@Composable
fun PrivateIntro(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalMushinColors.current
    var checked by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize().background(colors.background).testTag("private_hint")) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PrivateTipAnimation(Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp))
            BasicText(
                stringResource(R.string.private_hint_title),
                style = MushinType.style(
                    24.sp,
                    colors.textPrimary,
                    FontWeight.Medium,
                ).copy(textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(8.dp))
            BasicText(
                stringResource(R.string.private_hint_text),
                style = MushinType.style(16.sp, colors.textSecondary).copy(textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(20.dp))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            IntroCheck(checked) { checked = !checked }
            Spacer(Modifier.height(12.dp))
            IntroDone(checked, onDone)
            Spacer(Modifier.height(16.dp))
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun IntroCheck(checked: Boolean, onToggle: () -> Unit) {
    val colors = LocalMushinColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(BUTTON_SHAPE)
            .pressable(role = Role.Checkbox, onClick = onToggle)
            .semantics(mergeDescendants = true) { toggleableState = ToggleableState(checked) }
            .testTag("private_hint_check")
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(CHECK_SIZE)
                .background(if (checked) colors.accent else Color.Transparent, CHECK_SHAPE)
                .border(1.dp, if (checked) colors.accent else colors.textSecondary, CHECK_SHAPE),
        )
        Spacer(Modifier.width(12.dp))
        BasicText(
            stringResource(R.string.private_hint_check),
            style = MushinType.style(16.sp, colors.textPrimary),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun IntroDone(enabled: Boolean, onDone: () -> Unit) {
    val colors = LocalMushinColors.current
    val background = if (enabled) colors.accent else colors.keyBackground
    val content = if (enabled) colors.background else colors.textSecondary
    Box(
        Modifier
            .fillMaxWidth()
            .height(BUTTON_HEIGHT)
            .clip(BUTTON_SHAPE)
            .background(background)
            .pressable(enabled = enabled, onClick = onDone)
            .testTag("private_hint_done"),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            stringResource(R.string.private_hint_done),
            style = MushinType.style(16.sp, content, FontWeight.Medium),
        )
    }
}

private val CHECK_SIZE = 24.dp
private val CHECK_SHAPE = RoundedCornerShape(6.dp)
private val BUTTON_HEIGHT = 52.dp
private val BUTTON_SHAPE = RoundedCornerShape(26.dp)
