package com.mushinlab.launcher.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.widgets.WidgetRepository
import com.mushinlab.launcher.core.model.HostedWidget
import com.mushinlab.launcher.core.model.PendingWidget
import com.mushinlab.launcher.core.model.WidgetLayout
import com.mushinlab.launcher.core.model.WidgetPanelMode
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
    fun AC_39_1_defaultLayoutIsAStandardPanelWithTheClockInTheFirstSlot() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tmp.root, "widgets.preferences_pb")
        }
        val repository = WidgetRepository(store, backgroundScope)
        assertThat(repository.loaded.filterNotNull().first()).isEqualTo(WidgetState())
        assertThat(WidgetLayout().mode).isEqualTo(WidgetPanelMode.STANDARD)
        assertThat(WidgetLayout().widgets).containsExactly(HostedWidget.clock)
    }

    @Test
    fun AC_39_4_layoutsAndPendingSystemRequestSurviveReopeningTheStore() = runTest {
        val file = File(tmp.root, "widgets.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val store = PreferenceDataStoreFactory.create(scope = firstScope) { file }
        val repo = WidgetRepository(store, firstScope)
        val layout = WidgetLayout(
            arrangement = com.mushinlab.launcher.core.model.WidgetArrangement.COLUMN,
            mode = WidgetPanelMode.EXPANDED,
            widgets = listOf(HostedWidget(12, "Calendar"), HostedWidget.clock),
        )
        val pending = PendingWidget(2, 13)
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
    fun AC_39_4_legacyPanelsMigrateWithoutDroppingOverflowOrPendingIds() = runTest {
        val bytes = java.io.ByteArrayOutputStream()
        java.io.DataOutputStream(bytes).use { out ->
            out.writeInt(1)
            out.writeInt(1)
            out.writeLong(7)
            out.writeUTF("BOTTOM")
            out.writeUTF("LARGE")
            out.writeUTF("MEDIUM")
            out.writeInt(5)
            repeat(5) { index ->
                out.writeInt(index + 10)
                out.writeUTF(if (index < 2) "TOP" else "BOTTOM")
                out.writeUTF("Widget $index")
            }
            out.writeBoolean(true)
            out.writeLong(7)
            out.writeUTF("BOTTOM")
            out.writeInt(20)
        }
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tmp.root, "legacy.preferences_pb")
        }
        store.edit {
            it[stringPreferencesKey("widget_state")] = java.util.Base64.getEncoder().encodeToString(bytes.toByteArray())
        }
        val repository = WidgetRepository(store, backgroundScope)
        val migrated = repository.loaded.filterNotNull().first()
        val layout = requireNotNull(migrated.layouts[7])
        assertThat(layout.mode).isEqualTo(WidgetPanelMode.EXPANDED)
        assertThat(layout.widgets.map { it.id }).containsExactly(HostedWidget.CLOCK_ID, 10, 11, 12, 13, 14).inOrder()
        assertThat(migrated.pending?.id).isEqualTo(20)
        repository.update { it }
        assertThat(repository.loaded.filterNotNull().first()).isEqualTo(migrated)
    }

    @Test
    fun AC_39_4_versionTwoClockBecomesTheFirstSlotAndWidensAFullStandardPanel() = runTest {
        fun layout(out: java.io.DataOutputStream, space: Long, clock: String) {
            out.writeLong(space)
            out.writeUTF(clock)
            out.writeUTF("STANDARD")
            out.writeUTF("COLUMN")
            out.writeInt(2)
            repeat(2) { index ->
                out.writeInt(index + 30)
                out.writeUTF("TOP")
                out.writeUTF("Widget $index")
            }
        }
        val bytes = java.io.ByteArrayOutputStream()
        java.io.DataOutputStream(bytes).use { out ->
            out.writeInt(2)
            out.writeInt(2)
            layout(out, 1, "TOP")
            layout(out, 2, "")
            out.writeBoolean(false)
        }
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tmp.root, "v2.preferences_pb")
        }
        store.edit {
            it[stringPreferencesKey("widget_state")] = java.util.Base64.getEncoder().encodeToString(bytes.toByteArray())
        }
        val layouts = WidgetRepository(store, backgroundScope).loaded.filterNotNull().first().layouts
        assertThat(layouts.getValue(1).widgets.map { it.id }).containsExactly(HostedWidget.CLOCK_ID, 30, 31).inOrder()
        assertThat(layouts.getValue(1).mode).isEqualTo(WidgetPanelMode.EXPANDED)
        assertThat(layouts.getValue(2).widgets.map { it.id }).containsExactly(30, 31).inOrder()
        assertThat(layouts.getValue(2).mode).isEqualTo(WidgetPanelMode.STANDARD)
        assertThat(
            layouts.getValue(2).arrangement,
        ).isEqualTo(com.mushinlab.launcher.core.model.WidgetArrangement.COLUMN)
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
