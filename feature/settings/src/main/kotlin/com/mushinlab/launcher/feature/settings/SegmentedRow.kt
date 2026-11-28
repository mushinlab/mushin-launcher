package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable

@Composable
fun <T> SegmentedRow(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    tagPrefix: String,
    onSelect: (T) -> Unit,
) {
    val colors = LocalMushinColors.current
    Column(
        Modifier
            .settingsCard()
            .testTag("${tagPrefix}_segments")
            .padding(horizontal = CARD_PADDING, vertical = 8.dp),
    ) {
        BasicText(title, style = MushinType.style(17.sp, colors.textPrimary))
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(SEGMENT_RADIUS))
                .background(colors.keyBackground)
                .padding(SEGMENT_INSET),
        ) {
            options.forEach { (value, label) ->
                val on = value == selected
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(SEGMENT_RADIUS - SEGMENT_INSET))
                        .background(if (on) colors.keyPressed else Color.Transparent)
                        .pressable(role = Role.RadioButton) { onSelect(value) }
                        .semantics { this.selected = on }
                        .testTag("${tagPrefix}_$value")
                        .heightIn(min = SEGMENT_HEIGHT)
                        .padding(horizontal = SEGMENT_PADDING),
                    contentAlignment = Alignment.Center,
                ) {
                    SingleLineText(
                        label,
                        MushinType.style(14.sp, if (on) colors.textPrimary else colors.textSecondary),
                    )
                }
            }
        }
    }
}

private val SEGMENT_RADIUS = 10.dp
private val SEGMENT_INSET = 3.dp
private val SEGMENT_HEIGHT = 34.dp
private val SEGMENT_PADDING = 6.dp
