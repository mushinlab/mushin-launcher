package com.mushinlab.launcher.core.model

enum class ProfileType { MAIN, WORK, PRIVATE, OTHER }

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

data class AppOverride(
    val key: AppKey,
    val customLabel: String?,
    val hidden: Boolean,
    val orphanedAt: Long?,
    val updatedAt: Long,
    val systemShortcut: Boolean = false,
)

data class AppUsage(
    val key: AppKey,
    val launchCount: Int,
    val lastLaunchedAt: Long,
    val decayedScore: Double,
)

data class SearchRecent(
    val key: AppKey,
    val lastLaunchedAt: Long,
    val launchCount: Int,
)

object OverrideLimits {
    const val LABEL_MAX_LENGTH = 40
    const val ORPHAN_TTL_MS = 30L * 24 * 60 * 60 * 1000
}
