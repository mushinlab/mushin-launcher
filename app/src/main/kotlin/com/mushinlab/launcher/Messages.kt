package com.mushinlab.launcher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.OverrideLimits
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.pressable
import com.mushinlab.launcher.feature.settings.TextInputDialog
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun RenameDialog(vm: LauncherViewModel, key: AppKey) {
    val entry = vm.model.entry(key)
    val current = vm.model.override(key)?.customLabel ?: entry?.label.orEmpty()
    TextInputDialog(
        title = stringResource(R.string.rename_title, entry?.label.orEmpty()),
        initial = current,
        maxLength = OverrideLimits.LABEL_MAX_LENGTH,
        error = null,
        confirm = stringResource(R.string.rename_save),
        extra = stringResource(R.string.rename_reset) to {
            vm.apps.rename(key, null)
            vm.navigator.renameTarget = null
        },
        onConfirm = { text ->
            vm.apps.rename(key, text)
            vm.navigator.renameTarget = null
        },
        onDismiss = { vm.navigator.renameTarget = null },
    )
}

@Composable
internal fun LearnHint(onOpen: () -> Unit, onDismiss: () -> Unit, onNever: () -> Unit) {
    val colors = LocalMushinColors.current
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dismissPx = with(LocalDensity.current) { LEARN_HINT_SWIPE.toPx() }
    var options by remember { mutableStateOf(false) }
    LaunchedEffect(options) {
        if (!options) {
            delay(LEARN_HINT_MS)
            onDismiss()
        }
    }
    Column(
        Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(16.dp)
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offset.value
                alpha = 1f - (abs(offset.value) / (size.width.takeIf { it > 0f } ?: 1f)).coerceIn(0f, 1f)
            }
            .clip(RoundedCornerShape(LEARN_HINT_RADIUS))
            .background(colors.surface)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (abs(offset.value) > dismissPx) {
                                onDismiss()
                            } else {
                                offset.animateTo(0f, tween(Motion.TRANSITION_MS))
                            }
                        }
                    },
                ) { change, amount ->
                    change.consume()
                    scope.launch { offset.snapTo(offset.value + amount) }
                }
            }
            .pressable(onClick = onOpen)
            .semantics(mergeDescendants = true) { }
            .testTag("learn_hint")
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        LearnHintContent(options, onToggleOptions = { options = !options }, onDismiss, onNever)
    }
}

@Composable
private fun LearnHintContent(
    options: Boolean,
    onToggleOptions: () -> Unit,
    onDismiss: () -> Unit,
    onNever: () -> Unit,
) {
    val colors = LocalMushinColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(
                stringResource(R.string.learn_hint_title),
                style = MushinType.style(17.sp, colors.textPrimary, FontWeight.Medium),
            )
            BasicText(stringResource(R.string.learn_hint_subtitle), style = MushinType.style(15.sp, colors.accent))
        }
        GlyphButton(
            Glyph.CLOSE,
            stringResource(R.string.learn_hint_options),
            colors.textSecondary,
            tag = "learn_hint_close",
            size = LEARN_HINT_CLOSE,
            glyphSize = LEARN_HINT_CLOSE_GLYPH,
            onClick = onToggleOptions,
        )
    }
    if (options) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
            HintButton(stringResource(R.string.learn_hint_hide), "learn_hint_hide", onDismiss)
            HintButton(stringResource(R.string.learn_hint_never), "learn_hint_never", onNever)
        }
    }
}

@Composable
private fun HintButton(label: String, tag: String, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(LEARN_HINT_RADIUS))
            .pressable(onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = MushinType.style(15.sp, LocalMushinColors.current.accent, FontWeight.Medium))
    }
}

@Composable
internal fun NoticeBar(notice: Notice, top: Boolean, onDismiss: () -> Unit) {
    val colors = LocalMushinColors.current
    LaunchedEffect(notice.id) {
        delay(NOTICE_MS)
        onDismiss()
    }
    Row(
        Modifier
            .windowInsetsPadding(if (top) WindowInsets.statusBars else WindowInsets.navigationBars)
            .padding(16.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(colors.surface)
            .testTag("notice")
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            notice.arg?.let { stringResource(notice.text, it) } ?: stringResource(notice.text),
            style = MushinType.style(15.sp, colors.textPrimary),
            modifier = Modifier.weight(1f),
        )
        val label = notice.actionLabel
        val action = notice.action
        if (label != null && action != null) {
            Box(
                Modifier.heightIn(min = 48.dp).pressable {
                    action()
                    onDismiss()
                }.padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) { BasicText(stringResource(label), style = MushinType.style(15.sp, colors.accent)) }
        }
    }
}

private const val NOTICE_MS = 4000L
private const val LEARN_HINT_MS = 8000L
private val LEARN_HINT_SWIPE = 96.dp
private val LEARN_HINT_RADIUS = 12.dp
private val LEARN_HINT_CLOSE = 40.dp
private val LEARN_HINT_CLOSE_GLYPH = 18.dp
