package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.pressable

@Composable
fun ConfirmDialog(
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    DialogFrame(onDismiss) {
        BasicText(message, style = MushinType.style(17.sp, LocalMushinColors.current.textPrimary))
        Spacer(Modifier.height(16.dp))
        DialogButtons(
            listOf(
                stringResource(R.string.settings_cancel) to onDismiss,
                confirm to onConfirm,
            ),
        )
    }
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    maxLength: Int,
    error: String?,
    confirm: String,
    extra: Pair<String, () -> Unit>? = null,
    options: (@Composable ColumnScope.() -> Unit)? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalMushinColors.current
    var text by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    DialogFrame(onDismiss) {
        BasicText(title, style = MushinType.style(17.sp, colors.textPrimary, FontWeight.Medium))
        Spacer(Modifier.height(12.dp))
        BasicTextField(
            value = text,
            onValueChange = { if (it.length <= maxLength) text = it },
            singleLine = true,
            textStyle = MushinType.style(18.sp, colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.divider, RoundedCornerShape(6.dp))
                .padding(12.dp)
                .focusRequester(focus)
                .testTag("text_input"),
        )
        options?.invoke(this)
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            BasicText(error, style = MushinType.style(14.sp, colors.accent))
        }
        Spacer(Modifier.height(16.dp))
        DialogButtons(
            listOfNotNull(
                extra,
                stringResource(R.string.settings_cancel) to onDismiss,
                confirm to { onConfirm(text) },
            ),
        )
    }
}

@Composable
private fun DialogFrame(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalMushinColors.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM_ALPHA))
            .pressable(pressedColor = Color.Transparent, onClick = onDismiss)
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
                .pressable(pressedColor = Color.Transparent) {}
                .padding(20.dp)
                .testTag("dialog"),
            content = content,
        )
    }
}

@Composable
private fun DialogButtons(buttons: List<Pair<String, () -> Unit>>) {
    val colors = LocalMushinColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        buttons.forEachIndexed { index, (label, action) ->
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .pressable(onClick = action)
                    .testTag("dialog_button_$index")
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(label, style = MushinType.style(16.sp, colors.accent, FontWeight.Medium))
            }
        }
    }
}

private const val SCRIM_ALPHA = 0.5f
