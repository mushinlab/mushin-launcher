package com.bluecubelabs.env.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluecubelabs.env.core.ui.EnvType
import com.bluecubelabs.env.core.ui.Glyph
import com.bluecubelabs.env.core.ui.GlyphButton
import com.bluecubelabs.env.core.ui.LocalEnvColors
import com.bluecubelabs.env.core.ui.Motion
import com.bluecubelabs.env.core.ui.SingleLineText
import com.bluecubelabs.env.core.ui.pressable
import kotlin.math.roundToInt

@Composable
fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalEnvColors.current
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
            SingleLineText(title, EnvType.style(22.sp, colors.textPrimary))
        }
        val body = if (scrollable) Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier.weight(1f)
        Column(body.padding(vertical = 4.dp)) { content() }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
fun SectionHeader(text: String) {
    val colors = LocalEnvColors.current
    BasicText(
        text,
        style = EnvType.style(14.sp, colors.accent, FontWeight.Medium),
        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
internal fun Modifier.settingsCard(): Modifier = this
    .fillMaxWidth()
    .padding(horizontal = CARD_MARGIN, vertical = CARD_GAP / 2)
    .clip(RoundedCornerShape(CARD_RADIUS))
    .background(LocalEnvColors.current.surface)

@Composable
fun CardGap() {
    Spacer(Modifier.height(12.dp))
}

@Composable
internal fun Modifier.selectedBackground(selected: Boolean): Modifier {
    val accent = LocalEnvColors.current.accent
    val color by animateColorAsState(
        if (selected) accent.copy(alpha = SELECTED_ALPHA) else accent.copy(alpha = 0f),
        tween(Motion.TRANSITION_MS),
        label = "selected",
    )
    return background(color).semantics { this.selected = selected }
}

@Composable
fun NavRow(
    title: String,
    value: String? = null,
    summary: String? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    tag: String? = null,
    onClick: () -> Unit,
) {
    val colors = LocalEnvColors.current
    Row(
        Modifier
            .settingsCard()
            .heightIn(min = 56.dp)
            .selectedBackground(selected)
            .pressable(enabled = enabled, onClick = onClick)
            .let { if (tag != null) it.testTag(tag) else it }
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = CARD_PADDING, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f, fill = false)) {
            BasicText(title, style = EnvType.style(17.sp, colors.textPrimary))
            if (summary != null) {
                Spacer(Modifier.height(2.dp))
                BasicText(summary, style = EnvType.style(14.sp, colors.textSecondary))
            }
        }
        if (value != null) {
            Spacer(Modifier.width(16.dp))
            SingleLineText(value, EnvType.style(15.sp, colors.textSecondary))
        }
    }
}

@Composable
fun ToggleRow(title: String, checked: Boolean, tag: String? = null, onToggle: (Boolean) -> Unit) {
    val colors = LocalEnvColors.current
    Row(
        Modifier
            .settingsCard()
            .heightIn(min = 56.dp)
            .pressable(role = Role.Switch) { onToggle(!checked) }
            .let { if (tag != null) it.testTag(tag) else it }
            .padding(horizontal = CARD_PADDING, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(title, style = EnvType.style(17.sp, colors.textPrimary), modifier = Modifier.weight(1f))
        Switch(checked)
    }
}

@Composable
private fun Switch(checked: Boolean) {
    val colors = LocalEnvColors.current
    Box(
        Modifier
            .size(width = 40.dp, height = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) colors.accent else colors.keyBackground)
            .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (checked) colors.background else colors.textSecondary),
        )
    }
}

@Composable
fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    tag: String,
    onChange: (Float) -> Unit,
) {
    val colors = LocalEnvColors.current
    val locale = LocalConfiguration.current.locales[0]
    val steps = ((range.endInclusive - range.start) / step).roundToInt()
    var shown by remember(value) { mutableFloatStateOf(value) }
    val currentOnChange by rememberUpdatedState(onChange)
    fun select(fraction: Float) {
        val next = range.start + (fraction.coerceIn(0f, 1f) * steps).roundToInt() * step
        if (next != shown) {
            shown = next
            currentOnChange(next)
        }
    }
    Column(Modifier.settingsCard().padding(horizontal = CARD_PADDING, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(title, style = EnvType.style(17.sp, colors.textPrimary), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(16.dp))
            SingleLineText(
                String.format(locale, "%.2f", shown),
                EnvType.style(15.sp, colors.textSecondary),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .testTag(tag)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(shown, range, steps - 1)
                    setProgress { target ->
                        select((target - range.start) / (range.endInclusive - range.start))
                        true
                    }
                }
                .pointerInput(range, step) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        select(down.position.x / size.width)
                        horizontalDrag(down.id) { change ->
                            change.consume()
                            select(change.position.x / size.width)
                        }
                    }
                }
                .drawBehind {
                    val fraction = (shown - range.start) / (range.endInclusive - range.start)
                    val track = SLIDER_TRACK.toPx()
                    val y = size.height / 2
                    drawRoundRect(
                        colors.textSecondary.copy(alpha = SLIDER_REST_ALPHA),
                        topLeft = Offset(0f, y - track / 2),
                        size = Size(size.width, track),
                        cornerRadius = CornerRadius(track / 2),
                    )
                    drawRoundRect(
                        colors.accent,
                        topLeft = Offset(0f, y - track / 2),
                        size = Size(size.width * fraction, track),
                        cornerRadius = CornerRadius(track / 2),
                    )
                    drawCircle(colors.accent, radius = SLIDER_THUMB.toPx(), center = Offset(size.width * fraction, y))
                },
        )
    }
}

@Composable
fun <T> ChoiceRows(options: List<Pair<T, String>>, selected: T, tagPrefix: String, onSelect: (T) -> Unit) {
    options.forEach { (value, label) ->
        NavRow(
            title = label,
            selected = value == selected,
            tag = "${tagPrefix}_$value",
            onClick = { onSelect(value) },
        )
    }
}

@Composable
fun NoteText(text: String, color: Color = LocalEnvColors.current.textSecondary) {
    BasicText(
        text,
        style = EnvType.style(14.sp, color),
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
    )
}

@Composable
fun ConfirmDialog(
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    DialogFrame(onDismiss) {
        BasicText(message, style = EnvType.style(17.sp, LocalEnvColors.current.textPrimary))
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
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalEnvColors.current
    var text by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    DialogFrame(onDismiss) {
        BasicText(title, style = EnvType.style(17.sp, colors.textPrimary, FontWeight.Medium))
        Spacer(Modifier.height(12.dp))
        BasicTextField(
            value = text,
            onValueChange = { if (it.length <= maxLength) text = it },
            singleLine = true,
            textStyle = EnvType.style(18.sp, colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.divider, RoundedCornerShape(6.dp))
                .padding(12.dp)
                .focusRequester(focus)
                .testTag("text_input"),
        )
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            BasicText(error, style = EnvType.style(14.sp, colors.accent))
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
    val colors = LocalEnvColors.current
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
    val colors = LocalEnvColors.current
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
                BasicText(label, style = EnvType.style(16.sp, colors.accent, FontWeight.Medium))
            }
        }
    }
}

internal const val DISABLED_ALPHA = 0.4f
internal val CARD_RADIUS = 6.dp
internal val CARD_MARGIN = 16.dp
internal val CARD_GAP = 4.dp
internal val CARD_PADDING = 16.dp
private const val SELECTED_ALPHA = 0.24f
private const val SCRIM_ALPHA = 0.5f

private val SLIDER_TRACK = 4.dp
private val SLIDER_THUMB = 10.dp
private const val SLIDER_REST_ALPHA = 0.35f
