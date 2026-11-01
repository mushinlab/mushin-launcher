package com.mushinlab.launcher

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.feature.settings.EditorItem
import com.mushinlab.launcher.feature.settings.FolderSummary
import com.mushinlab.launcher.feature.settings.PickerApp
import com.mushinlab.launcher.feature.settings.SettingsRoute
import com.mushinlab.launcher.feature.settings.SettingsUiState
import com.mushinlab.launcher.feature.settings.SpaceSummary
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Test

class PickerModelTest {
    private val slack = key("slack")
    private val maps = key("maps")
    private val secret = key("secret")
    private val state = SettingsUiState(
        spaces = persistentListOf(SpaceSummary(1, "Work", SpaceType.WORK, PalettePreset.NAVY, current = true)),
        spaceItems = persistentMapOf(
            1L to persistentListOf(EditorItem(slack, "Slack", hidden = false, missing = false)),
        ),
        folders = persistentMapOf(4L to FolderSummary(4, "Banks", persistentListOf())),
        apps = persistentListOf(
            PickerApp(maps, "Maps", hidden = false),
            PickerApp(secret, "Secret", hidden = true),
            PickerApp(slack, "Slack", hidden = false),
        ),
        capacity = 2,
    )

    @Test
    fun spacePickerMarksTheHomeListHidesHiddenAppsAndKeepsTheLimit() {
        val model = pickerModel(SettingsRoute.PickApps(1), state)

        assertThat(model?.entries?.map { it.label to it.checked })
            .containsExactly("Maps" to false, "Slack" to true).inOrder()
        assertThat(model?.limit).isEqualTo(2)
        assertThat(model?.name).isEqualTo("Work")
    }

    @Test
    fun folderPickerHasNoLimitAndReplacePickerMarksTheReplacedApp() {
        assertThat(pickerModel(SettingsRoute.PickFolderApps(4), state)?.let { it.limit to it.name })
            .isEqualTo(null to "Banks")
        val replace = pickerModel(SettingsRoute.ReplaceApp(1, slack), state)
        assertThat(replace?.entries?.single { it.checked }?.key).isEqualTo(slack)
        assertThat(replace?.name).isEqualTo("Slack")
        assertThat(pickerModel(SettingsRoute.PickFolderApps(9), state)).isNull()
    }
}
