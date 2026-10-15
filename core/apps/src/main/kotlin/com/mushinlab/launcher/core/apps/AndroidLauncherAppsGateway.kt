package com.mushinlab.launcher.core.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.ContextCompat
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.ProfileType
import java.io.File

class AndroidLauncherAppsGateway(private val context: Context) : LauncherAppsGateway {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var callback: LauncherApps.Callback? = null
    private var receiver: BroadcastReceiver? = null

    fun userFor(serial: Long): UserHandle? = userManager.getUserForSerialNumber(serial)

    fun serialOf(user: UserHandle): Long = userManager.getSerialNumberForUser(user)

    override fun profiles(): List<ProfileInfo> = launcherApps.profiles.map { user ->
        ProfileInfo(
            serial = serialOf(user),
            type = profileType(user),
            isQuiet = runCatching { userManager.isQuietModeEnabled(user) }.getOrDefault(false),
        )
    }

    override fun activities(userSerial: Long, packageName: String?): List<ActivityRecord> {
        val user = userFor(userSerial) ?: return emptyList()
        return runCatching { launcherApps.getActivityList(packageName, user) }
            .getOrDefault(emptyList())
            .map { it.toRecord(userSerial) }
    }

    override fun setListener(listener: LauncherAppsGateway.Listener?) {
        callback?.let { launcherApps.unregisterCallback(it) }
        receiver?.let { context.unregisterReceiver(it) }
        callback = null
        receiver = null
        if (listener == null) return
        val newCallback = PackageCallback(listener)
        launcherApps.registerCallback(newCallback, mainHandler)
        callback = newCallback
        val newReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                listener.onProfilesChanged()
            }
        }
        ContextCompat.registerReceiver(context, newReceiver, profileFilter(), ContextCompat.RECEIVER_NOT_EXPORTED)
        receiver = newReceiver
    }

    private fun profileFilter() = IntentFilter().apply {
        addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
        addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
        addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
        addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
        addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            addAction(Intent.ACTION_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
        }
    }

    private fun profileType(user: UserHandle): ProfileType {
        if (user == Process.myUserHandle()) return ProfileType.MAIN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val type = runCatching { launcherApps.getLauncherUserInfo(user)?.userType }.getOrNull()
            return when (type) {
                UserManager.USER_TYPE_PROFILE_MANAGED -> ProfileType.WORK
                UserManager.USER_TYPE_PROFILE_PRIVATE -> ProfileType.PRIVATE
                null -> ProfileType.WORK
                else -> ProfileType.OTHER
            }
        }
        return ProfileType.WORK
    }

    private fun LauncherActivityInfo.toRecord(userSerial: Long): ActivityRecord {
        val app = applicationInfo
        val packageName = componentName.packageName
        return ActivityRecord(
            key = AppKey(packageName, componentName.className, userSerial),
            label = label?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: packageName,
            firstInstallTime = firstInstallTime,
            lastUpdateTime = app.sourceDir?.let { File(it).lastModified() } ?: firstInstallTime,
            isSystem = app.flags and ApplicationInfo.FLAG_SYSTEM != 0 &&
                app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0,
            isSuspended = app.flags and ApplicationInfo.FLAG_SUSPENDED != 0,
        )
    }

    private inner class PackageCallback(private val listener: LauncherAppsGateway.Listener) : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            listener.onPackageRemoved(packageName, serialOf(user))
        }

        override fun onPackageAdded(packageName: String, user: UserHandle) {
            listener.onPackageAdded(packageName, serialOf(user))
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            listener.onPackageChanged(packageName, serialOf(user))
        }

        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            val serial = serialOf(user)
            packageNames.forEach { listener.onPackageChanged(it, serial) }
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            if (replacing) return
            val serial = serialOf(user)
            packageNames.forEach { listener.onPackageChanged(it, serial) }
        }

        override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) {
            listener.onPackagesSuspended(packageNames.toList(), serialOf(user), suspended = true)
        }

        override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) {
            listener.onPackagesSuspended(packageNames.toList(), serialOf(user), suspended = false)
        }
    }
}
