package com.bluecubelabs.env.core.model

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
