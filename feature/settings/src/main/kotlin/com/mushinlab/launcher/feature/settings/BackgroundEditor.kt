package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.BackgroundCrop
import com.mushinlab.launcher.core.model.CropFrame
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.SpaceBackground
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.LocalMushinFont
import com.mushinlab.launcher.core.ui.LocalMushinStroke
import com.mushinlab.launcher.core.ui.MushinTheme
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SpaceBackdrop
import com.mushinlab.launcher.core.ui.pressable
import kotlin.math.min
import kotlin.math.roundToInt

@Stable
internal class BackgroundEditor(brightness: Float, blur: Float, rowAlpha: Float) {
    var crop by mutableStateOf(BackgroundCrop())
    var brightness by mutableFloatStateOf(brightness)
    var blur by mutableFloatStateOf(blur)
    var rowAlpha by mutableFloatStateOf(rowAlpha)

    fun edit(spaceId: Long, frame: IntSize) =
        BackgroundEdit(spaceId, crop, frame.width, frame.height, brightness, blur, rowAlpha)
}

@Composable
internal fun BackgroundEditorScreen(
    spaceId: Long,
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    val space = state.spaces.firstOrNull { it.id == spaceId }
    if (space == null) {
        back()
        return
    }
    val background = actions.background
    LaunchedEffect(spaceId) { background.edit(spaceId) }
    DisposableEffect(spaceId) { onDispose { background.close(spaceId) } }
    val image = state.backgroundSource?.takeIf { it.spaceId == spaceId }?.image
    val editor = remember(spaceId) {
        BackgroundEditor(space.backgroundBrightness, space.backgroundBlur, space.backgroundRowAlpha)
    }
    LaunchedEffect(image) { editor.crop = BackgroundCrop() }
    val window = LocalWindowInfo.current.containerSize
    var confirmRemove by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .background(LocalMushinColors.current.background)
            .testTag("background_editor"),
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        EditorBar {
            EditorButton(stringResource(R.string.settings_cancel), "background_cancel", onClick = back)
        }
        EditorPreview(space, state.settings, image, editor, window, Modifier.weight(1f).fillMaxWidth())
        EditorSliders(editor)
        ImageActions(replaceable = image != null, onRemove = { confirmRemove = true }) {
            push(SettingsRoute.BackgroundPicker(spaceId))
        }
        EditorBar {
            Spacer(Modifier.weight(1f))
            EditorButton(
                stringResource(R.string.settings_save),
                "background_save",
                primary = true,
                enabled = image != null && window.width > 0,
            ) {
                background.save(editor.edit(spaceId, window))
                back()
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
    if (confirmRemove) {
        ConfirmDialog(
            message = stringResource(R.string.background_remove_confirm),
            confirm = stringResource(R.string.space_background_remove),
            onConfirm = {
                confirmRemove = false
                background.remove(spaceId)
                back()
            },
            onDismiss = { confirmRemove = false },
        )
    }
}

@Composable
private fun EditorPreview(
    space: SpaceSummary,
    settings: LauncherSettings,
    image: ImageBitmap?,
    editor: BackgroundEditor,
    window: IntSize,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier.padding(vertical = PREVIEW_MARGIN), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val frame = if (window.width > 0 && window.height > 0) {
            window
        } else {
            with(density) { IntSize(maxWidth.roundToPx(), maxHeight.roundToPx()) }
        }
        val width = with(density) { frame.width.toDp() }
        val height = with(density) { frame.height.toDp() }
        val scale = min(1f, min(maxWidth / width, maxHeight / height))
        Box(Modifier.size(width * scale, height * scale).clipToBounds(), contentAlignment = Alignment.Center) {
            PreviewFrame(space, settings, image, editor, Modifier.requiredSize(width, height), scale)
        }
    }
}

@Composable
private fun PreviewFrame(
    space: SpaceSummary,
    settings: LauncherSettings,
    image: ImageBitmap?,
    editor: BackgroundEditor,
    modifier: Modifier,
    scale: Float,
) {
    MushinTheme(space.palette, LocalMushinFont.current, LocalMushinStroke.current) {
        Box(
            modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clipToBounds()
                .background(LocalMushinColors.current.background)
                .cropGestures(image, editor)
                .testTag("background_preview"),
        ) {
            if (image != null) {
                SpaceBackdrop(
                    image,
                    editor.brightness,
                    editor.blur,
                    Modifier.fillMaxSize().testTag("background_preview_image"),
                    editor.crop,
                )
            }
            HomeMock(settings, overImage = image != null, rowAlpha = editor.rowAlpha)
        }
    }
}

private fun Modifier.cropGestures(image: ImageBitmap?, editor: BackgroundEditor): Modifier = if (image == null) {
    this
} else {
    this
        .pointerInput(image) {
            detectTransformGestures { centroid, pan, zoom, _ ->
                val frame = CropFrame(image.width, image.height, size.width.toFloat(), size.height.toFloat())
                val zoomed = frame.zoom(editor.crop, centroid.x - pan.x, centroid.y - pan.y, zoom)
                editor.crop = frame.pan(zoomed, pan.x, pan.y)
            }
        }
        .pointerInput(image) {
            detectTapGestures(onDoubleTap = { editor.crop = BackgroundCrop() })
        }
}

@Composable
private fun EditorSliders(editor: BackgroundEditor) {
    val labels = listOf(
        stringResource(R.string.background_brightness),
        stringResource(R.string.background_blur),
        stringResource(R.string.background_row_alpha),
    )
    val labelWidth = rememberLabelWidth(labels, MushinType.style(15.sp, LocalMushinColors.current.textPrimary))
    InlineSliderRow(
        labels[0],
        labelWidth,
        editor.brightness,
        SpaceBackground.BRIGHTNESS_MIN..SpaceBackground.BRIGHTNESS_MAX,
        SpaceBackground.STEP,
        tag = "background_brightness",
        format = ::percent,
    ) { editor.brightness = it }
    InlineSliderRow(
        labels[1],
        labelWidth,
        editor.blur,
        0f..SpaceBackground.BLUR_MAX,
        SpaceBackground.STEP,
        tag = "background_blur",
        format = ::percent,
    ) { editor.blur = it }
    InlineSliderRow(
        labels[2],
        labelWidth,
        editor.rowAlpha,
        SpaceBackground.ROW_ALPHA_MIN..SpaceBackground.ROW_ALPHA_MAX,
        SpaceBackground.STEP,
        tag = "background_row_alpha",
        format = ::percent,
    ) { editor.rowAlpha = it }
}

@Composable
private fun rememberLabelWidth(labels: List<String>, style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(labels, style, density) {
        val widest = labels.maxOf { measurer.measure(it, style, maxLines = 1).size.width }
        with(density) { widest.toDp() }.coerceAtMost(LABEL_MAX_WIDTH)
    }
}

@Composable
private fun EditorBar(buttons: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = CARD_MARGIN, vertical = CARD_GAP),
        horizontalArrangement = Arrangement.spacedBy(CARD_GAP),
        verticalAlignment = Alignment.CenterVertically,
        content = buttons,
    )
}

@Composable
private fun EditorButton(
    label: String,
    tag: String,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = LocalMushinColors.current
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    Box(
        Modifier
            .heightIn(min = BUTTON_HEIGHT)
            .clip(RoundedCornerShape(CARD_RADIUS))
            .background(if (primary) colors.accent.copy(alpha = alpha) else Color.Transparent)
            .pressable(enabled = enabled, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = BUTTON_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        val text = if (primary) colors.background else colors.accent.copy(alpha = alpha)
        BasicText(label, style = MushinType.style(16.sp, text, FontWeight.Medium))
    }
}

private fun percent(value: Float): String = "${(value * PERCENT).roundToInt()} %"

private const val PERCENT = 100
private val PREVIEW_MARGIN = 8.dp
private val BUTTON_HEIGHT = 48.dp
private val BUTTON_PADDING = 16.dp
private val LABEL_MAX_WIDTH = 140.dp
