package com.mushinlab.launcher

import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.AppUsage
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.LearnedGesture
import com.mushinlab.launcher.core.model.SpaceUsage
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.model.Usefulness
import com.mushinlab.launcher.core.search.Alphabet
import com.mushinlab.launcher.feature.settings.EditorItem
import com.mushinlab.launcher.feature.settings.GestureSummary
import com.mushinlab.launcher.feature.settings.LearnedApp
import com.mushinlab.launcher.feature.settings.LearnedDrawing
import com.mushinlab.launcher.feature.settings.PickerApp
import com.mushinlab.launcher.feature.settings.RenamedApp
import com.mushinlab.launcher.feature.settings.ScoreRow
import com.mushinlab.launcher.feature.settings.SettingsUiState
import com.mushinlab.launcher.feature.settings.SpaceSummary
import com.mushinlab.launcher.feature.settings.UsageRow
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

internal fun settingsState(
    d: LauncherData,
    gestures: Map<SwipeDirection, GestureAction>,
    roleHeld: Boolean,
    capacity: Int,
    versionName: String,
): SettingsUiState = SettingsUiState(
    settings = d.settings,
    spaces = d.spaces.map {
        SpaceSummary(it.id, it.name, it.type, it.preset, it.id == d.current?.id, it.coloredIcons)
    }.toImmutableList(),
    spaceItems = d.spaces.associate { space ->
        space.id to d.items[space.id].orEmpty().map { key ->
            EditorItem(
                key = key,
                label = d.label(key) ?: key.packageName,
                hidden = d.overrides[key]?.hidden == true,
                missing = key !in d.apps,
            )
        }.toImmutableList()
    }.toImmutableMap(),
    apps = d.apps.values
        .map { PickerApp(it.key, d.label(it.key) ?: it.label, d.overrides[it.key]?.hidden == true) }
        .sortedWith(Alphabet.comparator { it.label })
        .toImmutableList(),
    renamed = d.overrides.values
        .filter { it.customLabel != null && it.key in d.apps }
        .map { RenamedApp(it.key, d.apps[it.key]?.label.orEmpty(), it.customLabel.orEmpty()) }
        .toImmutableList(),
    gestures = gestures.mapValues { (_, action) ->
        val key = (action as? GestureAction.LaunchApp)?.key
        GestureSummary(action, key?.let(d::label), key != null && key !in d.apps)
    }.toImmutableMap(),
    capacity = capacity,
    isDefaultLauncher = roleHeld,
    versionName = versionName,
)

internal fun SettingsUiState.withMemory(samples: List<LearnedGesture>, usage: Map<AppKey, AppUsage>): SettingsUiState {
    val labels = apps.associate { it.key to it.label }
    return copy(
        learned = samples.groupBy { it.key }
            .mapNotNull { (key, list) ->
                val label = labels[key] ?: return@mapNotNull null
                val drawings = list.sortedByDescending {
                    it.weight
                }.map { LearnedDrawing(it.path, it.weight, it.shape) }
                LearnedApp(key, label, drawings.toImmutableList())
            }
            .sortedWith(Alphabet.comparator { it.label })
            .toImmutableList(),
        usage = usage.values
            .mapNotNull { u -> labels[u.key]?.let { UsageRow(u.key, it, u.launchCount) } }
            .sortedByDescending { it.launches }
            .toImmutableList(),
    )
}

internal fun SettingsUiState.withUsefulness(usage: List<SpaceUsage>, now: Long): SettingsUiState {
    val labels = apps.associate { it.key to it.label }
    return copy(
        usefulness = Usefulness.scores(usage, now)
            .mapNotNull { (key, score) -> labels[key]?.let { ScoreRow(key, it, score) } }
            .sortedByDescending { it.score }
            .toImmutableList(),
    )
}
