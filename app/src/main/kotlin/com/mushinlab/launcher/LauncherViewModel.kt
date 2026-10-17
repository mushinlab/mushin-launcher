package com.mushinlab.launcher

import android.content.ContentResolver
import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.runtime.mutableLongStateOf
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mushinlab.launcher.core.apps.icons.IconKey
import com.mushinlab.launcher.core.data.repo.resolveCurrentSpace
import com.mushinlab.launcher.core.data.snapshot.HomeSnapshot
import com.mushinlab.launcher.core.model.AppEntry
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.model.Palette
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.model.swipesByApp
import com.mushinlab.launcher.core.search.SearchEngine
import com.mushinlab.launcher.core.search.SearchIndex
import com.mushinlab.launcher.core.search.SearchOptions
import com.mushinlab.launcher.feature.home.HomeSuggestion
import com.mushinlab.launcher.feature.home.HomeUiState
import com.mushinlab.launcher.feature.settings.SettingsRouteCodec
import com.mushinlab.launcher.feature.settings.SettingsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class LauncherViewModel(
    private val container: AppContainer,
    contentResolver: ContentResolver,
    initial: HomeSnapshot?,
    private val versionName: String,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val roleHeld = MutableStateFlow(true)
    private val notificationAccess = MutableStateFlow(false)
    private val capacity = MutableStateFlow(0)
    private val effectChannel = Channel<LauncherEffect>(Channel.UNLIMITED)
    private val iconsWarm = MutableStateFlow(false)

    private val catalog: StateFlow<Map<AppKey, AppEntry>?> =
        combine(container.catalog.apps, container.catalog.loaded) { list, loaded ->
            if (loaded) list.associateBy { it.key } else null
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val data: StateFlow<LauncherData?> = combine(
        container.spaces.spaces,
        container.spaces.items,
        catalog,
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

    val effects: Flow<LauncherEffect> = effectChannel.receiveAsFlow()
    val minute = mutableLongStateOf(System.currentTimeMillis())
    val iconsReady: StateFlow<Boolean> = iconsWarm.asStateFlow()
    val settings = container.settings.settings
    val onboardingDone: StateFlow<Boolean?> = container.settings.loaded.map { it?.onboardingDone }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val usage = container.history.usage
    val gestureBindings = container.gestures.bindings
    val learnedGestures = container.gestureLearning.samples

    val spaces = SpaceEditor(container, { id -> data.value?.items?.get(id).orEmpty() }, { capacity.value })
    val apps = AppEditor(container)
    val preferences = PreferencesEditor(container)
    val memory = MemoryEditor(container)
    val suggestions = SuggestionEditor(container) { id -> data.value?.items?.get(id).orEmpty() }

    val model: LauncherModel = RepositoryLauncherModel(container, data, spaces, apps, preferences)
    val navigator = LauncherNavigator()
    val backgrounds = SpaceBackgroundEditor(container, contentResolver, viewModelScope, navigator) {
        effectChannel.trySend(it)
    }
    val launches = AppLaunches(model, navigator)
    val homeInput = HomeInteractions(model, navigator, launches) { effectChannel.trySend(it) }
    val searchInput = SearchInteractions(model, navigator, launches) { effectChannel.trySend(it) }

    val ready: StateFlow<Boolean> = data.map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val palette: StateFlow<Palette> = data.map { it?.current?.palette ?: initial?.palette ?: Palettes.Graphite }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, initial?.palette ?: Palettes.Graphite)

    val background: StateFlow<Bitmap?> = combine(container.spaces.spaces, container.settings.loaded) { spaces, s ->
        s?.let { resolveCurrentSpace(spaces, it.currentSpaceId)?.id }
    }
        .combine(container.backgrounds.versions) { id, versions -> id?.let { it to versions[it] } }
        .distinctUntilChanged()
        .mapLatest { current -> current?.takeIf { it.second != null }?.let { container.backgrounds.load(it.first) } }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

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
        d?.let { homeState(it, role, counts, bindings.swipesByApp()).copy(suggestion = hint) }
            ?: snapshotState(initial, role)
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, snapshotState(initial, roleHeld.value))

    val popupSpaces = data.map(::popupSpaces).stateIn(viewModelScope, SharingStarted.Eagerly, popupSpaces(null))

    val searchIndex: StateFlow<SearchIndex> = data
        .map { d -> d?.let(::searchIndex) ?: SearchIndex.EMPTY }
        .distinctUntilChanged { a, b -> a === b }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, SearchIndex.EMPTY)

    val searchEngine: StateFlow<SearchEngine> = container.settings.settings
        .map { SearchOptions(fuzzy = it.searchFuzzy, layoutSwap = it.searchLayoutSwap, translit = it.searchTranslit) }
        .distinctUntilChanged()
        .map { SearchEngine(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SearchEngine())

    val recents = combine(data, container.history.recents, ::recentKeys)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, recentKeys(null, emptyMap()))

    val settingsState: StateFlow<SettingsUiState> = combine(
        combine(data, container.gestures.bindings, roleHeld, capacity, notificationAccess) { d, g, role, cap, access ->
            d?.let { settingsState(it, g, role, cap, versionName).copy(notificationAccess = access) }
                ?: SettingsUiState(capacity = cap, isDefaultLauncher = role, versionName = versionName)
        },
        container.gestureLearning.samples,
        container.history.usage,
        container.history.spaceUsage,
        container.backgrounds.versions,
    ) { state, samples, usage, spaceUsage, backgrounds ->
        state.withMemory(samples, usage)
            .withUsefulness(spaceUsage[state.currentSpaceId].orEmpty(), System.currentTimeMillis())
            .withBackgrounds(backgrounds)
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState(versionName = versionName))

    init {
        savedState.get<Bundle>(SETTINGS_ROUTES)
            ?.getStringArrayList(SETTINGS_ROUTES)
            ?.mapNotNull(SettingsRouteCodec::decode)
            ?.let(navigator.settings::restore)
        savedState.setSavedStateProvider(SETTINGS_ROUTES) {
            bundleOf(SETTINGS_ROUTES to ArrayList(navigator.settings.routes.map(SettingsRouteCodec::encode)))
        }
        combine(home, palette) { state, _ -> state }
            .debounce(SNAPSHOT_DEBOUNCE_MS)
            .onEach { state -> data.value?.let { homeSnapshot(it, state) }?.let(container.snapshots::write) }
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

    fun request(effect: LauncherEffect) {
        effectChannel.trySend(effect)
    }

    private suspend fun warmIcons() {
        val d = data.filterNotNull().first()
        if (d.settings.iconMode != IconMode.NONE) {
            val coloured = d.current?.coloredIcons == true
            val sizePx = (HOME_ICON_DP * container.densityScale()).toInt()
            withContext(Dispatchers.IO) {
                d.currentItems().forEach { key ->
                    d.apps[key]?.let { container.icons.load(IconKey(key, it.lastUpdateTime, sizePx, coloured)) }
                }
            }
        }
        iconsWarm.value = true
    }

    private companion object {
        const val SNAPSHOT_DEBOUNCE_MS = 500L
        const val HOME_ICON_DP = 28f
        const val SETTINGS_ROUTES = "settings_routes"
    }
}
