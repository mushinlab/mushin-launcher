package com.bluecubelabs.env.core.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.net.Uri
import android.os.Process
import android.os.UserManager
import com.bluecubelabs.env.core.model.AppKey

sealed interface LaunchResult {
    data object Started : LaunchResult

    data object UnlockRequested : LaunchResult

    data object Suspended : LaunchResult

    data object Failed : LaunchResult
}

class AppLauncher(
    private val context: Context,
    private val gateway: AndroidLauncherAppsGateway,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    fun launch(key: AppKey, sourceBounds: Rect? = null, animate: Boolean = true): LaunchResult {
        val user = gateway.userFor(key.userSerial) ?: return LaunchResult.Failed
        if (runCatching { userManager.isQuietModeEnabled(user) }.getOrDefault(false)) {
            return if (requestUnquiet(key.userSerial)) LaunchResult.UnlockRequested else LaunchResult.Failed
        }
        val component = ComponentName(key.packageName, key.activityName)
        return try {
            if (animate || user != Process.myUserHandle()) {
                launcherApps.startMainActivity(component, user, sourceBounds, null)
            } else {
                context.startActivity(
                    Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(component)
                        .addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                                Intent.FLAG_ACTIVITY_NO_ANIMATION,
                        ),
                )
            }
            LaunchResult.Started
        } catch (e: SecurityException) {
            if (isSuspended(key)) LaunchResult.Suspended else LaunchResult.Failed
        } catch (e: ActivityNotFoundException) {
            LaunchResult.Failed
        } catch (e: IllegalStateException) {
            if (isSuspended(key)) LaunchResult.Suspended else LaunchResult.Failed
        }
    }

    fun openDetails(key: AppKey): Boolean {
        val user = gateway.userFor(key.userSerial) ?: return false
        return runCatching {
            launcherApps.startAppDetailsActivity(ComponentName(key.packageName, key.activityName), user, null, null)
        }.isSuccess
    }

    fun uninstall(key: AppKey): Boolean {
        val user = gateway.userFor(key.userSerial) ?: return false
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${key.packageName}"))
            .putExtra(Intent.EXTRA_USER, user)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    fun requestUnquiet(userSerial: Long): Boolean {
        val user = gateway.userFor(userSerial) ?: return false
        return runCatching { userManager.requestQuietModeEnabled(false, user) }.isSuccess
    }

    private fun isSuspended(key: AppKey): Boolean = gateway.activities(key.userSerial, key.packageName)
        .any { it.isSuspended }
}
