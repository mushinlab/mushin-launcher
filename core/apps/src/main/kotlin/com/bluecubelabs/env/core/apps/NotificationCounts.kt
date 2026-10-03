package com.bluecubelabs.env.core.apps

data class PostedNotification(
    val packageName: String,
    val userSerial: Long,
    val clearable: Boolean,
    val groupSummary: Boolean,
)

data class NotificationSource(val packageName: String, val userSerial: Long)

object NotificationCounts {
    fun count(posted: List<PostedNotification>, ownPackage: String): Map<NotificationSource, Int> = posted
        .filter { it.clearable && !it.groupSummary && it.packageName != ownPackage }
        .groupingBy { NotificationSource(it.packageName, it.userSerial) }
        .eachCount()
}
