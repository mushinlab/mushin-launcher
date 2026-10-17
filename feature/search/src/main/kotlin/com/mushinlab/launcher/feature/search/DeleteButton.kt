package com.mushinlab.launcher.feature.search

import android.view.HapticFeedbackConstants
import android.view.ViewConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.pressFeedback
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun DeleteButton(drawn: Boolean, haptics: Boolean, actions: KeyboardActions, modifier: Modifier = Modifier) {
    val colors = LocalMushinColors.current
    val view = LocalView.current
    val currentActions by rememberUpdatedState(actions)
    val currentHaptics by rememberUpdatedState(haptics)
    var pressed by remember { mutableStateOf(false) }
    val description = stringResource(if (drawn) R.string.search_clear_drawing else R.string.search_backspace)
    Box(
        modifier
            .size(SearchDimens.deleteTouchWidth, SearchDimens.queryHeight)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = description
                onClick { currentActions.onBackspace() }
            }
            .testTag("key_BACKSPACE")
            .pointerInput(Unit) {
                handleDelete(
                    actions = { currentActions },
                    haptic = { if (currentHaptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) },
                    onPressed = { pressed = it },
                )
            },
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            Modifier
                .size(SearchDimens.deleteWidth, SearchDimens.deleteHeight)
                .clip(RoundedCornerShape(KeyboardDimens.radius))
                .background(colors.keyBackground)
                .pressFeedback(pressed),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                if (drawn) "×" else "⌫",
                style = MushinType.style(
                    if (drawn) KeyboardDimens.crossSize else MushinType.keySize,
                    colors.textPrimary,
                ),
            )
        }
    }
}

private suspend fun PointerInputScope.handleDelete(
    actions: () -> KeyboardActions,
    haptic: () -> Unit,
    onPressed: (Boolean) -> Unit,
) = coroutineScope {
    val clearSwipe = KeyboardDimens.clearSwipe.toPx()
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        onPressed(true)
        haptic()
        var repeated = false
        val repeat = launch {
            delay(ViewConfiguration.getKeyRepeatTimeout().toLong())
            repeated = true
            while (true) {
                if (actions().onBackspace()) haptic()
                delay(ViewConfiguration.getKeyRepeatDelay().toLong())
            }
        }
        var clearing = false
        var released = false
        try {
            while (!released) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                change.consume()
                released = !change.pressed
                if (!clearing && change.position.x - down.position.x <= -clearSwipe) {
                    clearing = true
                    repeat.cancel()
                }
            }
        } finally {
            repeat.cancel()
            onPressed(false)
        }
        if (released) {
            when {
                clearing -> actions().onClear()
                !repeated -> actions().onBackspace()
            }
        }
    }
}
