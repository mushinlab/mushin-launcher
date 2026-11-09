package com.mushinlab.launcher

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mushinlab.launcher.core.model.HostedWidget
import com.mushinlab.launcher.core.model.PendingWidget
import com.mushinlab.launcher.core.model.WidgetCell
import com.mushinlab.launcher.core.model.WidgetLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WidgetEditor(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    private val currentSpace: () -> Long?,
    private val emit: (LauncherEffect) -> Unit,
) {
    private val context = container.applicationContext()
    private val repository = container.widgets
    private val host = container.widgetHost
    val panels = WidgetPanelEditor(repository, scope, currentSpace, host)
    var choices by mutableStateOf<List<WidgetChoice>?>(null)
        private set
    private var choosing = false
    private var target: WidgetCell? = null
    private val manager = AppWidgetManager.getInstance(context)
    private val initialized = scope.async {
        val state = repository.loaded.filterNotNull().first()
        val retained =
            state.layouts.values.flatMap { it.widgets }.map { it.id }.toSet() + listOfNotNull(state.pending?.id)
        host.appWidgetIds.filterNot(retained::contains).forEach(host::deleteAppWidgetId)
        if (!container.settings.loaded.filterNotNull().first().showClock) {
            panels.removeClocks()
            container.settings.update { it.copy(showClock = true) }
        }
    }

    fun add(cell: WidgetCell? = null) {
        val spaceId = currentSpace() ?: return
        if (choosing) return
        val layout = repository.loaded.value?.layouts?.get(spaceId) ?: WidgetLayout()
        val free = layout.freeCells
        target = cell?.takeIf { it in free } ?: free.firstOrNull() ?: return
        val clock = if (layout.widgets.none { it.isClock }) listOf(WidgetChoice.Clock) else emptyList()
        choices = clock + compactProviders(context).map { WidgetChoice.Hosted(it) }
    }

    fun dismissChoices() {
        choices = null
    }

    fun choose(choice: WidgetChoice) {
        val spaceId = currentSpace() ?: return
        choices = null
        when (choice) {
            WidgetChoice.Clock -> panels.append(spaceId, HostedWidget.clock, target)
            is WidgetChoice.Hosted -> bind(spaceId, info = choice.info, cell = target)
        }
    }

    private fun bind(spaceId: Long, info: AppWidgetProviderInfo, cell: WidgetCell?) {
        if (choosing || !fitsCompactWidget(context, info)) return
        choosing = true
        scope.launch {
            initialized.await()
            cancelPending()
            val id = host.allocateAppWidgetId()
            repository.update { it.copy(pending = PendingWidget(spaceId, id, cell)) }
            emit(LauncherEffect.BindWidget(id, info))
        }
    }

    fun onPicked(success: Boolean) = scope.launch {
        initialized.await()
        val pending = repository.loaded.filterNotNull().first().pending ?: return@launch
        when {
            !success -> {
                cancelPending()
                choosing = false
            }

            manager.getAppWidgetInfo(pending.id)?.configure != null -> emit(LauncherEffect.ConfigureWidget(pending.id))

            else -> {
                finish(pending)
                choosing = false
            }
        }
    }

    fun onConfigured(success: Boolean) = scope.launch {
        initialized.await()
        val pending = repository.loaded.filterNotNull().first().pending ?: return@launch
        if (success) finish(pending) else cancelPending()
        choosing = false
    }

    fun unavailable() {
        onConfigured(false)
        emit(LauncherEffect.WidgetUnavailable)
    }

    suspend fun refresh(ids: Set<Long> = container.spaces.spaces.value.map { it.id }.toSet()) {
        initialized.await()
        if (ids.isEmpty()) return
        val state = repository.loaded.filterNotNull().first()
        val removed = state.layouts.flatMap { (space, layout) ->
            layout.widgets.filter { !it.isClock && (space !in ids || manager.getAppWidgetInfo(it.id) == null) }
        }
        val removedIds = removed.map { it.id }.toSet()
        repository.update { current ->
            current.copy(
                layouts = current.layouts.filterKeys(ids::contains).mapValues { (_, layout) ->
                    layout.copy(widgets = layout.widgets.filterNot { it.id in removedIds })
                },
            )
        }
        removed.forEach { host.deleteAppWidgetId(it.id) }
        if (state.pending?.spaceId?.let { it !in ids } == true) cancelPending()
    }

    private suspend fun finish(pending: PendingWidget) {
        val info = manager.getAppWidgetInfo(pending.id)
        if (info == null || container.spaces.spaces.value.none { it.id == pending.spaceId }) {
            cancelPending()
            return
        }
        val widget = HostedWidget(pending.id, info.loadLabel(context.packageManager))
        var added = false
        repository.update { state ->
            val layout = state.layouts[pending.spaceId] ?: WidgetLayout()
            val next = layout.add(widget, pending.cell)
            added = next != null
            state.copy(layouts = state.layouts + (pending.spaceId to (next ?: layout)), pending = null)
        }
        if (!added) host.deleteAppWidgetId(pending.id)
    }

    private suspend fun cancelPending() {
        val pending = repository.loaded.filterNotNull().first().pending ?: return
        host.deleteAppWidgetId(pending.id)
        repository.update { it.copy(pending = null) }
    }
}

sealed interface WidgetChoice {
    data object Clock : WidgetChoice

    data class Hosted(val info: AppWidgetProviderInfo) : WidgetChoice
}
