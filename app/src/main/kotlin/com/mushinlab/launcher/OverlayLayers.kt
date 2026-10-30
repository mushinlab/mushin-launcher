package com.mushinlab.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.feature.home.FolderPanel
import com.mushinlab.launcher.feature.home.HomePopup
import com.mushinlab.launcher.feature.home.HomeSuggestion
import com.mushinlab.launcher.feature.home.PopupActions
import com.mushinlab.launcher.feature.home.PopupApp
import com.mushinlab.launcher.feature.home.SuggestionActions
import com.mushinlab.launcher.feature.home.SuggestionPanel
import com.mushinlab.launcher.feature.settings.SettingsRoute

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
internal fun FolderLayer(vm: LauncherViewModel) {
    val overlay = vm.navigator.overlay as? Overlay.Folder ?: return
    val folders by vm.folderRows.collectAsStateWithLifecycle()
    val apps = folders[overlay.id]
    if (apps.isNullOrEmpty()) {
        LaunchedEffect(Unit) { vm.navigator.overlay = Overlay.None }
        return
    }
    Box(Modifier.zIndex(2f).fillMaxSize()) {
        FolderPanel(
            overlay.anchor,
            apps,
            onLaunch = { key ->
                vm.navigator.overlay = Overlay.None
                vm.launches.launch(key, fromSearch = false)
            },
            onDismiss = { vm.navigator.overlay = Overlay.None },
        )
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
