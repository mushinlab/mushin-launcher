package com.mushinlab.launcher.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText

@Composable
fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalMushinColors.current
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding()
            .testTag("settings"),
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(
            Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlyphButton(
                Glyph.BACK,
                stringResource(R.string.settings_back),
                colors.textPrimary,
                tag = "settings_back",
                modifier = Modifier.padding(horizontal = 12.dp).clip(CircleShape).background(colors.surface),
                size = 40.dp,
                onClick = onBack,
            )
            Column {
                SingleLineText(title, MushinType.style(22.sp, colors.textPrimary))
                LocalSpaceName.current?.let { space ->
                    SingleLineText(
                        space,
                        MushinType.style(12.sp, colors.textSecondary),
                        Modifier.testTag("settings_space"),
                    )
                }
            }
        }
        val body = if (scrollable) Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier.weight(1f)
        Column(body.padding(vertical = 4.dp)) { content() }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

val LocalSpaceName = staticCompositionLocalOf<String?> { null }

@Composable
fun SectionHeader(text: String) {
    val colors = LocalMushinColors.current
    BasicText(
        text,
        style = MushinType.style(14.sp, colors.accent, FontWeight.Medium),
        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
internal fun Modifier.settingsCard(): Modifier = this
    .fillMaxWidth()
    .padding(horizontal = CARD_MARGIN, vertical = CARD_GAP / 2)
    .clip(RoundedCornerShape(CARD_RADIUS))
    .background(LocalMushinColors.current.surface)

@Composable
fun CardGap() {
    Spacer(Modifier.height(12.dp))
}

@Composable
internal fun Modifier.selectedBackground(selected: Boolean): Modifier {
    val accent = LocalMushinColors.current.accent
    val color by animateColorAsState(
        if (selected) accent.copy(alpha = SELECTED_ALPHA) else accent.copy(alpha = 0f),
        tween(Motion.TRANSITION_MS),
        label = "selected",
    )
    return background(color).semantics { this.selected = selected }
}

@Composable
fun NoteText(text: String, color: Color = LocalMushinColors.current.textSecondary) {
    BasicText(
        text,
        style = MushinType.style(14.sp, color),
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
    )
}

internal const val DISABLED_ALPHA = 0.4f
internal val CARD_RADIUS = 6.dp
internal val CARD_MARGIN = 16.dp
internal val CARD_GAP = 4.dp
internal val CARD_PADDING = 16.dp
private const val SELECTED_ALPHA = 0.24f
