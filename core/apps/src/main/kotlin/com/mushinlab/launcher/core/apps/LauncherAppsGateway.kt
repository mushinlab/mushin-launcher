package com.mushinlab.launcher.core.apps

import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.ProfileType

/** A user profile visible to the launcher; [isQuiet] is true while the profile is paused. */
data class ProfileInfo(
    val serial: Long,
    val type: ProfileType,
    val isQuiet: Boolean,
)

/**
 * A launchable activity as read from the system. [lastUpdateTime] is the modification time of the APK in
 * epoch ms and invalidates cached icons.
 */
data class ActivityRecord(
    val key: AppKey,
    val label: String,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isSystem: Boolean,
    val isSuspended: Boolean,
)

/** Access to launchable activities and package callbacks, abstracted so [AppCatalog] is testable on the JVM. */
interface LauncherAppsGateway {
    fun profiles(): List<ProfileInfo>

    /** Launchable activities of a profile, optionally of one package; empty when the profile is gone. */
    fun activities(userSerial: Long, packageName: String? = null): List<ActivityRecord>

    /** Replaces the callback listener; null unregisters all callbacks. */
    fun setListener(listener: Listener?)

    /**
     * Package and profile changes. Packages that become available or unavailable (for example, on removable
     * storage) are reported through [onPackageChanged].
     */
    interface Listener {
        fun onPackageAdded(packageName: String, userSerial: Long)

        fun onPackageRemoved(packageName: String, userSerial: Long)

        fun onPackageChanged(packageName: String, userSerial: Long)

        fun onPackagesSuspended(packageNames: List<String>, userSerial: Long, suspended: Boolean)

        fun onProfilesChanged()
    }
}
