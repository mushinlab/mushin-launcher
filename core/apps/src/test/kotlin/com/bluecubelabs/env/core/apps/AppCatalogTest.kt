package com.bluecubelabs.env.core.apps

import app.cash.turbine.test
import com.bluecubelabs.env.core.model.ProfileType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppCatalogTest {
    private val gateway = FakeLauncherAppsGateway().apply {
        profiles += ProfileInfo(serial = 10, type = ProfileType.WORK, isQuiet = true)
        install("com.slack", "com.slack.Main", "Slack")
        install("com.google.maps", "com.google.maps.Main", "Maps")
        install("com.slack", "com.slack.Main", "Slack", serial = 10)
    }

    @Test
    fun AC_02_1_loadsEveryProfile() = runTest {
        val catalog = AppCatalog(gateway, this, StandardTestDispatcher(testScheduler))
        catalog.start()
        advanceUntilIdle()
        val apps = catalog.apps.value
        assertThat(apps.map { it.label to it.key.userSerial }).containsExactly(
            "Slack" to 0L,
            "Maps" to 0L,
            "Slack" to 10L,
        )
        assertThat(catalog.loaded.value).isTrue()
        assertThat(gateway.listener).isSameInstanceAs(catalog)
    }

    @Test
    fun AC_02_5_quietProfileAppsAreDimmed() = runTest {
        val catalog = AppCatalog(gateway, this, StandardTestDispatcher(testScheduler))
        catalog.start()
        advanceUntilIdle()
        val work = catalog.apps.value.single { it.key.userSerial == 10L }
        assertThat(work.profile).isEqualTo(ProfileType.WORK)
        assertThat(work.isQuiet).isTrue()
        assertThat(work.isDimmed).isTrue()
        assertThat(catalog.apps.value.filter { it.key.userSerial == 0L }.none { it.isDimmed }).isTrue()
    }

    @Test
    fun AC_02_2_addedPackageAppearsAndEmitsEvent() = runTest {
        val catalog = AppCatalog(gateway, this, StandardTestDispatcher(testScheduler))
        catalog.start()
        advanceUntilIdle()
        catalog.events.test {
            gateway.install("org.telegram", "org.telegram.Main", "Telegram")
            gateway.listener?.onPackageAdded("org.telegram", 0)
            assertThat(awaitItem()).isEqualTo(PackageEvent.Added("org.telegram", 0))
        }
        assertThat(catalog.apps.value.map { it.label }).contains("Telegram")
    }

    @Test
    fun AC_02_3_removedPackageDisappearsOnlyForThatProfile() = runTest {
        val catalog = AppCatalog(gateway, this, StandardTestDispatcher(testScheduler))
        catalog.start()
        advanceUntilIdle()
        catalog.events.test {
            gateway.uninstall("com.slack")
            gateway.listener?.onPackageRemoved("com.slack", 0)
            assertThat(awaitItem()).isEqualTo(PackageEvent.Removed("com.slack", 0))
        }
        assertThat(catalog.apps.value.map { it.label to it.key.userSerial }).containsExactly(
            "Maps" to 0L,
            "Slack" to 10L,
        )
    }

    @Test
    fun AC_02_4_changedPackageReloadsLabel() = runTest {
        val catalog = AppCatalog(gateway, this, StandardTestDispatcher(testScheduler))
        catalog.start()
        advanceUntilIdle()
        gateway.uninstall("com.google.maps")
        gateway.install("com.google.maps", "com.google.maps.Main", "Google Maps")
        gateway.listener?.onPackageChanged("com.google.maps", 0)
        advanceUntilIdle()
        assertThat(catalog.apps.value.map { it.label }).contains("Google Maps")
        assertThat(catalog.apps.value.map { it.label }).doesNotContain("Maps")
    }

    @Test
    fun AC_02_6_everyLauncherActivityIsSeparateComponent() = runTest {
        gateway.install("com.google.maps", "com.google.maps.Navigation", "Navigation")
        val catalog = AppCatalog(gateway, this, StandardTestDispatcher(testScheduler))
        catalog.start()
        advanceUntilIdle()
        assertThat(catalog.apps.value.filter { it.key.packageName == "com.google.maps" }).hasSize(2)
    }

    @Test
    fun suspendedPackagesAreDimmedAndRestored() = runTest {
        val catalog = AppCatalog(gateway, this, StandardTestDispatcher(testScheduler))
        catalog.start()
        advanceUntilIdle()
        gateway.listener?.onPackagesSuspended(listOf("com.google.maps"), 0, suspended = true)
        advanceUntilIdle()
        assertThat(catalog.apps.value.single { it.label == "Maps" }.isSuspended).isTrue()
        gateway.listener?.onPackagesSuspended(listOf("com.google.maps"), 0, suspended = false)
        advanceUntilIdle()
        assertThat(catalog.apps.first().single { it.label == "Maps" }.isSuspended).isFalse()
    }

    @Test
    fun profileChangeReloadsQuietFlags() = runTest {
        val catalog = AppCatalog(gateway, this, StandardTestDispatcher(testScheduler))
        catalog.start()
        advanceUntilIdle()
        gateway.profiles[1] = gateway.profiles[1].copy(isQuiet = false)
        gateway.listener?.onProfilesChanged()
        advanceUntilIdle()
        assertThat(catalog.apps.value.single { it.key.userSerial == 10L }.isQuiet).isFalse()
    }
}
