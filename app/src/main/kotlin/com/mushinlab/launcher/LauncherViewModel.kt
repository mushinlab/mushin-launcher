package com.mushinlab.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mushinlab.launcher.core.apps.LaunchResult
import com.mushinlab.launcher.core.apps.NotificationSource
import com.mushinlab.launcher.core.apps.icons.IconKey
import com.mushinlab.launcher.core.data.repo.SpaceError
import com.mushinlab.launcher.core.data.repo.SpaceOutcome
import com.mushinlab.launcher.core.data.repo.resolveCurrentSpace
import com.mushinlab.launcher.core.data.snapshot.HomeSnapshot
import com.mushinlab.launcher.core.data.snapshot.SnapshotRow
import com.mushinlab.launcher.core.model.AppEntry
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.AppOverride
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.Palette
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.model.ProfileType
import com.mushinlab.launcher.core.model.Space
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.model.swipesByApp
import com.mushinlab.launcher.core.search.Alphabet
import com.mushinlab.launcher.core.search.GestureLearning
import com.mushinlab.launcher.core.search.GestureSample
import com.mushinlab.launcher.core.search.SearchEngine
import com.mushinlab.launcher.core.search.SearchIndex
import com.mushinlab.launcher.core.search.SearchItem
import com.mushinlab.launcher.core.search.SearchOptions
import com.mushinlab.launcher.feature.home.HomeRow
import com.mushinlab.launcher.feature.home.HomeSuggestion
import com.mushinlab.launcher.feature.home.HomeUiState
import com.mushinlab.launcher.feature.home.PopupSpace
import com.mushinlab.launcher.feature.search.RecentRow
import com.mushinlab.launcher.feature.settings.ERR_CUSTOM
import com.mushinlab.launcher.feature.settings.ERR_EMPTY
import com.mushinlab.launcher.feature.settings.ERR_LONG
import com.mushinlab.launcher.feature.settings.ERR_TAKEN
import com.mushinlab.launcher.feature.settings.ERR_WORK
import com.mushinlab.launcher.feature.settings.EditorItem
import com.mushinlab.launcher.feature.settings.GestureSummary
import com.mushinlab.launcher.feature.settings.PickerApp
import com.mushinlab.launcher.feature.settings.RenamedApp
import com.mushinlab.launcher.feature.settings.SettingsUiState
import com.mushinlab.launcher.feature.settings.SpaceSummary
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
    val spaces: List<Space>,
    val current: Space?,
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
    private val notificationAccess = MutableStateFlow(false)
    private val capacity = MutableStateFlow(0)

    private val catalogMap: StateFlow<Map<AppKey, AppEntry>?> =
        combine(container.catalog.apps, container.catalog.loaded) { list, loaded ->
            if (loaded) list.associateBy { it.key } else null
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val data: StateFlow<LauncherData?> = combine(
        container.spaces.spaces,
        container.spaces.items,
        catalogMap,
        container.overrides.overrides,
        container.settings.loaded,
    ) { spaces, items, apps, overrides, settings ->
        if (apps == null || settings == null) {
            null
        } else {
            LauncherData(spaces, resolveCurrentSpace(spaces, settings.currentSpaceId), items, apps, overrides, settings)
        }
    }
        .filter { it != null && it.spaces.isNotEmpty() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val ready: StateFlow<Boolean> = data.map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val palette: StateFlow<Palette> = data.map { it?.current?.palette ?: initial?.palette ?: Palettes.Graphite }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, initial?.palette ?: Palettes.Graphite)

    val coloredIcons: StateFlow<Boolean> = data.map { it?.current?.coloredIcons == true }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val suggestion: StateFlow<HomeSuggestion?> = combine(
        data,
        container.history.spaceUsage,
        container.gestures.bindings,
        capacity,
    ) { d, usage, bindings, cap ->
        d?.let { suggestionFor(it, usage, bindings.swipesByApp().keys, cap, System.currentTimeMillis()) }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val home: StateFlow<HomeUiState> = combine(
        data,
        roleHeld,
        container.notificationCounts,
        container.gestures.bindings,
        suggestion,
    ) { d, role, counts, bindings, hint ->
        if (d == null) {
            snapshotState(initial, role)
        } else {
            homeState(d, role, counts, bindings.swipesByApp()).copy(suggestion = hint)
        }
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, snapshotState(initial, roleHeld.value))

    val popupSpaces: StateFlow<ImmutableList<PopupSpace>> = data.map { d ->
        d?.spaces?.map { PopupSpace(it.id, it.name, it.id == d.current?.id) }?.toImmutableList() ?: persistentListOf()
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
        val space = d?.current ?: return@combine persistentListOf()
        val favorites = d.items[space.id].orEmpty().toSet()
        recents[space.id].orEmpty()
            .asSequence()
            .filter { it.key !in favorites }
            .filter { d.overrides[it.key]?.hidden != true }
            .mapNotNull { r -> d.apps[r.key]?.let { RecentRow(r.key, d.overrides[r.key]?.customLabel ?: it.label) } }
            .take(RECENTS_LIMIT)
            .toList()
            .toImmutableList()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, persistentListOf())

    val settingsState: StateFlow<SettingsUiState> = combine(
        combine(data, container.gestures.bindings, roleHeld, capacity, notificationAccess) { d, g, role, cap, access ->
            if (d == null) {
                SettingsUiState(capacity = cap, isDefaultLauncher = role, versionName = versionName)
            } else {
                settingsState(d, g, role, cap).copy(notificationAccess = access)
            }
        },
        container.gestureLearning.samples,
        container.history.usage,
        container.history.spaceUsage,
    ) { state, samples, usage, spaces ->
        state.withMemory(
            samples,
            usage,
        ).withUsefulness(spaces[state.currentSpaceId].orEmpty(), System.currentTimeMillis())
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState(versionName = versionName))

    val gestureBindings = container.gestures.bindings

    val learnedGestures = container.gestureLearning.samples

    val memory = MemoryEditor(container)

    val spaces = SpaceEditor(container)

    val suggestions = SuggestionEditor(container) { id -> data.value?.items?.get(id).orEmpty() }

    private val _iconsReady = MutableStateFlow(false)
    val iconsReady: StateFlow<Boolean> = _iconsReady

    init {
        combine(home, palette) { state, _ -> state }
            .debounce(SNAPSHOT_DEBOUNCE_MS)
            .onEach { state -> data.value?.let { d -> writeSnapshot(d, state) } }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
        viewModelScope.launch { warmIcons() }
    }

    fun setAccess(roleHeld: Boolean, notificationAccess: Boolean) {
        this.roleHeld.value = roleHeld
        this.notificationAccess.value = notificationAccess
    }

    fun setCapacity(value: Int) {
        capacity.value = value
    }

    fun currentSpaceId(): Long? = data.value?.current?.id

    fun entry(key: AppKey): AppEntry? = data.value?.apps?.get(key)

    fun override(key: AppKey): AppOverride? = data.value?.overrides?.get(key)

    fun isInCurrentSpace(key: AppKey): Boolean {
        val d = data.value ?: return false
        return d.current?.let { key in d.items[it.id].orEmpty() } == true
    }

    val currentSpaceName: String? get() = data.value?.current?.name

    fun launch(
        key: AppKey,
        fromSearch: Boolean,
        animate: Boolean = true,
        gestures: List<GestureSample> = emptyList(),
    ): LaunchResult {
        val result = container.launcher.launch(key, animate = animate)
        if (result == LaunchResult.Started) {
            val spaceId = currentSpaceId()
            container.scope.launch { container.history.recordLaunch(key, spaceId, fromSearch) }
            if (gestures.isNotEmpty()) {
                container.scope.launch { container.gestureLearning.update { GestureLearning.learn(it, gestures, key) } }
            }
        }
        return result
    }

    fun switchSpace(id: Long) {
        container.scope.launch { container.settings.update { it.copy(currentSpaceId = id) } }
    }

    fun setActiveLayout(id: String) {
        container.scope.launch { container.settings.update { it.copy(activeKeyboardLayout = id) } }
    }

    fun setHidden(key: AppKey, hidden: Boolean) {
        container.scope.launch { container.overrides.setHidden(key, hidden) }
    }

    fun setSystemShortcut(key: AppKey, bound: Boolean) {
        container.scope.launch { container.overrides.setSystemShortcut(key, bound) }
    }

    fun rename(key: AppKey, label: String?) {
        container.scope.launch { container.overrides.rename(key, label) }
    }

    fun removeFromCurrentSpace(key: AppKey) {
        val spaceId = currentSpaceId() ?: return
        removeSpaceItem(spaceId, key)
    }

    fun addSpaceItem(spaceId: Long, key: AppKey): Boolean {
        val limit = itemLimit
        if (data.value?.items?.get(spaceId).orEmpty().size >= limit) return false
        container.scope.launch { container.spaces.addItems(spaceId, listOf(key), limit) }
        return true
    }

    fun removeSpaceItem(spaceId: Long, key: AppKey) {
        container.scope.launch { container.spaces.removeItem(spaceId, key) }
    }

    private val itemLimit: Int get() = capacity.value.takeIf { it > 0 } ?: Int.MAX_VALUE

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        container.scope.launch { container.settings.update(transform) }
    }

    suspend fun createSpace(name: String, type: SpaceType): String? =
        when (
            val result = container.spaces.create(
                name,
                type,
                if (type ==
                    SpaceType.WORK
                ) {
                    PalettePreset.NAVY
                } else {
                    PalettePreset.MOSS
                },
            )
        ) {
            is SpaceOutcome.Ok -> null
            is SpaceOutcome.Failed -> errorCode(result.error)
        }

    suspend fun renameSpace(id: Long, name: String): String? = when (
        val result = container.spaces.rename(
            id,
            name,
        )
    ) {
        is SpaceOutcome.Ok -> null
        is SpaceOutcome.Failed -> errorCode(result.error)
    }

    fun deleteSpace(id: Long) {
        container.scope.launch { container.spaces.delete(id) }
    }

    fun setSpaceItems(id: Long, keys: List<AppKey>) {
        container.scope.launch { container.spaces.setItems(id, keys) }
    }

    fun setGesture(direction: SwipeDirection, action: GestureAction) {
        container.scope.launch { container.gestures.set(direction, action) }
    }

    fun clearHistory(spaceId: Long) {
        container.scope.launch { container.history.clearRecents(spaceId) }
    }

    private fun errorCode(error: SpaceError): String = when (error) {
        SpaceError.NameEmpty -> ERR_EMPTY
        SpaceError.NameTooLong -> ERR_LONG
        SpaceError.NameTaken -> ERR_TAKEN
        SpaceError.WorkLimitReached -> ERR_WORK
        SpaceError.CustomLimitReached -> ERR_CUSTOM
        else -> error.toString()
    }

    private suspend fun warmIcons() {
        val d = data.filterNotNull().first()
        if (d.settings.iconMode != IconMode.NONE) {
            val space = d.current
            val keys = space?.let { d.items[it.id] }.orEmpty()
            withContext(Dispatchers.IO) {
                keys.forEach { key ->
                    d.apps[key]?.let { entry ->
                        container.icons.load(
                            IconKey(key, entry.lastUpdateTime, homeIconPx(), space?.coloredIcons == true),
                        )
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

    private fun homeState(
        d: LauncherData,
        role: Boolean,
        counts: Map<NotificationSource, Int>,
        swipes: Map<AppKey, List<SwipeDirection>>,
    ): HomeUiState {
        val space = d.current
        val rows = space?.let { d.items[it.id] }.orEmpty().mapNotNull { key ->
            val entry = d.apps[key] ?: return@mapNotNull null
            HomeRow(
                key = key,
                label = d.overrides[key]?.customLabel ?: entry.label,
                dimmed = entry.isDimmed,
                work = entry.profile == ProfileType.WORK,
                notifications = if (d.settings.notificationCounts) {
                    counts[NotificationSource(key.packageName, key.userSerial)] ?: 0
                } else {
                    0
                },
                swipes = swipes[key]?.toImmutableList() ?: persistentListOf(),
                shortcut = d.overrides[key]?.systemShortcut == true,
            )
        }
        return HomeUiState(
            rows = rows.toImmutableList(),
            iconMode = d.settings.iconMode,
            textSize = d.settings.homeTextSize,
            clockFormat = d.settings.clockFormat,
            showDate = d.settings.showDate,
            clockPosition = d.settings.clockPosition,
            homeListOffset = d.current?.listOffset ?: d.settings.homeListOffset,
            backgroundOpacity = d.settings.backgroundOpacity,
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
            spaces = d.spaces.map {
                SpaceSummary(it.id, it.name, it.type, it.preset, it.id == d.current?.id, it.coloredIcons)
            }.toImmutableList(),
            spaceItems = d.spaces.associate { space ->
                space.id to d.items[space.id].orEmpty().map { key ->
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
        val space = d.current ?: return
        container.snapshots.write(
            HomeSnapshot(
                spaceId = space.id,
                palette = space.palette,
                iconMode = state.iconMode,
                textSize = state.textSize,
                clockFormat = state.clockFormat,
                showDate = state.showDate,
                rows = state.rows.map { SnapshotRow(it.key, it.label, it.dimmed, it.work) },
                clockPosition = state.clockPosition,
                listOffset = state.homeListOffset,
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
                clockPosition = snapshot.clockPosition,
                homeListOffset = snapshot.listOffset,
                roleMissing = !role,
            )
        }
    }
}
