package com.mushinlab.launcher.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.settings.SettingsRepository
import com.mushinlab.launcher.core.model.AppFont
import com.mushinlab.launcher.core.model.AppFontWeight
import com.mushinlab.launcher.core.model.ClockFormat
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.HomeTextSize
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.PressFeedback
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
    fun corruptedFileFallsBackToDefaultsInsteadOfCrashing() = runTest {
        val file = File(tmp.root, "settings.preferences_pb").apply { writeBytes(byteArrayOf(-1, -2, -3, 7, 0, 42)) }
        val repo = SettingsRepository(SettingsRepository.dataStore(backgroundScope) { file }, backgroundScope)
        assertThat(repo.settings.first()).isEqualTo(LauncherSettings())
    }

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
        assertThat(settings.clockPosition).isEqualTo(ClockPosition.START)
        assertThat(settings.font).isEqualTo(AppFont.SYSTEM)
        assertThat(settings.fontWeight).isEqualTo(AppFontWeight.REGULAR)
        assertThat(settings.homeTextSize).isEqualTo(HomeTextSize.M)
        assertThat(settings.notificationCounts).isFalse()
        assertThat(settings.pressFeedback).isEqualTo(PressFeedback.FLASH)
        assertThat(settings.keyboardLayouts).containsExactly("en", "ru").inOrder()
        assertThat(settings.activeKeyboardLayout).isEqualTo("en")
        assertThat(settings.keyboardHaptics).isTrue()
        assertThat(settings.keyboardDigitHints).isTrue()
        assertThat(settings.keyboardWidth).isEqualTo(1f)
        assertThat(settings.keyboardShift).isEqualTo(0f)
        assertThat(settings.homeListOffset).isEqualTo(0)
        assertThat(settings.searchColoredIcons).isFalse()
        assertThat(settings.gestureShiftPenalty).isEqualTo(0.5f)
        assertThat(settings.gestureSuggestMargin).isEqualTo(0.3f)
        assertThat(settings.searchFuzzy).isTrue()
        assertThat(settings.searchLayoutSwap).isTrue()
        assertThat(settings.searchTranslit).isTrue()
        assertThat(settings.onboardingDone).isFalse()
        assertThat(settings.backgroundOpacity).isEqualTo(0.5f)
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
                keyboardWidth = 0.1f,
                keyboardShift = -0.5f,
                homeListOffset = 10_000,
                searchColoredIcons = true,
                notificationCounts = true,
                pressFeedback = PressFeedback.DIM,
                gestureShiftPenalty = 0.8f,
                gestureSuggestMargin = 5f,
                clockPosition = ClockPosition.END,
                font = AppFont.MONTSERRAT,
                fontWeight = AppFontWeight.LIGHT,
                backgroundOpacity = 1.7f,
            )
        }
        val settings = repo.settings.first { it.currentEnvId == 7L }
        assertThat(settings.iconMode).isEqualTo(IconMode.RIGHT)
        assertThat(settings.keyboardWidth).isEqualTo(0.6f)
        assertThat(settings.keyboardShift).isEqualTo(-0.5f)
        assertThat(settings.homeListOffset).isEqualTo(480)
        assertThat(settings.searchColoredIcons).isTrue()
        assertThat(settings.notificationCounts).isTrue()
        assertThat(settings.pressFeedback).isEqualTo(PressFeedback.DIM)
        assertThat(settings.gestureShiftPenalty).isEqualTo(0.8f)
        assertThat(settings.gestureSuggestMargin).isEqualTo(1f)
        assertThat(settings.keyboardLayouts).containsExactly("ru", "en").inOrder()
        assertThat(settings.clockPosition).isEqualTo(ClockPosition.END)
        assertThat(settings.font).isEqualTo(AppFont.MONTSERRAT)
        assertThat(settings.fontWeight).isEqualTo(AppFontWeight.LIGHT)
        assertThat(settings.backgroundOpacity).isEqualTo(1f)
        repo.update { it.copy(currentEnvId = null) }
        assertThat(repo.settings.first { it.currentEnvId == null }.iconMode).isEqualTo(IconMode.RIGHT)
    }
}
