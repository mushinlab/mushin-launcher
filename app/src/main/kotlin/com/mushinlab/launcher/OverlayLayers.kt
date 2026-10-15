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
internal fun SuggestionLayer(vm: LauncherViewModel) {
    val overlay = vm.navigator.overlay as? Overlay.Suggestion ?: return
    val home by vm.home.collectAsStateWithLifecycle()
    val suggestion = home.suggestion
    if (suggestion == null) {
        LaunchedEffect(Unit) { vm.navigator.overlay = Overlay.None }
        return
    }
    val actions = remember(vm) {
        object : SuggestionActions {
            override fun onReplace(suggestion: HomeSuggestion) {
                vm.navigator.overlay = Overlay.None
                vm.model.currentSpace?.let { vm.suggestions.accept(it.id, suggestion, replace = true) }
            }

            override fun onAdd(suggestion: HomeSuggestion) {
                vm.navigator.overlay = Overlay.None
                vm.model.currentSpace?.let { vm.suggestions.accept(it.id, suggestion, replace = false) }
            }

            override fun onDismissSuggestion(suggestion: HomeSuggestion) {
                vm.navigator.overlay = Overlay.None
                vm.model.currentSpace?.let { vm.suggestions.dismiss(it.id, suggestion) }
            }

            override fun onClose() {
                vm.navigator.overlay = Overlay.None
            }
        }
    }
    Box(Modifier.zIndex(2f).fillMaxSize()) {
        SuggestionPanel(overlay.anchor, suggestion, actions)
    }
}

@Composable
internal fun PopupLayer(vm: LauncherViewModel) {
    val popup = vm.navigator.overlay as? Overlay.Popup ?: return
    val spaces by vm.popupSpaces.collectAsStateWithLifecycle()
    val app = popup.app?.let { key ->
        val entry = vm.model.entry(key) ?: return@let null
        PopupApp(
            key = key,
            label = vm.model.override(key)?.customLabel ?: entry.label,
            hidden = vm.model.override(key)?.hidden == true,
            inSpace = vm.model.isInCurrentSpace(key),
            canUninstall = !entry.isSystem,
        )
    }
    val actions = remember(vm) { popupActions(vm) }
    Box(Modifier.zIndex(2f).fillMaxSize()) {
        HomePopup(popup.anchor, app, spaces, actions)
    }
}

private fun popupActions(vm: LauncherViewModel) = object : PopupActions {
    override fun onSelectSpace(id: Long) {
        vm.spaces.switchTo(id)
        vm.navigator.overlay = Overlay.None
    }

    override fun onOpenSettings() {
        vm.navigator.overlay = Overlay.None
        vm.navigator.settings.open()
    }

    override fun onOpenSpaceSettings(id: Long) {
        vm.navigator.overlay = Overlay.None
        vm.navigator.settings.open(SettingsRoute.SpaceEditor(id))
    }

    override fun onAbout(key: AppKey) {
        vm.navigator.overlay = Overlay.None
        vm.apps.openDetails(key)
    }

    override fun onRename(key: AppKey) {
        vm.navigator.overlay = Overlay.None
        vm.navigator.renameTarget = key
    }

    override fun onToggleHidden(key: AppKey, hidden: Boolean) {
        vm.navigator.overlay = Overlay.None
        vm.apps.setHidden(key, hidden)
    }

    override fun onRemoveFromSpace(key: AppKey) {
        vm.navigator.overlay = Overlay.None
        vm.model.removeFromCurrentSpace(key)
    }

    override fun onUninstall(key: AppKey) {
        vm.navigator.overlay = Overlay.None
        vm.apps.uninstall(key)
    }

    override fun onDismiss() {
        vm.navigator.overlay = Overlay.None
    }
}
