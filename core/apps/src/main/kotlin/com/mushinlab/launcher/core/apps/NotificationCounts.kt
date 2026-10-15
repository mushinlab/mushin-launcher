package com.mushinlab.launcher.core.apps

/** A notification currently in the shade, reduced to what counting needs. */
data class PostedNotification(
    val packageName: String,
    val userSerial: Long,
    val clearable: Boolean,
    val groupSummary: Boolean,
)

data class NotificationSource(val packageName: String, val userSerial: Long)

/**
 * Per-app notification counts for the badges next to app labels.
 *
 * [count] includes only notifications the user can dismiss and skips group summaries and the launcher's own.
 */
object NotificationCounts {
    fun count(posted: List<PostedNotification>, ownPackage: String): Map<NotificationSource, Int> = posted
        .filter { it.clearable && !it.groupSummary && it.packageName != ownPackage }
        .groupingBy { NotificationSource(it.packageName, it.userSerial) }
        .eachCount()
}
