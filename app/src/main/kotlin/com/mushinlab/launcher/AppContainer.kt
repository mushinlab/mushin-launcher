package com.mushinlab.launcher

import android.app.Application
import android.app.NotificationManager
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.util.Log
import com.mushinlab.launcher.core.apps.AndroidLauncherAppsGateway
import com.mushinlab.launcher.core.apps.AppCatalog
import com.mushinlab.launcher.core.apps.AppLauncher
import com.mushinlab.launcher.core.apps.NotificationShade
import com.mushinlab.launcher.core.apps.NotificationSource
import com.mushinlab.launcher.core.apps.PackageEvent
import com.mushinlab.launcher.core.apps.ReflectionNotificationShade
import com.mushinlab.launcher.core.apps.icons.IconLoader
import com.mushinlab.launcher.core.apps.icons.LauncherIconSource
import com.mushinlab.launcher.core.data.background.LegacyBackgroundTransition
import com.mushinlab.launcher.core.data.background.SpaceBackgroundStore
import com.mushinlab.launcher.core.data.db.MushinDatabase
import com.mushinlab.launcher.core.data.repo.GestureLearningRepository
import com.mushinlab.launcher.core.data.repo.GestureRepository
import com.mushinlab.launcher.core.data.repo.HistoryRepository
import com.mushinlab.launcher.core.data.repo.OverrideRepository
import com.mushinlab.launcher.core.data.repo.PackageDataCleaner
import com.mushinlab.launcher.core.data.repo.SpaceLookRepository
import com.mushinlab.launcher.core.data.repo.SpaceRepository
import com.mushinlab.launcher.core.data.settings.SettingsRepository
import com.mushinlab.launcher.core.data.snapshot.HomeSnapshotStore
import java.io.File
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AppContainer(private val app: Application) {
    val scope = CoroutineScope(
        SupervisorJob() +
            Dispatchers.Default +
            CoroutineExceptionHandler { _, e -> Log.w(TAG, "Background task failed", e) },
    )

    val database by lazy { MushinDatabase.build(app) }
    val spaces by lazy { SpaceRepository(database, scope) }
    val looks by lazy { SpaceLookRepository(database) }
    val overrides by lazy { OverrideRepository(database, scope) }
    val history by lazy { HistoryRepository(database, scope) }
    val gestures by lazy { GestureRepository(database, scope) }
    val gestureLearning by lazy { GestureLearningRepository(database, scope) }
    val cleaner by lazy { PackageDataCleaner(database) }
    val settings by lazy { SettingsRepository.create(app, scope) }
    val snapshots by lazy { HomeSnapshotStore(app.filesDir) }
    val language by lazy { AppLanguage(app) }
    val backgrounds by lazy {
        val metrics = app.resources.displayMetrics
        SpaceBackgroundStore(
            File(app.filesDir, BACKGROUNDS_DIR),
            minOf(metrics.widthPixels, metrics.heightPixels),
            maxOf(metrics.widthPixels, metrics.heightPixels),
        )
    }

    val gateway by lazy { AndroidLauncherAppsGateway(app) }
    val catalog by lazy { AppCatalog(gateway, scope) }
    val icons by lazy { IconLoader(app, LauncherIconSource(app, gateway)) }
    val launcher by lazy { AppLauncher(app, gateway) }
    val shade: NotificationShade by lazy { ReflectionNotificationShade(app) }
    val iconProvider by lazy { LauncherIconProvider(icons, catalog) }
    val notificationCounts = MutableStateFlow<Map<NotificationSource, Int>>(emptyMap())
    val notificationListener = ComponentName(app, NotificationCountService::class.java)

    fun hasNotificationAccess(): Boolean =
        app.getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(notificationListener)

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
        scope.launch {
            catalog.loaded.first { it }
            catalog.apps.collectLatest { apps -> cleaner.reconcile(apps.map { it.key }) }
        }
        scope.launch {
            settings.settings.map { it.notificationCounts }.distinctUntilChanged().filter { it }.collect {
                if (hasNotificationAccess()) NotificationListenerService.requestRebind(notificationListener)
            }
        }
        scope.launch(Dispatchers.IO) { ExitLog(app).recordNewExits() }
        scope.launch(Dispatchers.IO) {
            backgrounds.scan()
            val legacy = File(app.filesDir, LegacyBackgroundTransition.LEGACY_FILE_NAME)
            LegacyBackgroundTransition(legacy, backgrounds, database, settings).run()
        }
    }

    private companion object {
        const val TAG = "AppContainer"
        const val BACKGROUNDS_DIR = "backgrounds"
    }
}
