package com.bluecubelabs.env

import android.app.Application
import com.bluecubelabs.env.core.apps.AndroidLauncherAppsGateway
import com.bluecubelabs.env.core.apps.AppCatalog
import com.bluecubelabs.env.core.apps.AppLauncher
import com.bluecubelabs.env.core.apps.NotificationShade
import com.bluecubelabs.env.core.apps.PackageEvent
import com.bluecubelabs.env.core.apps.ReflectionNotificationShade
import com.bluecubelabs.env.core.apps.icons.IconLoader
import com.bluecubelabs.env.core.apps.icons.LauncherIconSource
import com.bluecubelabs.env.core.data.db.EnvDatabase
import com.bluecubelabs.env.core.data.repo.EnvironmentRepository
import com.bluecubelabs.env.core.data.repo.GestureRepository
import com.bluecubelabs.env.core.data.repo.HistoryRepository
import com.bluecubelabs.env.core.data.repo.OverrideRepository
import com.bluecubelabs.env.core.data.repo.PackageDataCleaner
import com.bluecubelabs.env.core.data.settings.SettingsRepository
import com.bluecubelabs.env.core.data.snapshot.HomeSnapshotStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(private val app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { EnvDatabase.build(app) }
    val environments by lazy { EnvironmentRepository(database, scope) }
    val overrides by lazy { OverrideRepository(database, scope) }
    val history by lazy { HistoryRepository(database, scope) }
    val gestures by lazy { GestureRepository(database, scope) }
    val cleaner by lazy { PackageDataCleaner(database) }
    val settings by lazy { SettingsRepository.create(app, scope) }
    val snapshots by lazy { HomeSnapshotStore(app.filesDir) }

    val gateway by lazy { AndroidLauncherAppsGateway(app) }
    val catalog by lazy { AppCatalog(gateway, scope) }
    val icons by lazy { IconLoader(app, LauncherIconSource(app, gateway)) }
    val launcher by lazy { AppLauncher(app, gateway) }
    val shade: NotificationShade by lazy { ReflectionNotificationShade(app) }

    fun densityScale(): Float = app.resources.displayMetrics.density

    fun start() {
        catalog.start()
        scope.launch {
            catalog.events.collect { event ->
                icons.invalidate(event.packageName, event.userSerial)
                when (event) {
                    is PackageEvent.Removed -> cleaner.onPackageRemoved(event.packageName, event.userSerial)
                    is PackageEvent.Added -> cleaner.onPackageAdded(event.packageName, event.userSerial)
                    is PackageEvent.Changed -> Unit
                }
            }
        }
        scope.launch { cleaner.purgeExpiredOverrides() }
        scope.launch(Dispatchers.IO) { ExitLog(app).recordNewExits() }
    }
}
