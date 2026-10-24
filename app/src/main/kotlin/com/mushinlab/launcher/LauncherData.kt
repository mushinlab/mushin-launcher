package com.mushinlab.launcher

import com.mushinlab.launcher.core.apps.NotificationSource
import com.mushinlab.launcher.core.data.repo.SpaceError
import com.mushinlab.launcher.core.data.snapshot.HomeSnapshot
import com.mushinlab.launcher.core.data.snapshot.SnapshotRow
import com.mushinlab.launcher.core.model.AppEntry
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.AppOverride
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.ProfileType
import com.mushinlab.launcher.core.model.SearchRecent
import com.mushinlab.launcher.core.model.Space
import com.mushinlab.launcher.core.model.SpaceBackground
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.search.Alphabet
import com.mushinlab.launcher.core.search.SearchIndex
import com.mushinlab.launcher.core.search.SearchItem
import com.mushinlab.launcher.feature.home.HomeRow
import com.mushinlab.launcher.feature.home.HomeUiState
import com.mushinlab.launcher.feature.home.PopupSpace
import com.mushinlab.launcher.feature.settings.ERR_CUSTOM
import com.mushinlab.launcher.feature.settings.ERR_EMPTY
import com.mushinlab.launcher.feature.settings.ERR_LONG
import com.mushinlab.launcher.feature.settings.ERR_TAKEN
import com.mushinlab.launcher.feature.settings.ERR_WORK
import com.mushinlab.launcher.feature.settings.EditorItem
import com.mushinlab.launcher.feature.settings.GestureSummary
import com.mushinlab.launcher.feature.settings.PickerApp
import com.mushinlab.launcher.feature.settings.RenamedApp
import com.mushinlab.launcher.feature.settings.SettingsUiState
import com.mushinlab.launcher.feature.settings.SpaceSummary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

data class LauncherData(
    val spaces: List<Space>,
    val current: Space?,
    val items: Map<Long, List<AppKey>>,
    val apps: Map<AppKey, AppEntry>,
    val overrides: Map<AppKey, AppOverride>,
    val settings: LauncherSettings,
) {
    fun label(key: AppKey): String? = overrides[key]?.customLabel ?: apps[key]?.label

    fun currentItems(): List<AppKey> = current?.let { items[it.id] }.orEmpty()
}

internal fun homeState(
    d: LauncherData,
    roleHeld: Boolean,
    counts: Map<NotificationSource, Int>,
    swipes: Map<AppKey, List<SwipeDirection>>,
): HomeUiState {
    val rows = d.currentItems().mapNotNull { key ->
        val entry = d.apps[key] ?: return@mapNotNull null
        HomeRow(
            key = key,
            label = d.overrides[key]?.customLabel ?: entry.label,
            dimmed = entry.isDimmed,
            work = entry.profile == ProfileType.WORK,
            notifications = if (d.settings.notificationCounts) {
                counts[NotificationSource(key.packageName, key.userSerial)] ?: 0
            } else {
                0
            },
            swipes = swipes[key]?.toImmutableList() ?: persistentListOf(),
        )
    }
    return HomeUiState(
        rows = rows.toImmutableList(),
        iconMode = d.settings.iconMode,
        textSize = d.settings.homeTextSize,
        textAlign = d.settings.homeTextAlign,
        drawing = d.settings.gestureInput,
        clockFormat = d.settings.clockFormat,
        showDate = d.settings.showDate,
        clockPosition = d.settings.clockPosition,
        homeListOffset = d.current?.listOffset ?: d.settings.homeListOffset,
        backgroundBrightness = d.current?.backgroundBrightness ?: 0f,
        backgroundBlur = d.current?.backgroundBlur ?: 0f,
        backgroundRowAlpha = d.current?.backgroundRowAlpha ?: SpaceBackground.ROW_ALPHA_DEFAULT,
        roleMissing = !roleHeld,
    )
}

internal fun snapshotState(snapshot: HomeSnapshot?, roleHeld: Boolean): HomeUiState = if (snapshot == null) {
    HomeUiState(roleMissing = !roleHeld)
} else {
    HomeUiState(
        rows = snapshot.rows.map { HomeRow(it.key, it.label, it.dimmed, it.work) }.toImmutableList(),
        iconMode = snapshot.iconMode,
        textSize = snapshot.textSize,
        textAlign = snapshot.textAlign,
        clockFormat = snapshot.clockFormat,
        showDate = snapshot.showDate,
        clockPosition = snapshot.clockPosition,
        homeListOffset = snapshot.listOffset,
        roleMissing = !roleHeld,
    )
}

internal fun homeSnapshot(d: LauncherData, state: HomeUiState): HomeSnapshot? {
    val space = d.current ?: return null
    return HomeSnapshot(
        spaceId = space.id,
        palette = space.palette,
        iconMode = state.iconMode,
        textSize = state.textSize,
        clockFormat = state.clockFormat,
        showDate = state.showDate,
        rows = state.rows.map { SnapshotRow(it.key, it.label, it.dimmed, it.work) },
        clockPosition = state.clockPosition,
        listOffset = state.homeListOffset,
        textAlign = state.textAlign,
    )
}

internal fun popupSpaces(d: LauncherData?): ImmutableList<PopupSpace> =
    d?.spaces?.map { PopupSpace(it.id, it.name, it.id == d.current?.id) }?.toImmutableList() ?: persistentListOf()

internal fun searchIndex(d: LauncherData): SearchIndex = SearchIndex.build(
    d.apps.values.map { entry ->
        val override = d.overrides[entry.key]
        SearchItem(entry.key, entry.label, override?.customLabel, override?.hidden == true)
    },
)

internal fun recentKeys(d: LauncherData?, recents: Map<Long, List<SearchRecent>>): ImmutableList<AppKey> {
    val space = d?.current ?: return persistentListOf()
    return recents[space.id].orEmpty().map { it.key }.toImmutableList()
}

internal fun SpaceError.code(): String = when (this) {
    SpaceError.NameEmpty -> ERR_EMPTY
    SpaceError.NameTooLong -> ERR_LONG
    SpaceError.NameTaken -> ERR_TAKEN
    SpaceError.WorkLimitReached -> ERR_WORK
    SpaceError.CustomLimitReached -> ERR_CUSTOM
    else -> toString()
}
