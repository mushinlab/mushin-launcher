package com.mushinlab.launcher

import androidx.activity.compose.BackHandler
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.OverrideLimits
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.model.swipesByApp
import com.mushinlab.launcher.core.search.GestureSample
import com.mushinlab.launcher.core.search.KeyboardLayouts
import com.mushinlab.launcher.core.ui.LocalIconProvider
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.LocalPressFeedback
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.core.ui.MushinFont
import com.mushinlab.launcher.core.ui.MushinStroke
import com.mushinlab.launcher.core.ui.MushinTheme
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.pressable
import com.mushinlab.launcher.feature.home.HomeActions
import com.mushinlab.launcher.feature.home.HomePopup
import com.mushinlab.launcher.feature.home.HomeRow
import com.mushinlab.launcher.feature.home.HomeScreen
import com.mushinlab.launcher.feature.home.HomeSuggestion
import com.mushinlab.launcher.feature.home.PopupActions
import com.mushinlab.launcher.feature.home.PopupApp
import com.mushinlab.launcher.feature.home.SuggestionActions
import com.mushinlab.launcher.feature.home.SuggestionPanel
import com.mushinlab.launcher.feature.search.SearchActions
import com.mushinlab.launcher.feature.search.SearchChrome
import com.mushinlab.launcher.feature.search.SearchOption
import com.mushinlab.launcher.feature.search.SearchScreen
import com.mushinlab.launcher.feature.settings.SettingsActions
import com.mushinlab.launcher.feature.settings.SettingsHost
import com.mushinlab.launcher.feature.settings.SettingsRoute
import com.mushinlab.launcher.feature.settings.TextInputDialog
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
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
internal fun LearnHint(onOpen: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalMushinColors.current
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dismissPx = with(LocalDensity.current) { LEARN_HINT_SWIPE.toPx() }
    LaunchedEffect(Unit) {
        delay(LEARN_HINT_MS)
        onDismiss()
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
        BasicText(
            stringResource(R.string.learn_hint_title),
            style = MushinType.style(17.sp, colors.textPrimary, FontWeight.Medium),
        )
        BasicText(stringResource(R.string.learn_hint_subtitle), style = MushinType.style(15.sp, colors.accent))
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
            stringResource(notice.text, *notice.args.toTypedArray()),
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
