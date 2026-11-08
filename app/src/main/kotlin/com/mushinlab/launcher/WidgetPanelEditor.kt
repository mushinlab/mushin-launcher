package com.mushinlab.launcher

import com.mushinlab.launcher.core.data.widgets.WidgetRepository
import com.mushinlab.launcher.core.model.HostedWidget
import com.mushinlab.launcher.core.model.WidgetArrangement
import com.mushinlab.launcher.core.model.WidgetLayout
import com.mushinlab.launcher.core.model.WidgetPanelMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WidgetPanelEditor(
    private val repository: WidgetRepository,
    private val scope: CoroutineScope,
    private val currentSpace: () -> Long?,
    private val host: MushinWidgetHost,
) {
    fun resize(mode: WidgetPanelMode, arrangement: WidgetArrangement) {
        val id = currentSpace() ?: return
        scope.launch {
            change(id) {
                if (it.widgets.size <= mode.capacity) it.copy(mode = mode, arrangement = arrangement) else it
            }
        }
    }

    fun move(id: Int, index: Int) {
        val space = currentSpace() ?: return
        scope.launch {
            change(space) { layout ->
                val items = layout.widgets.toMutableList()
                val from = items.indexOfFirst { it.id == id }
                if (from >= 0 && index in items.indices) {
                    items.add(index, items.removeAt(from))
                }
                layout.copy(widgets = items)
            }
        }
    }

    fun append(spaceId: Long, widget: HostedWidget) {
        scope.launch {
            change(spaceId) { layout ->
                if (layout.widgets.size < layout.mode.capacity && layout.widgets.none { it.id == widget.id }) {
                    layout.copy(widgets = layout.widgets + widget)
                } else {
                    layout
                }
            }
        }
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
