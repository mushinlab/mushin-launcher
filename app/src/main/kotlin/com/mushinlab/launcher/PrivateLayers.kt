package com.mushinlab.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable
import com.mushinlab.launcher.feature.settings.tips.PrivateIntro

@Composable
internal fun PrivateGateLayer(vm: LauncherViewModel) {
    val gate by vm.privateSpace.gate.collectAsStateWithLifecycle()
    val current = gate ?: return
    val colors = LocalMushinColors.current
    Box(
        Modifier.zIndex(7f).fillMaxSize().background(colors.background.copy(alpha = SCRIM)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(CARD_MARGIN)
                .fillMaxWidth()
                .background(colors.surface, CARD_SHAPE)
                .padding(CARD_PADDING)
                .testTag("private_pin"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val title = when (current.mode) {
                PrivateGate.Mode.SET, PrivateGate.Mode.UNLOCK -> R.string.private_enter
                PrivateGate.Mode.CONFIRM -> R.string.private_repeat
            }
            SingleLineText(stringResource(title), MushinType.style(MushinType.resultSize, colors.textPrimary))
            Spacer(Modifier.height(16.dp))
            PinDots(current)
            if (current.error) {
                Spacer(Modifier.height(8.dp))
                SingleLineText(
                    stringResource(R.string.private_wrong),
                    MushinType.style(MushinType.secondarySize, colors.danger),
                    Modifier.testTag("private_pin_error"),
                )
            }
            Spacer(Modifier.height(20.dp))
            PinKeypad(onDigit = vm.privateSpace::digit, onBackspace = vm.privateSpace::backspace)
            Spacer(Modifier.height(8.dp))
            SingleLineText(
                stringResource(R.string.private_cancel),
                MushinType.style(MushinType.secondarySize, colors.textSecondary),
                Modifier.pressable(onClick = vm.privateSpace::cancel).padding(12.dp).testTag("private_pin_cancel"),
            )
        }
    }
}

@Composable
private fun PinDots(gate: PrivateGate) {
    val colors = LocalMushinColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(DOT_GAP)) {
        repeat(PrivateGate.LENGTH) { index ->
            val filled = index < gate.digits.size
            Box(
                Modifier
                    .size(DOT)
                    .background(if (filled) colors.accent else Color.Transparent, CircleShape)
                    .border(OFFLINE_WIDTH, colors.textSecondary, CircleShape)
                    .testTag("private_pin_dot_$index"),
            )
        }
    }
}

@Composable
private fun PinKeypad(onDigit: (Int) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9), listOf(-1, 0, -2))
    Column(verticalArrangement = Arrangement.spacedBy(KEY_GAP)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
                row.forEach { digit ->
                    when (digit) {
                        -1 -> Spacer(Modifier.size(KEY_SIZE))
                        -2 -> PinKey("⌫", "private_pin_backspace", onBackspace)
                        else -> PinKey(digit.toString(), "private_pin_$digit") { onDigit(digit) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKey(text: String, tag: String, onClick: () -> Unit) {
    val colors = LocalMushinColors.current
    Box(
        Modifier
            .size(KEY_SIZE)
            .background(colors.keyBackground, CircleShape)
            .pressable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        SingleLineText(text, MushinType.style(KEY_TEXT, colors.textPrimary))
    }
}

@Composable
internal fun PrivateHintLayer(vm: LauncherViewModel) {
    val onboarding by vm.onboardingDone.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    if (onboarding != true || settings.privateHintSeen) return
    PrivateIntro(
        onDone = { vm.preferences.update { it.copy(privateHintSeen = true) } },
        modifier = Modifier.zIndex(7f),
    )
}

private val CARD_MARGIN = 24.dp
private val CARD_PADDING = 24.dp
private val CARD_SHAPE = RoundedCornerShape(20.dp)
private val DOT = 14.dp
private val DOT_GAP = 12.dp
private val KEY_SIZE = 56.dp
private val KEY_GAP = 8.dp
private val KEY_TEXT = MushinType.keySize
private val OFFLINE_WIDTH = 1.dp
private const val SCRIM = 0.9f
