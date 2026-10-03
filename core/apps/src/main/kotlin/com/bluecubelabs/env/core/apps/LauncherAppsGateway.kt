package com.bluecubelabs.env.core.apps

import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.ProfileType

data class ProfileInfo(
    val serial: Long,
    val type: ProfileType,
    val isQuiet: Boolean,
)

data class ActivityRecord(
    val key: AppKey,
    val label: String,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isSystem: Boolean,
    val isSuspended: Boolean,
)

interface LauncherAppsGateway {
    fun profiles(): List<ProfileInfo>

    fun activities(userSerial: Long, packageName: String? = null): List<ActivityRecord>

    fun setListener(listener: Listener?)

    interface Listener {
        fun onPackageAdded(packageName: String, userSerial: Long)

        fun onPackageRemoved(packageName: String, userSerial: Long)

        fun onPackageChanged(packageName: String, userSerial: Long)

        fun onPackagesSuspended(packageNames: List<String>, userSerial: Long, suspended: Boolean)

        fun onProfilesChanged()
    }
}
