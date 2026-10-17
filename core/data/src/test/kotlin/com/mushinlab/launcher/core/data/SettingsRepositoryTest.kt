package com.mushinlab.launcher.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
        assertThat(settings.currentSpaceId).isNull()
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
        assertThat(settings.homeListOffset).isNull()
        assertThat(settings.searchColoredIcons).isFalse()
        assertThat(settings.gestureShiftPenalty).isEqualTo(0.5f)
        assertThat(settings.gestureSuggestMargin).isEqualTo(0.3f)
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
                currentSpaceId = 7,
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
            )
        }
        val settings = repo.settings.first { it.currentSpaceId == 7L }
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
        repo.update { it.copy(currentSpaceId = null) }
        assertThat(repo.settings.first { it.currentSpaceId == null }.iconMode).isEqualTo(IconMode.RIGHT)
    }

    @Test
    fun homeListOffsetStaysAutoUntilSavedAndNullRemovesTheKey() = runTest {
        val file = File(tmp.root, "settings.preferences_pb")
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        val repo = SettingsRepository(store, backgroundScope)
        val key = intPreferencesKey("home_list_offset")
        repo.update { it.copy(iconMode = IconMode.LEFT) }
        assertThat(repo.settings.first { it.iconMode == IconMode.LEFT }.homeListOffset).isNull()
        assertThat(store.data.first().contains(key)).isFalse()
        repo.update { it.copy(homeListOffset = 0) }
        assertThat(repo.settings.first { it.homeListOffset != null }.homeListOffset).isEqualTo(0)
        assertThat(store.data.first()[key]).isEqualTo(0)
        repo.update { it.copy(homeListOffset = null) }
        assertThat(repo.settings.first { it.homeListOffset == null }.iconMode).isEqualTo(IconMode.LEFT)
        assertThat(store.data.first().contains(key)).isFalse()
    }

    @Test
    fun previouslySavedHomeListOffsetIsKept() = runTest {
        val file = File(tmp.root, "settings.preferences_pb")
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        store.edit { it[intPreferencesKey("home_list_offset")] = 120 }
        val repo = SettingsRepository(store, backgroundScope)
        assertThat(repo.settings.first { it.homeListOffset != null }.homeListOffset).isEqualTo(120)
    }

    @Test
    fun legacyBackgroundVisibilityIsNoLongerWrittenAndCanBeReadOnceAndDropped() = runTest {
        val file = File(tmp.root, "settings.preferences_pb")
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        val key = floatPreferencesKey("background_opacity")
        val repo = SettingsRepository(store, backgroundScope)
        repo.update { it.copy(iconMode = IconMode.LEFT) }
        assertThat(store.data.first().contains(key)).isFalse()
        assertThat(repo.legacyBackgroundOpacity()).isNull()
        store.edit { it[key] = 0.3f }
        repo.update { it.copy(iconMode = IconMode.RIGHT) }
        assertThat(repo.legacyBackgroundOpacity()).isEqualTo(0.3f)
        repo.dropLegacyBackgroundOpacity()
        assertThat(repo.legacyBackgroundOpacity()).isNull()
        assertThat(repo.settings.first { it.iconMode == IconMode.RIGHT }.iconMode).isEqualTo(IconMode.RIGHT)
    }
}
