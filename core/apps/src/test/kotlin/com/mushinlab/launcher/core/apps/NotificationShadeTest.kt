package com.mushinlab.launcher.core.apps

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationShadeTest {
    class StatusBar {
        var expanded = false

        fun expandNotificationsPanel() {
            expanded = true
        }
    }

    private fun contextWith(service: Any?) =
        object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
            override fun getSystemService(name: String): Any? = service
        }

    @Test
    fun shadeOpensThroughTheStatusBarService() {
        val statusBar = StatusBar()

        assertThat(ReflectionNotificationShade(contextWith(statusBar)).expand()).isTrue()
        assertThat(statusBar.expanded).isTrue()
    }

    @Test
    fun missingServiceOrMethodIsReportedInsteadOfCrashing() {
        assertThat(ReflectionNotificationShade(contextWith(null)).expand()).isFalse()
        assertThat(ReflectionNotificationShade(contextWith(Any())).expand()).isFalse()
    }
}
