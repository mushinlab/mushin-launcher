package com.mushinlab.launcher.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.widgets.WidgetRepository
import com.mushinlab.launcher.core.model.HostedWidget
import com.mushinlab.launcher.core.model.PendingWidget
import com.mushinlab.launcher.core.model.WidgetCell
import com.mushinlab.launcher.core.model.WidgetLayout
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
    fun AC_39_1_defaultLayoutHasTheClockInTheTopLeftCell() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tmp.root, "widgets.preferences_pb")
        }
        val repository = WidgetRepository(store, backgroundScope)
        assertThat(repository.loaded.filterNotNull().first()).isEqualTo(WidgetState())
        assertThat(WidgetLayout().widgets).containsExactly(HostedWidget.clock)
        assertThat(HostedWidget.clock.cell).isEqualTo(WidgetCell(0, 0))
    }

    @Test
    fun AC_39_4_layoutsAndPendingSystemRequestSurviveReopeningTheStore() = runTest {
        val file = File(tmp.root, "widgets.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val store = PreferenceDataStoreFactory.create(scope = firstScope) { file }
        val repo = WidgetRepository(store, firstScope)
        val layout = WidgetLayout(
            listOf(
                HostedWidget(12, "Calendar", WidgetCell(0, 0, 1, 2)),
                HostedWidget.clock.copy(cell = WidgetCell(1, 1)),
            ),
        )
        val pending = PendingWidget(2, 13, WidgetCell(1, 0))
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
    fun AC_39_4_legacyPanelsFillTheGridRowByRowDroppingTheClockBeforeHostedWidgets() = runTest {
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
        assertThat(layout.widgets.map { it.id }).containsExactly(10, 11, 12, 13).inOrder()
        assertThat(layout.widgets.map { it.cell }).containsExactlyElementsIn(WidgetCell.singles).inOrder()
        assertThat(migrated.pending?.id).isEqualTo(20)
        repository.update { it }
        assertThat(repository.loaded.filterNotNull().first()).isEqualTo(migrated)
    }

    @Test
    fun AC_39_4_versionTwoClockBecomesTheFirstSlotAndAColumnStaysAColumn() = runTest {
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
        assertThat(layouts.getValue(1).widgets.map { it.cell })
            .containsExactly(WidgetCell(0, 0), WidgetCell(1, 0), WidgetCell(0, 1)).inOrder()
        assertThat(layouts.getValue(2).widgets.map { it.id }).containsExactly(30, 31).inOrder()
        assertThat(
            layouts.getValue(2).widgets.map {
                it.cell
            },
        ).containsExactly(WidgetCell(0, 0), WidgetCell(0, 1)).inOrder()
    }

    @Test
    fun AC_39_4_versionThreePanelsBecomeGridCells() = runTest {
        val bytes = java.io.ByteArrayOutputStream()
        java.io.DataOutputStream(bytes).use { out ->
            out.writeInt(3)
            out.writeInt(1)
            out.writeLong(4)
            out.writeUTF("STANDARD")
            out.writeUTF("ROW")
            out.writeInt(2)
            out.writeInt(HostedWidget.CLOCK_ID)
            out.writeUTF("")
            out.writeInt(40)
            out.writeUTF("Calendar")
            out.writeBoolean(true)
            out.writeLong(4)
            out.writeInt(41)
        }
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tmp.root, "v3.preferences_pb")
        }
        store.edit {
            it[stringPreferencesKey("widget_state")] = java.util.Base64.getEncoder().encodeToString(bytes.toByteArray())
        }
        val state = WidgetRepository(store, backgroundScope).loaded.filterNotNull().first()
        assertThat(state.layouts.getValue(4).widgets)
            .containsExactly(HostedWidget.clock, HostedWidget(40, "Calendar", WidgetCell(1, 0))).inOrder()
        assertThat(state.pending).isEqualTo(PendingWidget(4, 41))
    }

    @Test
    fun legacyPanelWithARepeatedIdIsTreatedAsCorrupt() = runTest {
        val bytes = java.io.ByteArrayOutputStream()
        java.io.DataOutputStream(bytes).use { out ->
            out.writeInt(3)
            out.writeInt(1)
            out.writeLong(4)
            out.writeUTF("STANDARD")
            out.writeUTF("ROW")
            out.writeInt(2)
            repeat(2) {
                out.writeInt(40)
                out.writeUTF("Calendar")
            }
            out.writeBoolean(false)
        }
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tmp.root, "duplicate.preferences_pb")
        }
        store.edit {
            it[stringPreferencesKey("widget_state")] = java.util.Base64.getEncoder().encodeToString(bytes.toByteArray())
        }
        val state = WidgetRepository(store, backgroundScope).loaded.filterNotNull().first()
        assertThat(state).isEqualTo(WidgetState())
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
