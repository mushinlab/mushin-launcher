package com.mushinlab.launcher.core.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.net.Uri
import android.os.Process
import android.os.UserManager
import com.mushinlab.launcher.core.model.AppKey

/**
 * Outcome of [AppLauncher.launch]. [UnlockRequested] means the profile was paused and the user was asked to
 * turn it on; [Suspended] means the package is suspended and cannot start.
 */
sealed interface LaunchResult {
    data object Started : LaunchResult

    data object UnlockRequested : LaunchResult

    data object Suspended : LaunchResult

    data object Failed : LaunchResult
}

/** Starts apps and their system screens (app info, uninstall) in any user profile. */
class AppLauncher(
    private val context: Context,
    private val gateway: AndroidLauncherAppsGateway,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    /**
     * Starts the activity of [key]; never throws.
     *
     * [sourceBounds] is the on-screen origin of the opening animation. With [animate] false an app of the main
     * profile opens without a window animation. A paused profile gets an unpause request instead of a launch.
     */
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
        } catch (ignored: SecurityException) {
            if (isSuspended(key)) LaunchResult.Suspended else LaunchResult.Failed
        } catch (ignored: ActivityNotFoundException) {
            LaunchResult.Failed
        } catch (ignored: IllegalStateException) {
            if (isSuspended(key)) LaunchResult.Suspended else LaunchResult.Failed
        }
    }

    /** Opens the system app info screen; false when it could not be started. */
    fun openDetails(key: AppKey): Boolean {
        val user = gateway.userFor(key.userSerial) ?: return false
        return runCatching {
            launcherApps.startAppDetailsActivity(ComponentName(key.packageName, key.activityName), user, null, null)
        }.isSuccess
    }

    /** Opens the system uninstall dialog for the package of [key]; false when it could not be started. */
    fun uninstall(key: AppKey): Boolean {
        val user = gateway.userFor(key.userSerial) ?: return false
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${key.packageName}"))
            .putExtra(Intent.EXTRA_USER, user)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    /** Asks the system to turn a paused profile back on; the user may still have to confirm. */
    fun requestUnquiet(userSerial: Long): Boolean {
        val user = gateway.userFor(userSerial) ?: return false
        return runCatching { userManager.requestQuietModeEnabled(false, user) }.isSuccess
    }

    private fun isSuspended(key: AppKey): Boolean = gateway.activities(key.userSerial, key.packageName)
        .any { it.isSuspended }
}
