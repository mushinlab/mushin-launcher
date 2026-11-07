package com.mushinlab.launcher

import android.appwidget.AppWidgetManager
import android.content.Context
import com.mushinlab.launcher.core.data.settings.SettingsRepository
import com.mushinlab.launcher.core.data.widgets.WidgetRepository
import com.mushinlab.launcher.core.model.HostedWidget
import com.mushinlab.launcher.core.model.PendingWidget
import com.mushinlab.launcher.core.model.WidgetLayout
import com.mushinlab.launcher.core.model.WidgetPanelPosition
import com.mushinlab.launcher.core.model.WidgetPanelSize
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
    private val settings = container.settings
    private val host = container.widgetHost
    private var choosing = false
    private val manager = AppWidgetManager.getInstance(context)
    private val initialized = scope.async {
        val state = repository.loaded.filterNotNull().first()
        val retained =
            state.layouts.values.flatMap { it.widgets }.map { it.id }.toSet() + listOfNotNull(state.pending?.id)
        host.appWidgetIds.filterNot(retained::contains).forEach(host::deleteAppWidgetId)
    }

    fun add(panel: WidgetPanelPosition) {
        val spaceId = currentSpace() ?: return
        if (choosing) return
        choosing = true
        scope.launch {
            initialized.await()
            cancelPending()
            val id = host.allocateAppWidgetId()
            repository.update { it.copy(pending = PendingWidget(spaceId, panel, id)) }
            emit(LauncherEffect.PickWidget(id))
        }
    }

    fun onPicked(success: Boolean, clock: Boolean) = scope.launch {
        initialized.await()
        val pending = repository.loaded.filterNotNull().first().pending ?: return@launch
        when {
            !success -> {
                cancelPending()
                choosing = false
            }

            clock -> {
                change(pending.spaceId) { it.copy(clockPanel = pending.panel) }
                settings.update { it.copy(showClock = true) }
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

    fun resize(panel: WidgetPanelPosition, size: WidgetPanelSize) {
        val id = currentSpace() ?: return
        scope.launch {
            change(id) {
                if (panel == WidgetPanelPosition.TOP) it.copy(topSize = size) else it.copy(bottomSize = size)
            }
        }
    }

    fun remove(id: Int) {
        val space = currentSpace() ?: return
        scope.launch {
            change(space) { layout ->
                if (id == CLOCK_ID) {
                    layout.copy(clockPanel = null)
                } else {
                    layout.copy(widgets = layout.widgets.filterNot { it.id == id })
                }
            }
            if (id != CLOCK_ID) host.deleteAppWidgetId(id)
        }
    }

    suspend fun refresh(ids: Set<Long> = container.spaces.spaces.value.map { it.id }.toSet()) {
        initialized.await()
        if (ids.isEmpty()) return
        val state = repository.loaded.filterNotNull().first()
        val removed = state.layouts.flatMap { (space, layout) ->
            layout.widgets.filter { space !in ids || manager.getAppWidgetInfo(it.id) == null }
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
        val widget = HostedWidget(pending.id, pending.panel, info.loadLabel(context.packageManager))
        repository.update { state ->
            val layout = state.layouts[pending.spaceId] ?: WidgetLayout()
            state.copy(
                layouts = state.layouts + (pending.spaceId to layout.copy(widgets = layout.widgets + widget)),
                pending = null,
            )
        }
    }

    private suspend fun change(spaceId: Long, transform: (WidgetLayout) -> WidgetLayout) {
        repository.update {
            it.copy(layouts = it.layouts + (spaceId to transform(it.layouts[spaceId] ?: WidgetLayout())))
        }
    }

    private suspend fun cancelPending() {
        val pending = repository.loaded.filterNotNull().first().pending ?: return
        host.deleteAppWidgetId(pending.id)
        repository.update { it.copy(pending = null) }
    }

    companion object {
        const val CLOCK_ID = -1
    }
}
