package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.mushinlab.launcher.core.ui.EnvType
import com.mushinlab.launcher.core.ui.LocalEnvColors
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable

@Composable
fun <T> DropdownRow(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    tagPrefix: String,
    style: ((T) -> TextStyle)? = null,
    leading: (@Composable (T) -> Unit)? = null,
    onSelect: (T) -> Unit,
) {
    val colors = LocalEnvColors.current
    var expanded by remember { mutableStateOf(false) }
    var rowWidth by remember { mutableIntStateOf(0) }
    val current = options.firstOrNull { it.first == selected }
    Box(Modifier.settingsCard().onSizeChanged { rowWidth = it.width }) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .pressable(role = Role.DropdownList) { expanded = true }
                .testTag("${tagPrefix}_dropdown")
                .padding(horizontal = CARD_PADDING, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                BasicText(title, style = EnvType.style(17.sp, colors.textPrimary))
                if (current != null) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        leading?.invoke(current.first)
                        if (leading != null) Spacer(Modifier.width(8.dp))
                        SingleLineText(current.second, EnvType.style(14.sp, colors.textSecondary))
                    }
                }
            }
            BasicText("▾", style = EnvType.style(17.sp, colors.textSecondary))
        }
        if (expanded) {
            Popup(
                popupPositionProvider = remember { BelowOrAbove() },
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                DropdownMenu(options, selected, tagPrefix, rowWidth, style, leading) {
                    expanded = false
                    onSelect(it)
                }
            }
        }
    }
}

@Composable
private fun <T> DropdownMenu(
    options: List<Pair<T, String>>,
    selected: T,
    tagPrefix: String,
    widthPx: Int,
    style: ((T) -> TextStyle)?,
    leading: (@Composable (T) -> Unit)?,
    onPick: (T) -> Unit,
) {
    val colors = LocalEnvColors.current
    val width = with(LocalDensity.current) { widthPx.toDp() }
    Column(
        Modifier
            .width(width)
            .heightIn(max = MENU_MAX_HEIGHT)
            .clip(RoundedCornerShape(CARD_RADIUS))
            .background(colors.surface)
            .border(1.dp, colors.divider, RoundedCornerShape(CARD_RADIUS))
            .verticalScroll(rememberScrollState())
            .testTag("${tagPrefix}_menu"),
    ) {
        options.forEach { (value, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectedBackground(value == selected)
                    .pressable(role = Role.RadioButton) { onPick(value) }
                    .testTag("${tagPrefix}_$value")
                    .padding(horizontal = CARD_PADDING),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leading?.invoke(value)
                if (leading != null) Spacer(Modifier.width(12.dp))
                SingleLineText(label, style?.invoke(value) ?: EnvType.style(16.sp, colors.textPrimary))
            }
        }
    }
}

private class BelowOrAbove : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val below = anchorBounds.bottom
        val y = if (below + popupContentSize.height <= windowSize.height) {
            below
        } else {
            (anchorBounds.top - popupContentSize.height).coerceAtLeast(0)
        }
        return IntOffset(anchorBounds.left, y)
    }
}

private val MENU_MAX_HEIGHT = 360.dp
