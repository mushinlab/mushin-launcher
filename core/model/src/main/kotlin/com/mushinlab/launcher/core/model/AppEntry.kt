package com.mushinlab.launcher.core.model

/** Android user profile of an app; [OTHER] covers profile types the launcher does not recognize. */
enum class ProfileType { MAIN, WORK, PRIVATE, OTHER }

/**
 * A launchable activity as reported by the system for one user profile.
 *
 * [isQuiet] is true while the profile is paused (for example, the work profile is turned off) and [isSuspended]
 * while the package is suspended by the system or an admin; either one makes the app [isDimmed].
 */
data class AppEntry(
    val key: AppKey,
    val label: String,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isSystem: Boolean,
    val profile: ProfileType,
    val isQuiet: Boolean = false,
    val isSuspended: Boolean = false,
) {
    val isDimmed: Boolean get() = isQuiet || isSuspended
}

/**
 * User changes to one app: custom label, hidden flag and the manual "bound to a system gesture" mark.
 *
 * [orphanedAt] is set when the package is removed; the override is kept for [OverrideLimits.ORPHAN_TTL_MS]
 * so that reinstalling the app restores it.
 */
data class AppOverride(
    val key: AppKey,
    val customLabel: String?,
    val hidden: Boolean,
    val orphanedAt: Long?,
    val updatedAt: Long,
    val systemShortcut: Boolean = false,
)

/** Launch statistics of an app across all spaces; [decayedScore] is a launch count decayed by [UsageDecay]. */
data class AppUsage(
    val key: AppKey,
    val launchCount: Int,
    val lastLaunchedAt: Long,
    val decayedScore: Double,
)

/** An app launched from search, listed in the search recents of a space. */
data class SearchRecent(
    val key: AppKey,
    val lastLaunchedAt: Long,
    val launchCount: Int,
)

/** Limits for [AppOverride]: label length in characters and retention of overrides of removed packages. */
object OverrideLimits {
    const val LABEL_MAX_LENGTH = 40
    const val ORPHAN_TTL_MS = 30L * 24 * 60 * 60 * 1000
}
