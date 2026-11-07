package com.mushinlab.launcher.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.widgets.WidgetRepository
import com.mushinlab.launcher.core.model.HostedWidget
import com.mushinlab.launcher.core.model.PendingWidget
import com.mushinlab.launcher.core.model.WidgetLayout
import com.mushinlab.launcher.core.model.WidgetPanelPosition
import com.mushinlab.launcher.core.model.WidgetPanelSize
import com.mushinlab.launcher.core.model.WidgetState
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WidgetRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun AC_38_1_defaultLayoutHasOnlyTimeAndAnEmptyBottomPanel() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tmp.root, "widgets.preferences_pb")
        }
        val repository = WidgetRepository(store, backgroundScope)
        assertThat(repository.loaded.filterNotNull().first()).isEqualTo(WidgetState())
        assertThat(WidgetLayout().clockPanel).isEqualTo(WidgetPanelPosition.TOP)
        assertThat(WidgetLayout().widgets).isEmpty()
    }

    @Test
    fun AC_38_4_layoutsAndPendingSystemRequestSurviveReopeningTheStore() = runTest {
        val file = File(tmp.root, "widgets.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val store = PreferenceDataStoreFactory.create(scope = firstScope) { file }
        val repo = WidgetRepository(store, firstScope)
        val layout = WidgetLayout(
            clockPanel = null,
            topSize = WidgetPanelSize.SMALL,
            bottomSize = WidgetPanelSize.LARGE,
            widgets = listOf(HostedWidget(12, WidgetPanelPosition.BOTTOM, "Calendar")),
        )
        val pending = PendingWidget(2, WidgetPanelPosition.TOP, 13)
        try {
            repo.update { it.copy(layouts = mapOf(1L to layout), pending = pending) }
            repo.update { it.copy(layouts = it.layouts + (2L to WidgetLayout())) }
        } finally {
            firstScope.cancel()
            firstScope.coroutineContext[Job]?.join()
        }
        val reopened =
            WidgetRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { file }, backgroundScope)
        val state = reopened.loaded.filterNotNull().first()
        assertThat(state.layouts[1]).isEqualTo(layout)
        assertThat(state.layouts[2]).isEqualTo(WidgetLayout())
        assertThat(state.pending).isEqualTo(pending)
        reopened.update { it.copy(pending = null) }
        assertThat(reopened.loaded.filterNotNull().first { it.pending == null }.layouts[1]).isEqualTo(layout)
    }

    @Test
    fun corruptWidgetPayloadFallsBackWithoutAffectingOtherPreferences() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tmp.root, "widgets.preferences_pb")
        }
        store.edit { it[stringPreferencesKey("widget_state")] = "invalid widget data" }
        val repository = WidgetRepository(store, backgroundScope)
        assertThat(repository.loaded.filterNotNull().first()).isEqualTo(WidgetState())
    }
}
