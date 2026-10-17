package com.mushinlab.launcher.core.data.settings

import android.content.Context
import android.content.res.Resources
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
import com.mushinlab.launcher.core.model.KeyboardLanguages
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.StrokeWidth
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Global [LauncherSettings] in a Preferences DataStore. Keys keep their historical names (`current_env_id`).
 *
 * Unknown enum values and out-of-range numbers fall back to defaults or are clamped on read; a corrupt file
 * is replaced with defaults. [loaded] is null until the first read; [settings] substitutes defaults meanwhile.
 * [defaultLayouts] are the keyboard layouts while none are stored; the active layout then defaults to the first.
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope,
    private val defaultLayouts: List<String> = LauncherSettings().keyboardLayouts,
) {
    val loaded: StateFlow<LauncherSettings?> = dataStore.data
        .map { decode(it, defaultLayouts) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val settings: StateFlow<LauncherSettings> = loaded
        .map { it ?: LauncherSettings() }
        .stateIn(scope, SharingStarted.Eagerly, LauncherSettings())

    /** Atomically applies [transform] to the current settings and stores the result. */
    suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        dataStore.edit { prefs ->
            val next = transform(decode(prefs, defaultLayouts))
            encode(next, prefs)
        }
    }

    /** The global background visibility of versions before ADR-0020, or null when it is not stored. */
    suspend fun legacyBackgroundOpacity(): Float? = dataStore.data.first()[LEGACY_BACKGROUND_OPACITY]

    /** Removes the global background visibility of versions before ADR-0020. */
    suspend fun dropLegacyBackgroundOpacity() {
        dataStore.edit { it.remove(LEGACY_BACKGROUND_OPACITY) }
    }

    companion object {
        const val FILE_NAME = "settings"

        /**
         * Creates the repository backed by the app's `settings` DataStore file. Keyboard layouts default to the
         * layout of the device language plus English ([KeyboardLanguages.defaultsFor]).
         */
        fun create(context: Context, scope: CoroutineScope): SettingsRepository = SettingsRepository(
            dataStore(scope) { context.preferencesDataStoreFile(FILE_NAME) },
            scope,
            KeyboardLanguages.defaultsFor(Resources.getSystem().configuration.locales[0].language),
        )

        internal fun dataStore(scope: CoroutineScope, file: () -> File): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                migrations = listOf(ListOffsetAutoMigration),
                scope = scope,
                produceFile = file,
            )

        internal val CURRENT_SPACE_ID = longPreferencesKey("current_env_id")
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
        internal val LEGACY_BACKGROUND_OPACITY = floatPreferencesKey("background_opacity")
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

        internal fun decode(prefs: Preferences, defaultLayouts: List<String>): LauncherSettings {
            val defaults = LauncherSettings()
            val layouts = decodeLayouts(prefs[KB_LAYOUTS]) ?: defaultLayouts
            return LauncherSettings(
                currentSpaceId = prefs[CURRENT_SPACE_ID],
                iconMode = enumOr(prefs[ICON_MODE], defaults.iconMode),
                clockFormat = enumOr(prefs[CLOCK_FORMAT], defaults.clockFormat),
                showDate = prefs.valueOr(SHOW_DATE, defaults.showDate),
                clockPosition = enumOr(prefs[CLOCK_POSITION], defaults.clockPosition),
                font = enumOr(prefs[FONT], defaults.font),
                fontWeight = enumOr(prefs[FONT_WEIGHT], defaults.fontWeight),
                homeTextSize = enumOr(prefs[HOME_TEXT_SIZE], defaults.homeTextSize),
                strokeWidth = prefs.valueOr(STROKE_WIDTH, defaults.strokeWidth)
                    .coerceIn(StrokeWidth.MIN_DP, StrokeWidth.MAX_DP),
                strokeColor = enumOr(prefs[STROKE_COLOR], defaults.strokeColor),
                strokeDots = prefs.valueOr(STROKE_DOTS, defaults.strokeDots),
                notificationCounts = prefs.valueOr(NOTIFICATION_COUNTS, defaults.notificationCounts),
                pressFeedback = enumOr(prefs[PRESS_FEEDBACK], defaults.pressFeedback),
                keyboardLayouts = layouts,
                activeKeyboardLayout = prefs.valueOr(KB_ACTIVE_LAYOUT, layouts.first()),
                keyboardHaptics = prefs.valueOr(KB_HAPTICS, defaults.keyboardHaptics),
                keyboardDigitHints = prefs.valueOr(KB_DIGIT_HINTS, defaults.keyboardDigitHints),
                keyboardWidth = prefs.valueOr(KB_WIDTH, defaults.keyboardWidth)
                    .coerceIn(KeyboardFit.WIDTH_MIN, KeyboardFit.WIDTH_MAX),
                keyboardShift = prefs.valueOr(KB_SHIFT, defaults.keyboardShift)
                    .coerceIn(KeyboardFit.SHIFT_MIN, KeyboardFit.SHIFT_MAX),
                homeListOffset = prefs[HOME_LIST_OFFSET]?.coerceIn(0, HomeListOffset.MAX_DP),
                searchColoredIcons = prefs.valueOr(SEARCH_COLORED_ICONS, defaults.searchColoredIcons),
                gestureShiftPenalty = prefs.valueOr(GESTURE_SHIFT_PENALTY, defaults.gestureShiftPenalty)
                    .coerceIn(GestureTuning.SHIFT_PENALTY_MIN, GestureTuning.SHIFT_PENALTY_MAX),
                gestureSuggestMargin = prefs.valueOr(GESTURE_SUGGEST_MARGIN, defaults.gestureSuggestMargin)
                    .coerceIn(GestureTuning.SUGGEST_MARGIN_MIN, GestureTuning.SUGGEST_MARGIN_MAX),
                searchFuzzy = prefs.valueOr(SEARCH_FUZZY, defaults.searchFuzzy),
                searchLayoutSwap = prefs.valueOr(SEARCH_LAYOUT_SWAP, defaults.searchLayoutSwap),
                searchTranslit = prefs.valueOr(SEARCH_TRANSLIT, defaults.searchTranslit),
                onboardingDone = prefs.valueOr(ONBOARDING_DONE, defaults.onboardingDone),
            )
        }

        private fun encode(settings: LauncherSettings, prefs: MutablePreferences) {
            val spaceId = settings.currentSpaceId
            if (spaceId == null) prefs.remove(CURRENT_SPACE_ID) else prefs[CURRENT_SPACE_ID] = spaceId
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
            prefs[NOTIFICATION_COUNTS] = settings.notificationCounts
            prefs[PRESS_FEEDBACK] = settings.pressFeedback.name
            prefs[KB_LAYOUTS] = settings.keyboardLayouts.joinToString(",")
            prefs[KB_ACTIVE_LAYOUT] = settings.activeKeyboardLayout
            prefs[KB_HAPTICS] = settings.keyboardHaptics
            prefs[KB_DIGIT_HINTS] = settings.keyboardDigitHints
            prefs[KB_WIDTH] = settings.keyboardWidth
            prefs[KB_SHIFT] = settings.keyboardShift
            val listOffset = settings.homeListOffset
            if (listOffset == null) prefs.remove(HOME_LIST_OFFSET) else prefs[HOME_LIST_OFFSET] = listOffset
            prefs[SEARCH_COLORED_ICONS] = settings.searchColoredIcons
            prefs[GESTURE_SHIFT_PENALTY] = settings.gestureShiftPenalty
            prefs[GESTURE_SUGGEST_MARGIN] = settings.gestureSuggestMargin
            prefs[SEARCH_FUZZY] = settings.searchFuzzy
            prefs[SEARCH_LAYOUT_SWAP] = settings.searchLayoutSwap
            prefs[SEARCH_TRANSLIT] = settings.searchTranslit
            prefs[ONBOARDING_DONE] = settings.onboardingDone
        }

        private fun <T> Preferences.valueOr(key: Preferences.Key<T>, default: T): T = this[key] ?: default

        private fun decodeLayouts(raw: String?): List<String>? = raw
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }

        private inline fun <reified E : Enum<E>> enumOr(raw: String?, default: E): E =
            raw?.let { value -> enumValues<E>().firstOrNull { it.name == value } } ?: default
    }
}
