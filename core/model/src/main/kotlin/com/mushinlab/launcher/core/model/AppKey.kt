package com.mushinlab.launcher.core.model

/**
 * Identity of a launchable activity: package, activity class and serial number of the user profile.
 *
 * The same app in the main and the work profile has two different keys. [id] is a stable string form.
 */
data class AppKey(
    val packageName: String,
    val activityName: String,
    val userSerial: Long,
) {
    val id: String get() = "$packageName/$activityName/$userSerial"
}
