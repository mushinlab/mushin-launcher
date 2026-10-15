package com.mushinlab.launcher.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.mushinlab.launcher.core.model.GestureTuning
import com.mushinlab.launcher.core.model.HomeListOffset
import com.mushinlab.launcher.core.model.KeyboardFit
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.StrokeWidth
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope,
) {
    val loaded: StateFlow<LauncherSettings?> = dataStore.data
        .map(::decode)
        .stateIn(scope, SharingStarted.Eagerly, null)

    val settings: StateFlow<LauncherSettings> = loaded
        .map { it ?: LauncherSettings() }
        .stateIn(scope, SharingStarted.Eagerly, LauncherSettings())

    suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        dataStore.edit { prefs ->
            val next = transform(decode(prefs))
            encode(next, prefs)
        }
    }

    companion object {
        const val FILE_NAME = "settings"

        fun create(context: Context, scope: CoroutineScope): SettingsRepository = SettingsRepository(
            dataStore(scope) { context.preferencesDataStoreFile(FILE_NAME) },
            scope,
        )

        internal fun dataStore(scope: CoroutineScope, file: () -> File): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                scope = scope,
                produceFile = file,
            )

        internal val CURRENT_ENV_ID = longPreferencesKey("current_env_id")
        internal val ICON_MODE = stringPreferencesKey("icon_mode")
        internal val CLOCK_FORMAT = stringPreferencesKey("clock_format")
        internal val SHOW_DATE = booleanPreferencesKey("show_date")
        internal val CLOCK_POSITION = stringPreferencesKey("clock_position")
        internal val FONT = stringPreferencesKey("font")
        internal val FONT_WEIGHT = stringPreferencesKey("font_weight")
        internal val HOME_TEXT_SIZE = stringPreferencesKey("home_text_size")
        internal val STROKE_WIDTH = intPreferencesKey("stroke_width")
        internal val STROKE_COLOR = stringPreferencesKey("stroke_color")
        internal val STROKE_DOTS = booleanPreferencesKey("stroke_dots")
        internal val BACKGROUND_OPACITY = floatPreferencesKey("background_opacity")
        internal val NOTIFICATION_COUNTS = booleanPreferencesKey("notification_counts")
        internal val PRESS_FEEDBACK = stringPreferencesKey("press_feedback")
        internal val KB_LAYOUTS = stringPreferencesKey("kb_layouts")
        internal val KB_ACTIVE_LAYOUT = stringPreferencesKey("kb_active_layout")
        internal val KB_HAPTICS = booleanPreferencesKey("kb_haptics")
        internal val KB_DIGIT_HINTS = booleanPreferencesKey("kb_digit_hints")
        internal val KB_WIDTH = floatPreferencesKey("kb_width")
        internal val KB_SHIFT = floatPreferencesKey("kb_shift")
        internal val HOME_LIST_OFFSET = intPreferencesKey("home_list_offset")
        internal val SEARCH_COLORED_ICONS = booleanPreferencesKey("search_colored_icons")
        internal val GESTURE_SHIFT_PENALTY = floatPreferencesKey("gesture_shift_penalty")
        internal val GESTURE_SUGGEST_MARGIN = floatPreferencesKey("gesture_suggest_margin")
        internal val SEARCH_FUZZY = booleanPreferencesKey("search_fuzzy")
        internal val SEARCH_LAYOUT_SWAP = booleanPreferencesKey("search_layout_swap")
        internal val SEARCH_TRANSLIT = booleanPreferencesKey("search_translit")
        internal val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

        internal fun decode(prefs: Preferences): LauncherSettings {
            val defaults = LauncherSettings()
            return LauncherSettings(
                currentEnvId = prefs[CURRENT_ENV_ID],
                iconMode = enumOr(prefs[ICON_MODE], defaults.iconMode),
                clockFormat = enumOr(prefs[CLOCK_FORMAT], defaults.clockFormat),
                showDate = prefs[SHOW_DATE] ?: defaults.showDate,
                clockPosition = enumOr(prefs[CLOCK_POSITION], defaults.clockPosition),
                font = enumOr(prefs[FONT], defaults.font),
                fontWeight = enumOr(prefs[FONT_WEIGHT], defaults.fontWeight),
                homeTextSize = enumOr(prefs[HOME_TEXT_SIZE], defaults.homeTextSize),
                strokeWidth = (prefs[STROKE_WIDTH] ?: defaults.strokeWidth)
                    .coerceIn(StrokeWidth.MIN_DP, StrokeWidth.MAX_DP),
                strokeColor = enumOr(prefs[STROKE_COLOR], defaults.strokeColor),
                strokeDots = prefs[STROKE_DOTS] ?: defaults.strokeDots,
                backgroundOpacity = (prefs[BACKGROUND_OPACITY] ?: defaults.backgroundOpacity).coerceIn(0f, 1f),
                notificationCounts = prefs[NOTIFICATION_COUNTS] ?: defaults.notificationCounts,
                pressFeedback = enumOr(prefs[PRESS_FEEDBACK], defaults.pressFeedback),
                keyboardLayouts = prefs[KB_LAYOUTS]
                    ?.split(',')
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.takeIf { it.isNotEmpty() }
                    ?: defaults.keyboardLayouts,
                activeKeyboardLayout = prefs[KB_ACTIVE_LAYOUT] ?: defaults.activeKeyboardLayout,
                keyboardHaptics = prefs[KB_HAPTICS] ?: defaults.keyboardHaptics,
                keyboardDigitHints = prefs[KB_DIGIT_HINTS] ?: defaults.keyboardDigitHints,
                keyboardWidth = (prefs[KB_WIDTH] ?: defaults.keyboardWidth)
                    .coerceIn(KeyboardFit.WIDTH_MIN, KeyboardFit.WIDTH_MAX),
                keyboardShift = (prefs[KB_SHIFT] ?: defaults.keyboardShift)
                    .coerceIn(KeyboardFit.SHIFT_MIN, KeyboardFit.SHIFT_MAX),
                homeListOffset = (prefs[HOME_LIST_OFFSET] ?: defaults.homeListOffset)
                    .coerceIn(0, HomeListOffset.MAX_DP),
                searchColoredIcons = prefs[SEARCH_COLORED_ICONS] ?: defaults.searchColoredIcons,
                gestureShiftPenalty = (prefs[GESTURE_SHIFT_PENALTY] ?: defaults.gestureShiftPenalty)
                    .coerceIn(GestureTuning.SHIFT_PENALTY_MIN, GestureTuning.SHIFT_PENALTY_MAX),
                gestureSuggestMargin = (prefs[GESTURE_SUGGEST_MARGIN] ?: defaults.gestureSuggestMargin)
                    .coerceIn(GestureTuning.SUGGEST_MARGIN_MIN, GestureTuning.SUGGEST_MARGIN_MAX),
                searchFuzzy = prefs[SEARCH_FUZZY] ?: defaults.searchFuzzy,
                searchLayoutSwap = prefs[SEARCH_LAYOUT_SWAP] ?: defaults.searchLayoutSwap,
                searchTranslit = prefs[SEARCH_TRANSLIT] ?: defaults.searchTranslit,
                onboardingDone = prefs[ONBOARDING_DONE] ?: defaults.onboardingDone,
            )
        }

        private fun encode(settings: LauncherSettings, prefs: MutablePreferences) {
            val envId = settings.currentEnvId
            if (envId == null) prefs.remove(CURRENT_ENV_ID) else prefs[CURRENT_ENV_ID] = envId
            prefs[ICON_MODE] = settings.iconMode.name
            prefs[CLOCK_FORMAT] = settings.clockFormat.name
            prefs[SHOW_DATE] = settings.showDate
            prefs[CLOCK_POSITION] = settings.clockPosition.name
            prefs[FONT] = settings.font.name
            prefs[FONT_WEIGHT] = settings.fontWeight.name
            prefs[HOME_TEXT_SIZE] = settings.homeTextSize.name
            prefs[STROKE_WIDTH] = settings.strokeWidth
            prefs[STROKE_COLOR] = settings.strokeColor.name
            prefs[STROKE_DOTS] = settings.strokeDots
            prefs[BACKGROUND_OPACITY] = settings.backgroundOpacity
            prefs[NOTIFICATION_COUNTS] = settings.notificationCounts
            prefs[PRESS_FEEDBACK] = settings.pressFeedback.name
            prefs[KB_LAYOUTS] = settings.keyboardLayouts.joinToString(",")
            prefs[KB_ACTIVE_LAYOUT] = settings.activeKeyboardLayout
            prefs[KB_HAPTICS] = settings.keyboardHaptics
            prefs[KB_DIGIT_HINTS] = settings.keyboardDigitHints
            prefs[KB_WIDTH] = settings.keyboardWidth
            prefs[KB_SHIFT] = settings.keyboardShift
            prefs[HOME_LIST_OFFSET] = settings.homeListOffset
            prefs[SEARCH_COLORED_ICONS] = settings.searchColoredIcons
            prefs[GESTURE_SHIFT_PENALTY] = settings.gestureShiftPenalty
            prefs[GESTURE_SUGGEST_MARGIN] = settings.gestureSuggestMargin
            prefs[SEARCH_FUZZY] = settings.searchFuzzy
            prefs[SEARCH_LAYOUT_SWAP] = settings.searchLayoutSwap
            prefs[SEARCH_TRANSLIT] = settings.searchTranslit
            prefs[ONBOARDING_DONE] = settings.onboardingDone
        }

        private inline fun <reified E : Enum<E>> enumOr(raw: String?, default: E): E =
            raw?.let { value -> enumValues<E>().firstOrNull { it.name == value } } ?: default
    }
}
