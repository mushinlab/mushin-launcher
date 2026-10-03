package com.bluecubelabs.env

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluecubelabs.env.core.apps.LaunchResult
import com.bluecubelabs.env.core.apps.icons.IconKey
import com.bluecubelabs.env.core.data.repo.EnvError
import com.bluecubelabs.env.core.data.repo.EnvOutcome
import com.bluecubelabs.env.core.data.repo.resolveCurrentEnvironment
import com.bluecubelabs.env.core.data.snapshot.HomeSnapshot
import com.bluecubelabs.env.core.data.snapshot.SnapshotRow
import com.bluecubelabs.env.core.model.AppEntry
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.AppOverride
import com.bluecubelabs.env.core.model.EnvType
import com.bluecubelabs.env.core.model.Environment
import com.bluecubelabs.env.core.model.GestureAction
import com.bluecubelabs.env.core.model.IconMode
import com.bluecubelabs.env.core.model.LauncherSettings
import com.bluecubelabs.env.core.model.Palette
import com.bluecubelabs.env.core.model.PalettePreset
import com.bluecubelabs.env.core.model.Palettes
import com.bluecubelabs.env.core.model.ProfileType
import com.bluecubelabs.env.core.model.SwipeDirection
import com.bluecubelabs.env.core.search.Alphabet
import com.bluecubelabs.env.core.search.SearchEngine
import com.bluecubelabs.env.core.search.SearchIndex
import com.bluecubelabs.env.core.search.SearchItem
import com.bluecubelabs.env.core.search.SearchOptions
import com.bluecubelabs.env.feature.home.HomeRow
import com.bluecubelabs.env.feature.home.HomeUiState
import com.bluecubelabs.env.feature.home.PopupEnv
import com.bluecubelabs.env.feature.search.RecentRow
import com.bluecubelabs.env.feature.settings.ERR_CUSTOM
import com.bluecubelabs.env.feature.settings.ERR_EMPTY
import com.bluecubelabs.env.feature.settings.ERR_LONG
import com.bluecubelabs.env.feature.settings.ERR_TAKEN
import com.bluecubelabs.env.feature.settings.ERR_WORK
import com.bluecubelabs.env.feature.settings.EditorItem
import com.bluecubelabs.env.feature.settings.EnvSummary
import com.bluecubelabs.env.feature.settings.GestureSummary
import com.bluecubelabs.env.feature.settings.PickerApp
import com.bluecubelabs.env.feature.settings.RenamedApp
import com.bluecubelabs.env.feature.settings.SettingsUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LauncherData(
    val envs: List<Environment>,
    val current: Environment?,
    val items: Map<Long, List<AppKey>>,
    val apps: Map<AppKey, AppEntry>,
    val overrides: Map<AppKey, AppOverride>,
    val settings: LauncherSettings,
)

@OptIn(FlowPreview::class)
class LauncherViewModel(
    private val container: AppContainer,
    initial: HomeSnapshot?,
    private val versionName: String,
) : ViewModel() {
    private val roleHeld = MutableStateFlow(true)
    private val capacity = MutableStateFlow(0)

    private val catalogMap: StateFlow<Map<AppKey, AppEntry>?> =
        combine(container.catalog.apps, container.catalog.loaded) { list, loaded ->
            if (loaded) list.associateBy { it.key } else null
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val data: StateFlow<LauncherData?> = combine(
        container.environments.environments,
        container.environments.items,
        catalogMap,
        container.overrides.overrides,
        container.settings.loaded,
    ) { envs, items, apps, overrides, settings ->
        if (apps == null || settings == null) {
            null
        } else {
            LauncherData(envs, resolveCurrentEnvironment(envs, settings.currentEnvId), items, apps, overrides, settings)
        }
    }
        .filter { it != null && it.envs.isNotEmpty() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val ready: StateFlow<Boolean> = data.map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val palette: StateFlow<Palette> = data.map { it?.current?.palette ?: initial?.palette ?: Palettes.Graphite }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, initial?.palette ?: Palettes.Graphite)

    val home: StateFlow<HomeUiState> = combine(data, roleHeld) { d, role ->
        if (d == null) snapshotState(initial, role) else homeState(d, role)
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, snapshotState(initial, roleHeld.value))

    val popupEnvs: StateFlow<ImmutableList<PopupEnv>> = data.map { d ->
        d?.envs?.map { PopupEnv(it.id, it.name, it.id == d.current?.id) }?.toImmutableList() ?: persistentListOf()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, persistentListOf())

    val searchIndex: StateFlow<SearchIndex> = data
        .map { d -> d?.let { buildIndex(it) } ?: SearchIndex.EMPTY }
        .distinctUntilChanged { a, b -> a === b }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, SearchIndex.EMPTY)

    val searchEngine: StateFlow<SearchEngine> = container.settings.settings
        .map { SearchOptions(fuzzy = it.searchFuzzy, layoutSwap = it.searchLayoutSwap, translit = it.searchTranslit) }
        .distinctUntilChanged()
        .map { SearchEngine(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SearchEngine())

    val usage = container.history.usage

    val settings: StateFlow<LauncherSettings> = container.settings.settings

    val recents: StateFlow<ImmutableList<RecentRow>> = combine(data, container.history.recents) { d, recents ->
        val env = d?.current ?: return@combine persistentListOf()
        val favorites = d.items[env.id].orEmpty().toSet()
        recents[env.id].orEmpty()
            .asSequence()
            .filter { it.key !in favorites }
            .filter { d.overrides[it.key]?.hidden != true }
            .mapNotNull { r -> d.apps[r.key]?.let { RecentRow(r.key, d.overrides[r.key]?.customLabel ?: it.label) } }
            .take(RECENTS_LIMIT)
            .toList()
            .toImmutableList()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, persistentListOf())

    val settingsState: StateFlow<SettingsUiState> = combine(data, container.gestures.bindings, roleHeld, capacity) {
            d,
            g,
            role,
            cap,
        ->
        if (d == null) {
            SettingsUiState(capacity = cap, isDefaultLauncher = role, versionName = versionName)
        } else {
            settingsState(d, g, role, cap)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState(versionName = versionName))

    val gestureBindings = container.gestures.bindings

    private val _iconsReady = MutableStateFlow(false)
    val iconsReady: StateFlow<Boolean> = _iconsReady

    init {
        home.debounce(SNAPSHOT_DEBOUNCE_MS)
            .onEach { state -> data.value?.let { d -> writeSnapshot(d, state) } }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
        viewModelScope.launch { warmIcons() }
    }

    fun setRoleHeld(held: Boolean) {
        roleHeld.value = held
    }

    fun setCapacity(value: Int) {
        capacity.value = value
    }

    fun currentEnvId(): Long? = data.value?.current?.id

    fun entry(key: AppKey): AppEntry? = data.value?.apps?.get(key)

    fun override(key: AppKey): AppOverride? = data.value?.overrides?.get(key)

    fun isInCurrentEnv(key: AppKey): Boolean {
        val d = data.value ?: return false
        return d.current?.let { key in d.items[it.id].orEmpty() } == true
    }

    fun launch(key: AppKey, fromSearch: Boolean): LaunchResult {
        val result = container.launcher.launch(key)
        if (result == LaunchResult.Started) {
            val envId = currentEnvId()
            container.scope.launch { container.history.recordLaunch(key, envId, fromSearch) }
        }
        return result
    }

    fun switchEnv(id: Long) {
        viewModelScope.launch { container.settings.update { it.copy(currentEnvId = id) } }
    }

    fun setActiveLayout(id: String) {
        viewModelScope.launch { container.settings.update { it.copy(activeKeyboardLayout = id) } }
    }

    fun setHidden(key: AppKey, hidden: Boolean) {
        container.scope.launch { container.overrides.setHidden(key, hidden) }
    }

    fun rename(key: AppKey, label: String?) {
        container.scope.launch { container.overrides.rename(key, label) }
    }

    fun removeFromCurrentEnv(key: AppKey) {
        val envId = currentEnvId() ?: return
        container.scope.launch { container.environments.removeItem(envId, key) }
    }

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        container.scope.launch { container.settings.update(transform) }
    }

    suspend fun createEnv(name: String, type: EnvType): String? =
        when (
            val result = container.environments.create(
                name,
                type,
                if (type ==
                    EnvType.WORK
                ) {
                    PalettePreset.NAVY
                } else {
                    PalettePreset.MOSS
                },
            )
        ) {
            is EnvOutcome.Ok -> null
            is EnvOutcome.Failed -> errorCode(result.error)
        }

    suspend fun renameEnv(id: Long, name: String): String? = when (
        val result = container.environments.rename(
            id,
            name,
        )
    ) {
        is EnvOutcome.Ok -> null
        is EnvOutcome.Failed -> errorCode(result.error)
    }

    fun deleteEnv(id: Long) {
        container.scope.launch { container.environments.delete(id) }
    }

    fun setPalette(id: Long, preset: PalettePreset) {
        container.scope.launch { container.environments.setPalette(id, preset) }
    }

    fun setEnvItems(id: Long, keys: List<AppKey>) {
        container.scope.launch { container.environments.setItems(id, keys) }
    }

    fun setGesture(direction: SwipeDirection, action: GestureAction) {
        container.scope.launch { container.gestures.set(direction, action) }
    }

    fun clearHistory(envId: Long) {
        container.scope.launch { container.history.clearRecents(envId) }
    }

    private fun errorCode(error: EnvError): String = when (error) {
        EnvError.NameEmpty -> ERR_EMPTY
        EnvError.NameTooLong -> ERR_LONG
        EnvError.NameTaken -> ERR_TAKEN
        EnvError.WorkLimitReached -> ERR_WORK
        EnvError.CustomLimitReached -> ERR_CUSTOM
        else -> error.toString()
    }

    private suspend fun warmIcons() {
        val d = data.filterNotNull().first()
        if (d.settings.iconMode != IconMode.NONE) {
            val env = d.current
            val keys = env?.let { d.items[it.id] }.orEmpty()
            withContext(Dispatchers.IO) {
                keys.forEach { key ->
                    d.apps[key]?.let { entry ->
                        container.icons.load(IconKey(key, entry.lastUpdateTime, homeIconPx()))
                    }
                }
            }
        }
        _iconsReady.value = true
    }

    private fun homeIconPx(): Int = (HOME_ICON_DP * container.densityScale()).toInt()

    private fun buildIndex(d: LauncherData): SearchIndex = SearchIndex.build(
        d.apps.values.map { entry ->
            val o = d.overrides[entry.key]
            SearchItem(entry.key, entry.label, o?.customLabel, o?.hidden == true)
        },
    )

    private fun homeState(d: LauncherData, role: Boolean): HomeUiState {
        val env = d.current
        val rows = env?.let { d.items[it.id] }.orEmpty().mapNotNull { key ->
            val entry = d.apps[key] ?: return@mapNotNull null
            HomeRow(
                key = key,
                label = d.overrides[key]?.customLabel ?: entry.label,
                dimmed = entry.isDimmed,
                work = entry.profile == ProfileType.WORK,
            )
        }
        return HomeUiState(
            rows = rows.toImmutableList(),
            iconMode = d.settings.iconMode,
            textSize = d.settings.homeTextSize,
            clockFormat = d.settings.clockFormat,
            showDate = d.settings.showDate,
            roleMissing = !role,
        )
    }

    private fun settingsState(
        d: LauncherData,
        gestures: Map<SwipeDirection, GestureAction>,
        role: Boolean,
        cap: Int,
    ): SettingsUiState {
        val label = { key: AppKey -> d.overrides[key]?.customLabel ?: d.apps[key]?.label }
        val apps = d.apps.values
            .map { PickerApp(it.key, label(it.key) ?: it.label, d.overrides[it.key]?.hidden == true) }
            .sortedWith(Alphabet.comparator { it.label })
        return SettingsUiState(
            settings = d.settings,
            envs = d.envs.map {
                EnvSummary(it.id, it.name, it.type, it.preset, it.id == d.current?.id)
            }.toImmutableList(),
            envItems = d.envs.associate { env ->
                env.id to d.items[env.id].orEmpty().map { key ->
                    EditorItem(
                        key = key,
                        label = label(key) ?: key.packageName,
                        hidden = d.overrides[key]?.hidden == true,
                        missing = key !in d.apps,
                    )
                }.toImmutableList()
            }.toImmutableMap(),
            apps = apps.toImmutableList(),
            renamed = d.overrides.values
                .filter { it.customLabel != null && it.key in d.apps }
                .map { RenamedApp(it.key, d.apps[it.key]?.label.orEmpty(), it.customLabel.orEmpty()) }
                .toImmutableList(),
            gestures = gestures.mapValues { (_, action) ->
                val key = (action as? GestureAction.LaunchApp)?.key
                GestureSummary(action, key?.let(label), key != null && key !in d.apps)
            }.toImmutableMap(),
            capacity = cap,
            isDefaultLauncher = role,
            versionName = versionName,
        )
    }

    private fun writeSnapshot(d: LauncherData, state: HomeUiState) {
        val env = d.current ?: return
        container.snapshots.write(
            HomeSnapshot(
                envId = env.id,
                palette = env.palette,
                iconMode = state.iconMode,
                textSize = state.textSize,
                clockFormat = state.clockFormat,
                showDate = state.showDate,
                rows = state.rows.map { SnapshotRow(it.key, it.label, it.dimmed, it.work) },
            ),
        )
    }

    companion object {
        private const val RECENTS_LIMIT = 10
        private const val SNAPSHOT_DEBOUNCE_MS = 500L
        private const val HOME_ICON_DP = 28f

        fun snapshotState(snapshot: HomeSnapshot?, role: Boolean): HomeUiState = if (snapshot == null) {
            HomeUiState(roleMissing = !role)
        } else {
            HomeUiState(
                rows = snapshot.rows.map { HomeRow(it.key, it.label, it.dimmed, it.work) }.toImmutableList(),
                iconMode = snapshot.iconMode,
                textSize = snapshot.textSize,
                clockFormat = snapshot.clockFormat,
                showDate = snapshot.showDate,
                roleMissing = !role,
            )
        }
    }
}
