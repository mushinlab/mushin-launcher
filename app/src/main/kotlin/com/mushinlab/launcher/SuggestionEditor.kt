package com.mushinlab.launcher

import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.FolderKeys
import com.mushinlab.launcher.core.model.HintHistory
import com.mushinlab.launcher.core.model.SpaceUsage
import com.mushinlab.launcher.core.model.Usefulness
import com.mushinlab.launcher.feature.home.HomeSuggestion
import kotlinx.coroutines.launch

class SuggestionEditor(private val container: AppContainer, private val items: (Long) -> List<AppKey>) {
    fun accept(spaceId: Long, hint: HomeSuggestion, replace: Boolean) {
        val current = items(spaceId)
        val next = if (replace) current.map { if (it == hint.replace) hint.add else it } else current + hint.add
        container.scope.launch {
            container.spaces.setItems(spaceId, next.distinct())
            container.history.recordHint(spaceId)
        }
    }

    fun dismiss(spaceId: Long, hint: HomeSuggestion) {
        container.scope.launch {
            container.history.dismissSuggestion(spaceId, hint.add)
            container.history.recordHint(spaceId)
        }
    }
}

internal data class SpaceStats(
    val usage: Map<Long, List<SpaceUsage>>,
    val launchDays: Map<Long, Map<AppKey, Int>>,
)

internal fun suggestionFor(
    d: LauncherData,
    stats: SpaceStats,
    swipeApps: Set<AppKey>,
    cap: Int,
    now: Long,
): HomeSuggestion? {
    val space = d.current ?: return null
    val rows = d.items[space.id].orEmpty()
    val home = rows.filter { FolderKeys.idOf(it) == null }
    val elsewhere = d.items.filterKeys { it != space.id }.values.flatten().toSet()
    val label = { key: AppKey -> d.overrides[key]?.customLabel ?: d.apps[key]?.label.orEmpty() }
    val hint = Usefulness.suggest(
        home = home,
        usage = stats.usage[space.id].orEmpty(),
        eligible = { key ->
            val override = d.overrides[key]
            key in d.apps && override?.hidden != true && key !in swipeApps && key !in elsewhere
        },
        now = now,
        history = HintHistory(space.createdAt, space.hintAt, stats.launchDays[space.id].orEmpty()),
    ) ?: return null
    return HomeSuggestion(hint.add, label(hint.add), hint.replace, label(hint.replace), cap > 0 && rows.size < cap)
}
