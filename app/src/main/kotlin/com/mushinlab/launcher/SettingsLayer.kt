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
import com.mushinlab.launcher.feature.settings.AppActions
import com.mushinlab.launcher.feature.settings.MemoryActions
import com.mushinlab.launcher.feature.settings.PreferenceActions
import com.mushinlab.launcher.feature.settings.SettingsActions
import com.mushinlab.launcher.feature.settings.SettingsHost
import com.mushinlab.launcher.feature.settings.SettingsRoute
import com.mushinlab.launcher.feature.settings.SpaceActions
import com.mushinlab.launcher.feature.settings.SystemActions
import com.mushinlab.launcher.feature.settings.TextInputDialog
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun SettingsLayer(vm: LauncherViewModel) {
    val state by vm.settingsState.collectAsStateWithLifecycle()
    val actions = remember(vm) {
        SettingsActions(
            preferences = preferenceActions(vm),
            spaces = spaceActions(vm),
            apps = appActions(vm),
            system = systemActions(vm),
            memory = memoryActions(vm),
        )
    }
    val background by vm.background.collectAsStateWithLifecycle()
    SettingsHost(vm.navigator.settings, state.copy(hasBackground = background != null), actions)
}

private fun preferenceActions(vm: LauncherViewModel) = object : PreferenceActions {
    override fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) = vm.preferences.update(transform)

    override fun setGesture(direction: SwipeDirection, action: GestureAction) = vm.preferences.setGesture(
        direction,
        action,
    )
}

private fun spaceActions(vm: LauncherViewModel) = object : SpaceActions {
    override suspend fun createSpace(name: String, type: SpaceType): String? = vm.spaces.create(name, type)

    override suspend fun renameSpace(id: Long, name: String): String? = vm.spaces.rename(id, name)

    override fun deleteSpace(id: Long) = vm.spaces.delete(id)

    override fun switchSpace(id: Long) = vm.spaces.switchTo(id)

    override fun setPalette(id: Long, preset: PalettePreset) = vm.spaces.setPalette(id, preset)

    override fun setColoredIcons(id: Long, enabled: Boolean) = vm.spaces.setColoredIcons(id, enabled)

    override fun setSpaceItems(id: Long, keys: List<AppKey>) = vm.spaces.setItems(id, keys)

    override fun addSpaceItem(id: Long, key: AppKey) {
        vm.spaces.addItem(id, key)
    }

    override fun removeSpaceItem(id: Long, key: AppKey) = vm.spaces.removeItem(id, key)
}

private fun appActions(vm: LauncherViewModel) = object : AppActions {
    override fun setHidden(key: AppKey, hidden: Boolean) = vm.apps.setHidden(key, hidden)

    override fun rename(key: AppKey, label: String?) = vm.apps.rename(key, label)
}

private fun systemActions(vm: LauncherViewModel) = object : SystemActions {
    override fun requestDefaultLauncher() = vm.request(LauncherEffect.RequestHomeRole)

    override fun openNotificationAccess() = vm.request(LauncherEffect.OpenNotificationAccess)

    override fun openAssistantSettings() = vm.request(LauncherEffect.OpenAssistantSettings)

    override fun requestSearchTile() = vm.request(LauncherEffect.RequestSearchTile)

    override fun pickBackground() = vm.request(LauncherEffect.PickBackground)

    override fun clearBackground() = vm.preferences.clearBackground()
}

private fun memoryActions(vm: LauncherViewModel) = object : MemoryActions {
    override fun forgetDrawing(key: AppKey, path: String) = vm.memory.forget {
        it.key == key && it.path == path
    }

    override fun forgetApp(key: AppKey) = vm.memory.forget { it.key == key }

    override fun addDrawing(key: AppKey, sample: GestureSample) = vm.memory.add(key, sample)

    override fun forgetAllGestures() = vm.memory.forget { true }

    override fun clearUsage() = vm.memory.clearUsage()
}
