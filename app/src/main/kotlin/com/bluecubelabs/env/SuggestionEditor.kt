package com.bluecubelabs.env

import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.SpaceUsage
import com.bluecubelabs.env.core.model.Usefulness
import com.bluecubelabs.env.feature.home.HomeSuggestion
import kotlinx.coroutines.launch

class SuggestionEditor(private val container: AppContainer, private val items: (Long) -> List<AppKey>) {
    fun accept(envId: Long, hint: HomeSuggestion, replace: Boolean) {
        val current = items(envId)
        val next = if (replace) current.map { if (it == hint.replace) hint.add else it } else current + hint.add
        container.scope.launch { container.environments.setItems(envId, next.distinct()) }
    }

    fun dismiss(envId: Long, hint: HomeSuggestion) {
        container.scope.launch { container.history.dismissSuggestion(envId, hint.add) }
    }
}

internal fun suggestionFor(
    d: LauncherData,
    usage: Map<Long, List<SpaceUsage>>,
    swipeApps: Set<AppKey>,
    cap: Int,
    now: Long,
): HomeSuggestion? {
    val env = d.current ?: return null
    val home = d.items[env.id].orEmpty()
    val elsewhere = d.items.filterKeys { it != env.id }.values.flatten().toSet()
    val label = { key: AppKey -> d.overrides[key]?.customLabel ?: d.apps[key]?.label.orEmpty() }
    val hint = Usefulness.suggest(
        home = home,
        usage = usage[env.id].orEmpty(),
        eligible = { key ->
            val override = d.overrides[key]
            key in d.apps && override?.hidden != true && override?.systemShortcut != true &&
                key !in swipeApps && key !in elsewhere
        },
        now = now,
    ) ?: return null
    return HomeSuggestion(hint.add, label(hint.add), hint.replace, label(hint.replace), cap > 0 && home.size < cap)
}
