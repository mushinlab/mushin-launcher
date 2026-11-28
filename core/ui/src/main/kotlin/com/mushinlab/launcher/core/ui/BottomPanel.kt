package com.mushinlab.launcher.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Full-screen layer with a sheet at the bottom of the screen: a [PopupStyle.scrim] that fades in and the sheet that
 * slides up, both within [Motion.TRANSITION_MS]. Any touch on the scrim calls [onDismiss]. The sheet has the palette
 * [MushinColors.surface], rounded top corners, a grab bar and scrolls when its content is taller than the screen.
 */
@Composable
fun BottomPanel(onDismiss: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LocalMushinColors.current
    val shown = remember { Animatable(0f) }
    LaunchedEffect(Unit) { shown.animateTo(1f, tween(Motion.TRANSITION_MS)) }
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = shown.value }
                .background(PopupStyle.scrim(colors))
                .pointerInput(onDismiss) {
                    awaitEachGesture {
                        awaitFirstDown().consume()
                        onDismiss()
                    }
                }
                .testTag("popup_scrim"),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .graphicsLayer { translationY = size.height * (1f - shown.value) }
                .background(colors.surface, RoundedCornerShape(topStart = SHEET_RADIUS, topEnd = SHEET_RADIUS))
                .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() } }
                .verticalScroll(rememberScrollState())
                .testTag("popup")
                .padding(horizontal = SHEET_PADDING),
        ) {
            Box(
                Modifier
                    .padding(top = GRAB_TOP, bottom = GRAB_BOTTOM)
                    .size(GRAB_WIDTH, GRAB_HEIGHT)
                    .background(colors.divider, CircleShape)
                    .align(Alignment.CenterHorizontally),
            )
            content()
            Spacer(Modifier.height(SHEET_BOTTOM))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

/**
 * A text row of a [BottomPanel] in [color] at [size]. [selected] non-null marks the row as selectable and, when true,
 * shows an accent dot at its end; [onHold] works as in [holdToFire].
 */
@Composable
fun SheetItem(
    text: String,
    tag: String,
    color: Color,
    size: TextUnit = 17.sp,
    weight: FontWeight = FontWeight.Normal,
    selected: Boolean? = null,
    onHold: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val colors = LocalMushinColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = SHEET_ITEM)
            .holdToFire(onHold)
            .pressable(onClick = onClick)
            .semantics { if (selected != null) this.selected = selected }
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SingleLineText(text, MushinType.style(size, color, weight), Modifier.weight(1f))
        if (selected == true) {
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(SELECTED_DOT).background(colors.accent, CircleShape))
        }
    }
}

/** Large light title at the top of a [BottomPanel]. */
@Composable
fun SheetTitle(text: String) {
    SingleLineText(
        text,
        MushinType.style(SHEET_TITLE, LocalMushinColors.current.textPrimary, FontWeight.Light),
        Modifier.padding(bottom = 8.dp),
    )
}

private val SHEET_RADIUS = 28.dp
private val SHEET_PADDING = 28.dp
private val SHEET_BOTTOM = 16.dp
private val SHEET_ITEM = 50.dp
private val SHEET_TITLE = 26.sp
private val SELECTED_DOT = 8.dp
private val GRAB_TOP = 12.dp
private val GRAB_BOTTOM = 16.dp
private val GRAB_WIDTH = 36.dp
private val GRAB_HEIGHT = 4.dp
