package com.bluecubelabs.env.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.bluecubelabs.env.core.data.settings.SettingsRepository
import com.bluecubelabs.env.core.model.ClockFormat
import com.bluecubelabs.env.core.model.HomeTextSize
import com.bluecubelabs.env.core.model.IconMode
import com.bluecubelabs.env.core.model.LauncherSettings
import com.bluecubelabs.env.core.model.PressFeedback
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun defaultsMatchSpecTable() = runTest {
        val file = File(tmp.root, "settings.preferences_pb")
        val repo =
            SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { file }, backgroundScope)
        val settings = repo.settings.first()
        assertThat(settings).isEqualTo(LauncherSettings())
        assertThat(settings.currentEnvId).isNull()
        assertThat(settings.iconMode).isEqualTo(IconMode.NONE)
        assertThat(settings.clockFormat).isEqualTo(ClockFormat.SYSTEM)
        assertThat(settings.showDate).isTrue()
        assertThat(settings.homeTextSize).isEqualTo(HomeTextSize.M)
        assertThat(settings.notificationCounts).isFalse()
        assertThat(settings.pressFeedback).isEqualTo(PressFeedback.FLASH)
        assertThat(settings.keyboardLayouts).containsExactly("en", "ru").inOrder()
        assertThat(settings.activeKeyboardLayout).isEqualTo("en")
        assertThat(settings.keyboardHaptics).isTrue()
        assertThat(settings.keyboardDigitHints).isTrue()
        assertThat(settings.keyboardGestureTyping).isFalse()
        assertThat(settings.searchFuzzy).isTrue()
        assertThat(settings.searchLayoutSwap).isTrue()
        assertThat(settings.searchTranslit).isTrue()
        assertThat(settings.onboardingDone).isFalse()
    }

    @Test
    fun updateRoundTrips() = runTest {
        val file = File(tmp.root, "settings.preferences_pb")
        val repo =
            SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { file }, backgroundScope)
        repo.update {
            it.copy(
                currentEnvId = 7,
                iconMode = IconMode.RIGHT,
                keyboardLayouts = listOf("ru", "en"),
                keyboardGestureTyping = true,
                notificationCounts = true,
                pressFeedback = PressFeedback.DIM,
            )
        }
        val settings = repo.settings.first { it.currentEnvId == 7L }
        assertThat(settings.iconMode).isEqualTo(IconMode.RIGHT)
        assertThat(settings.keyboardGestureTyping).isTrue()
        assertThat(settings.notificationCounts).isTrue()
        assertThat(settings.pressFeedback).isEqualTo(PressFeedback.DIM)
        assertThat(settings.keyboardLayouts).containsExactly("ru", "en").inOrder()
        repo.update { it.copy(currentEnvId = null) }
        assertThat(repo.settings.first { it.currentEnvId == null }.iconMode).isEqualTo(IconMode.RIGHT)
    }
}
