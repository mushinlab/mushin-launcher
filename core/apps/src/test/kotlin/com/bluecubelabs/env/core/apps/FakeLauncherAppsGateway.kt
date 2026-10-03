package com.bluecubelabs.env.core.apps

import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.ProfileType

class FakeLauncherAppsGateway : LauncherAppsGateway {
    val profiles = mutableListOf(
        ProfileInfo(serial = 0, type = ProfileType.MAIN, isQuiet = false),
    )
    val records = mutableMapOf<Long, MutableList<ActivityRecord>>()
    var listener: LauncherAppsGateway.Listener? = null
        private set

    override fun profiles(): List<ProfileInfo> = profiles.toList()

    override fun activities(userSerial: Long, packageName: String?): List<ActivityRecord> =
        records[userSerial].orEmpty().filter { packageName == null || it.key.packageName == packageName }

    override fun setListener(listener: LauncherAppsGateway.Listener?) {
        this.listener = listener
    }

    fun install(packageName: String, activity: String, label: String, serial: Long = 0, system: Boolean = false) {
        records.getOrPut(serial) { mutableListOf() } += ActivityRecord(
            key = AppKey(packageName, activity, serial),
            label = label,
            firstInstallTime = 1L,
            lastUpdateTime = 1L,
            isSystem = system,
            isSuspended = false,
        )
    }

    fun uninstall(packageName: String, serial: Long = 0) {
        records[serial]?.removeAll { it.key.packageName == packageName }
    }
}
