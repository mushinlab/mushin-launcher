package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.AppFont
import com.mushinlab.launcher.core.model.AppFontWeight
import com.mushinlab.launcher.core.model.HomeTextSize
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.PressFeedback
import com.mushinlab.launcher.core.model.StrokeColor
import com.mushinlab.launcher.core.model.StrokeWidth
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinColors
import com.mushinlab.launcher.core.ui.MushinFonts
import com.mushinlab.launcher.core.ui.MushinStroke
import com.mushinlab.launcher.core.ui.SpaceBackdrop
import kotlin.math.roundToInt

@Composable
internal fun StyleScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    SettingsPage(stringResource(R.string.settings_style), back) {
        state.spaces.firstOrNull { it.current }?.let { space ->
            SectionHeader(stringResource(R.string.style_theme, space.name))
            PalettePicker(space.preset) { actions.spaces.setPalette(space.id, it) }
            ToggleRow(stringResource(R.string.space_colored_icons), space.coloredIcons, tag = "style_colored_icons") {
                actions.spaces.setColoredIcons(space.id, it)
            }
            SectionHeader(stringResource(R.string.space_background))
            SpaceBackgroundSection(space, actions.background, push)
            SectionHeader(stringResource(R.string.style_text))
        }
        TextStyleRows(state.settings, actions.preferences)
        NoteText(stringResource(R.string.style_note))
        SectionHeader(stringResource(R.string.settings_home))
        HomeSettingsSection(state, actions)
        SectionHeader(stringResource(R.string.style_stroke))
        StrokeRows(state.settings, actions.preferences)
    }
}

@Composable
private fun SpaceBackgroundSection(space: SpaceSummary, actions: BackgroundActions, push: (SettingsRoute) -> Unit) {
    NoteText(stringResource(R.string.style_background_hint, space.name))
    if (space.hasBackground) BackgroundThumbnail(space, actions)
    NavRow(stringResource(R.string.space_background_change), tag = "space_background_change") {
        if (space.hasBackground) push(SettingsRoute.BackgroundEditor(space.id)) else actions.pick(space.id)
    }
    if (space.hasBackground) {
        NavRow(stringResource(R.string.space_background_remove), tag = "space_background_remove") {
            actions.remove(space.id)
        }
    }
    NoteText(stringResource(R.string.space_background_note))
}

@Composable
private fun BackgroundThumbnail(space: SpaceSummary, actions: BackgroundActions) {
    val image by produceState<ImageBitmap?>(null, space.id, space.backgroundVersion) {
        value = actions.thumbnail(space.id)
    }
    val window = LocalWindowInfo.current.containerSize
    val screenHeight = with(LocalDensity.current) { window.height.toDp() }
    val description = stringResource(R.string.space_background_preview)
    Box(
        Modifier
            .padding(horizontal = CARD_MARGIN, vertical = CARD_GAP)
            .height(THUMBNAIL_HEIGHT)
            .aspectRatio(if (window.height > 0) window.width.toFloat() / window.height else DEFAULT_RATIO)
            .clip(RoundedCornerShape(CARD_RADIUS))
            .background(MushinColors.from(space.palette).background)
            .semantics { contentDescription = description }
            .testTag("space_background_preview"),
    ) {
        image?.let {
            val blur = if (screenHeight > 0.dp) space.backgroundBlur * (THUMBNAIL_HEIGHT / screenHeight) else 0f
            SpaceBackdrop(it, space.backgroundBrightness, blur, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun TextStyleRows(s: LauncherSettings, preferences: PreferenceActions) {
    val colors = LocalMushinColors.current
    DropdownRow(
        stringResource(R.string.style_font),
        listOf(
            AppFont.SYSTEM to stringResource(R.string.font_system),
            AppFont.INTER to "Inter",
            AppFont.OPEN_SANS to "Open Sans",
            AppFont.MONTSERRAT to "Montserrat",
            AppFont.CONDENSED to "Roboto Condensed",
        ),
        s.font,
        "font",
        style = { font ->
            TextStyle(
                fontFamily = MushinFonts.family(font),
                fontSize = 17.sp,
                fontWeight = FontWeight(s.fontWeight.value),
                color = colors.textPrimary,
            )
        },
    ) { font -> preferences.updateSettings { it.copy(font = font) } }
    DropdownRow(
        stringResource(R.string.style_weight),
        listOf(
            AppFontWeight.LIGHT to stringResource(R.string.weight_light),
            AppFontWeight.REGULAR to stringResource(R.string.weight_regular),
            AppFontWeight.MEDIUM to stringResource(R.string.weight_medium),
        ),
        s.fontWeight,
        "font_weight",
    ) { weight -> preferences.updateSettings { it.copy(fontWeight = weight) } }
    DropdownRow(
        stringResource(R.string.home_text_size),
        listOf(
            HomeTextSize.S to stringResource(R.string.home_text_s),
            HomeTextSize.M to stringResource(R.string.home_text_m),
            HomeTextSize.L to stringResource(R.string.home_text_l),
        ),
        s.homeTextSize,
        "text_size",
    ) { size -> preferences.updateSettings { it.copy(homeTextSize = size) } }
}

@Composable
private fun StrokeRows(s: LauncherSettings, preferences: PreferenceActions) {
    SliderRow(
        stringResource(R.string.style_stroke_width),
        s.strokeWidth.toFloat(),
        StrokeWidth.MIN_DP.toFloat()..StrokeWidth.MAX_DP.toFloat(),
        1f,
        tag = "stroke_width",
        format = { "${it.roundToInt()} dp" },
    ) { v -> preferences.updateSettings { it.copy(strokeWidth = v.roundToInt()) } }
    StrokeColorDropdown(s.strokeColor) { color -> preferences.updateSettings { it.copy(strokeColor = color) } }
    ToggleRow(stringResource(R.string.style_stroke_dots), s.strokeDots, tag = "stroke_dots") { on ->
        preferences.updateSettings { it.copy(strokeDots = on) }
    }
}

@Composable
private fun StrokeColorDropdown(selected: StrokeColor, onSelect: (StrokeColor) -> Unit) {
    val colors = LocalMushinColors.current
    val names = mapOf(
        StrokeColor.ACCENT to stringResource(R.string.stroke_accent),
        StrokeColor.TEXT to stringResource(R.string.stroke_text),
        StrokeColor.CORAL to stringResource(R.string.stroke_coral),
        StrokeColor.ORANGE to stringResource(R.string.stroke_orange),
        StrokeColor.AMBER to stringResource(R.string.stroke_amber),
        StrokeColor.LIME to stringResource(R.string.stroke_lime),
        StrokeColor.GREEN to stringResource(R.string.stroke_green),
        StrokeColor.TEAL to stringResource(R.string.stroke_teal),
        StrokeColor.SKY to stringResource(R.string.stroke_sky),
        StrokeColor.BLUE to stringResource(R.string.stroke_blue),
        StrokeColor.VIOLET to stringResource(R.string.stroke_violet),
        StrokeColor.PINK to stringResource(R.string.stroke_pink),
    )
    DropdownRow(
        stringResource(R.string.style_stroke_color),
        StrokeColor.entries.map { it to names[it].orEmpty() },
        selected,
        "stroke_color",
        leading = { color ->
            Box(
                Modifier
                    .size(STROKE_SWATCH)
                    .clip(CircleShape)
                    .background(MushinStroke(StrokeWidth.DEFAULT_DP.dp, color).color(colors)),
            )
        },
        onSelect = onSelect,
    )
}

@Composable
internal fun pressLabel(feedback: PressFeedback): String = stringResource(
    when (feedback) {
        PressFeedback.FLASH -> R.string.press_flash
        PressFeedback.HOLD -> R.string.press_hold
        PressFeedback.ACCENT -> R.string.press_accent
        PressFeedback.DIM -> R.string.press_dim
        PressFeedback.SCALE -> R.string.press_scale
    },
)

@Composable
internal fun PressSettingsScreen(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    SettingsPage(stringResource(R.string.settings_press), back) {
        DropdownRow(
            stringResource(R.string.settings_press),
            PressFeedback.entries.map { it to pressLabel(it) },
            state.settings.pressFeedback,
            "press",
        ) { feedback -> actions.preferences.updateSettings { it.copy(pressFeedback = feedback) } }
        NoteText(stringResource(R.string.press_note))
    }
}

private val STROKE_SWATCH = 18.dp

private val THUMBNAIL_HEIGHT = 160.dp
private const val DEFAULT_RATIO = 0.45f
