package com.mushinlab.launcher.core.apps

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Process
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppKey
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AppLauncherTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val launcher = AppLauncher(app, AndroidLauncherAppsGateway(app))
    private val serial = app.getSystemService(UserManager::class.java).getSerialNumberForUser(Process.myUserHandle())
    private val slack = AppKey("com.slack", "com.slack.Main", serial)
    private val stranger = AppKey("com.slack", "com.slack.Main", serial + 1000)

    @Test
    fun appsOfAnUnknownProfileAreNeverStarted() {
        assertThat(launcher.launch(stranger)).isEqualTo(LaunchResult.Failed)
        assertThat(launcher.openDetails(stranger)).isFalse()
        assertThat(launcher.uninstall(stranger)).isFalse()
        assertThat(launcher.requestUnquiet(stranger.userSerial)).isFalse()
        assertThat(shadowOf(app).nextStartedActivity).isNull()
    }

    @Test
    fun launchWithoutAnimationStartsExactlyThatActivityInANewTask() {
        val result = launcher.launch(slack, animate = false)

        val started = shadowOf(app).nextStartedActivity
        assertThat(result).isEqualTo(LaunchResult.Started)
        assertThat(started.component).isEqualTo(ComponentName("com.slack", "com.slack.Main"))
        assertThat(started.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0)
    }

    @Test
    fun uninstallGoesThroughTheSystemConfirmation() {
        assertThat(launcher.uninstall(slack)).isTrue()

        val started = shadowOf(app).nextStartedActivity
        assertThat(started.action).isEqualTo(Intent.ACTION_DELETE)
        assertThat(started.data.toString()).isEqualTo("package:com.slack")
    }
}
