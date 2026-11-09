package com.mushinlab.launcher

import com.mushinlab.launcher.core.data.widgets.WidgetRepository
import com.mushinlab.launcher.core.model.HostedWidget
import com.mushinlab.launcher.core.model.WidgetCell
import com.mushinlab.launcher.core.model.WidgetLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WidgetPanelEditor(
    private val repository: WidgetRepository,
    private val scope: CoroutineScope,
    private val currentSpace: () -> Long?,
    private val host: MushinWidgetHost,
) {
    fun place(id: Int, cell: WidgetCell) {
        val space = currentSpace() ?: return
        scope.launch { change(space) { it.place(id, cell) ?: it } }
    }

    fun append(spaceId: Long, widget: HostedWidget, cell: WidgetCell?) {
        scope.launch { change(spaceId) { it.add(widget, cell) ?: it } }
    }

    fun remove(id: Int) {
        val space = currentSpace() ?: return
        scope.launch {
            change(space) { layout -> layout.copy(widgets = layout.widgets.filterNot { it.id == id }) }
            if (id != HostedWidget.CLOCK_ID) host.deleteAppWidgetId(id)
        }
    }

    suspend fun removeClocks() {
        repository.update { state ->
            state.copy(
                layouts = state.layouts.mapValues { (_, layout) ->
                    layout.copy(widgets = layout.widgets.filterNot { it.isClock })
                },
            )
        }
    }

    private suspend fun change(spaceId: Long, transform: (WidgetLayout) -> WidgetLayout) {
        repository.update {
            it.copy(layouts = it.layouts + (spaceId to transform(it.layouts[spaceId] ?: WidgetLayout())))
        }
    }
}
