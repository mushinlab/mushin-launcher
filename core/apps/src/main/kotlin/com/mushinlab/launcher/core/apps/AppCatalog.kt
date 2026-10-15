package com.mushinlab.launcher.core.apps

import com.mushinlab.launcher.core.model.AppEntry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface PackageEvent {
    val packageName: String
    val userSerial: Long

    data class Added(override val packageName: String, override val userSerial: Long) : PackageEvent

    data class Changed(override val packageName: String, override val userSerial: Long) : PackageEvent

    data class Removed(override val packageName: String, override val userSerial: Long) : PackageEvent
}

class AppCatalog(
    private val gateway: LauncherAppsGateway,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : LauncherAppsGateway.Listener {
    private val mutex = Mutex()
    private val _apps = MutableStateFlow<ImmutableList<AppEntry>>(persistentListOf())
    private val _loaded = MutableStateFlow(false)
    private val _events = MutableSharedFlow<PackageEvent>(extraBufferCapacity = 64)
    private var profiles: Map<Long, ProfileInfo> = emptyMap()

    val apps: StateFlow<ImmutableList<AppEntry>> = _apps.asStateFlow()
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()
    val events: SharedFlow<PackageEvent> = _events.asSharedFlow()

    fun start() {
        gateway.setListener(this)
        reloadAll()
    }

    fun stop() {
        gateway.setListener(null)
    }

    fun reloadAll() {
        scope.launch(dispatcher) {
            mutex.withLock {
                val currentProfiles = gateway.profiles()
                profiles = currentProfiles.associateBy { it.serial }
                val entries = currentProfiles.flatMap { profile ->
                    gateway.activities(profile.serial).map { it.toEntry(profile) }
                }
                _apps.value = entries.toImmutableList()
                _loaded.value = true
            }
        }
    }

    override fun onPackageAdded(packageName: String, userSerial: Long) {
        refreshPackage(packageName, userSerial, PackageEvent.Added(packageName, userSerial))
    }

    override fun onPackageChanged(packageName: String, userSerial: Long) {
        refreshPackage(packageName, userSerial, PackageEvent.Changed(packageName, userSerial))
    }

    override fun onPackageRemoved(packageName: String, userSerial: Long) {
        scope.launch(dispatcher) {
            mutex.withLock {
                _apps.value = _apps.value
                    .filterNot { it.key.packageName == packageName && it.key.userSerial == userSerial }
                    .toImmutableList()
            }
            _events.emit(PackageEvent.Removed(packageName, userSerial))
        }
    }

    override fun onPackagesSuspended(packageNames: List<String>, userSerial: Long, suspended: Boolean) {
        scope.launch(dispatcher) {
            mutex.withLock {
                _apps.value = _apps.value.map { entry ->
                    if (entry.key.userSerial == userSerial && entry.key.packageName in packageNames) {
                        entry.copy(isSuspended = suspended)
                    } else {
                        entry
                    }
                }.toImmutableList()
            }
        }
    }

    override fun onProfilesChanged() {
        reloadAll()
    }

    private fun refreshPackage(packageName: String, userSerial: Long, event: PackageEvent) {
        scope.launch(dispatcher) {
            mutex.withLock {
                val profile = profiles[userSerial] ?: return@withLock
                val fresh = gateway.activities(userSerial, packageName).map { it.toEntry(profile) }
                val kept = _apps.value.filterNot {
                    it.key.packageName == packageName && it.key.userSerial == userSerial
                }
                _apps.value = (kept + fresh).toImmutableList()
            }
            _events.emit(event)
        }
    }

    private fun ActivityRecord.toEntry(profile: ProfileInfo) = AppEntry(
        key = key,
        label = label.take(MAX_LABEL_LENGTH),
        firstInstallTime = firstInstallTime,
        lastUpdateTime = lastUpdateTime,
        isSystem = isSystem,
        profile = profile.type,
        isQuiet = profile.isQuiet,
        isSuspended = isSuspended,
    )

    companion object {
        const val MAX_LABEL_LENGTH = 100
    }
}
