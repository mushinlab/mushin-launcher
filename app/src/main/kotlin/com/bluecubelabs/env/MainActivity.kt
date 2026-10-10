package com.bluecubelabs.env

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Trace
import android.provider.AlarmClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bluecubelabs.env.core.apps.LaunchResult
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.GestureAction
import com.bluecubelabs.env.core.model.Palette
import com.bluecubelabs.env.core.model.SwipeDirection
import com.bluecubelabs.env.core.search.GestureDecoder
import com.bluecubelabs.env.core.search.GesturePoint
import com.bluecubelabs.env.core.search.KeyboardLayouts
import com.bluecubelabs.env.feature.search.HardwareKey
import com.bluecubelabs.env.feature.search.KeyboardDimens
import com.bluecubelabs.env.feature.search.KeyboardRouter
import com.bluecubelabs.env.feature.search.SearchController
import com.bluecubelabs.env.feature.search.SearchMenu
import com.bluecubelabs.env.feature.search.SearchMode
import com.bluecubelabs.env.feature.search.glideCenters
import com.bluecubelabs.env.feature.settings.SettingsRoute
import com.bluecubelabs.env.feature.settings.SettingsStack

class MainActivity : ComponentActivity() {
    private val container get() = (application as EnvApp).container
    private lateinit var viewModel: LauncherViewModel

    val minute = mutableLongStateOf(System.currentTimeMillis())
    var overlay by mutableStateOf<Overlay>(Overlay.None)
    var searchMode by mutableStateOf(SearchMode.TYPING)
    var renameTarget by mutableStateOf<AppKey?>(null)
    var notice by mutableStateOf<Notice?>(null)
    var homeExit by mutableStateOf<SwipeDirection?>(null)
        private set
    private var pendingSwipeLaunch: AppKey? = null
    val search = SearchController()
    val iconProvider by lazy { LauncherIconProvider(container.icons, container.catalog) }
    val settingsStack = SettingsStack()
    var searchTracePending = false
    private var shadeWarned = false
    private var lastBarsDark: Boolean? = null

    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshAccess()
    }

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            minute.longValue = System.currentTimeMillis()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val snapshot = container.snapshots.read()
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LauncherViewModel(container, snapshot, versionName) as T
            },
        )[LauncherViewModel::class.java]
        applySystemBars(viewModel.palette.value)
        window.isNavigationBarContrastEnforced = false
        setContent {
            LauncherRoot(this, viewModel, iconProvider)
        }
    }

    override fun onStart() {
        super.onStart()
        minute.longValue = System.currentTimeMillis()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(this, timeReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onResume() {
        super.onResume()
        refreshAccess()
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(timeReceiver)
        homeExit = null
        pendingSwipeLaunch = null
        if (overlay != Overlay.None) closeOverlays()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleHomeIntent(intent)
    }

    fun handleHomeIntent(intent: Intent) {
        val isHome = intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)
        if (isHome) closeAll()
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (settingsStack.isOpen || renameTarget != null) return super.dispatchKeyEvent(event)
        val key = KeyboardRouter.map(event.keyCode, event.getUnicodeChar(event.metaState), event.isCtrlPressed)
            ?: return super.dispatchKeyEvent(event)
        if (overlay != Overlay.Search && key !is HardwareKey.Text) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_DOWN) handleHardwareKey(key)
        return true
    }

    fun applySystemBars(palette: Palette) {
        if (lastBarsDark == palette.isDark) return
        lastBarsDark = palette.isDark
        val transparent = android.graphics.Color.TRANSPARENT
        val style = if (palette.isDark) {
            SystemBarStyle.dark(transparent)
        } else {
            SystemBarStyle.light(transparent, transparent)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    fun requestRole() {
        val roleManager = getSystemService(RoleManager::class.java) ?: return
        roleRequest.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
    }

    private fun refreshAccess() {
        val held = getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) ?: true
        viewModel.setAccess(roleHeld = held, notificationAccess = container.hasNotificationAccess())
    }

    fun openSearch(mode: SearchMode, firstChar: Char? = null) {
        Trace.beginAsyncSection(SEARCH_TRACE, SEARCH_TRACE_COOKIE)
        searchTracePending = true
        search.reset()
        firstChar?.let { search.type(it) }
        searchMode = mode
        overlay = Overlay.Search
    }

    fun onSearchDrawn() {
        if (searchTracePending) {
            searchTracePending = false
            Trace.endAsyncSection(SEARCH_TRACE, SEARCH_TRACE_COOKIE)
        }
    }

    fun closeOverlays() {
        overlay = Overlay.None
        search.reset()
    }

    fun closeAll() {
        closeOverlays()
        settingsStack.close()
        renameTarget = null
    }

    fun handleBack() {
        when {
            renameTarget != null -> renameTarget = null
            overlay is Overlay.Popup || overlay is Overlay.Suggestion -> overlay = Overlay.None
            search.menu != null -> search.closeMenu()
            search.options != null -> search.closeOptions()
            overlay == Overlay.Search -> closeOverlays()
            settingsStack.isOpen -> settingsStack.pop()
        }
    }

    fun launch(key: AppKey, fromSearch: Boolean, animate: Boolean = true): Boolean {
        val entry = viewModel.entry(key)
        val gestures = if (fromSearch) search.gesturesToLearn() else emptyList()
        val result = viewModel.launch(key, fromSearch, animate, gestures)
        when (result) {
            LaunchResult.Started -> if (fromSearch) closeOverlays()

            LaunchResult.UnlockRequested -> Unit

            LaunchResult.Suspended -> notice = Notice(getString(R.string.notice_suspended))

            LaunchResult.Failed -> notice = if (viewModel.isInCurrentEnv(key)) {
                Notice(
                    text = getString(R.string.notice_launch_failed),
                    actionLabel = getString(R.string.notice_remove_from_env),
                    action = { viewModel.removeFromCurrentEnv(key) },
                )
            } else {
                Notice(getString(R.string.notice_launch_failed))
            }
        }
        if (entry == null && !fromSearch) notice = Notice(getString(R.string.notice_launch_failed))
        return result == LaunchResult.Started
    }

    fun launchSearchTarget() {
        val target = search.target()
        if (target == null) {
            window.decorView.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            launch(target.key, fromSearch = true)
        }
    }

    fun onSwipe(direction: SwipeDirection, fromShadeZone: Boolean) {
        val action = if (fromShadeZone) {
            GestureAction.Notifications
        } else {
            viewModel.gestureBindings.value[direction] ?: GestureAction.None
        }
        when (action) {
            GestureAction.None -> Unit

            GestureAction.Search -> openSearch(SearchMode.TYPING)

            GestureAction.AllApps -> openSearch(SearchMode.BROWSING)

            GestureAction.Notifications -> if (!container.shade.expand() && !shadeWarned) {
                shadeWarned = true
                notice = Notice(getString(R.string.notice_shade_unavailable))
            }

            is GestureAction.LaunchApp -> if (viewModel.entry(action.key) !=
                null
            ) {
                pendingSwipeLaunch = action.key
                homeExit = direction
            }
        }
    }

    fun completeSwipeLaunch() {
        val key = pendingSwipeLaunch ?: return
        pendingSwipeLaunch = null
        if (!launch(key, fromSearch = false, animate = false)) homeExit = null
    }

    fun openSearchMenu(key: AppKey, label: String, anchor: Offset) {
        val entry = viewModel.entry(key) ?: return
        search.openMenu(
            SearchMenu(
                key,
                label,
                anchor,
                viewModel.isInCurrentEnv(key),
                !entry.isSystem,
                viewModel.override(key)?.hidden == true,
            ),
        )
    }

    fun toggleHidden(key: AppKey, label: String, hidden: Boolean) {
        search.closeMenu()
        viewModel.setHidden(key, hidden)
        notice = Notice(getString(if (hidden) R.string.notice_hidden else R.string.notice_unhidden, label))
    }

    fun toggleInEnv(key: AppKey, add: Boolean) {
        search.closeMenu()
        val env = viewModel.currentEnvName ?: return
        val envId = viewModel.currentEnvId() ?: return
        notice = when {
            !add -> {
                viewModel.removeFromCurrentEnv(key)
                Notice(getString(R.string.notice_removed_from_env, env))
            }

            viewModel.addEnvItem(envId, key) -> Notice(getString(R.string.notice_added_to_env, env))

            else -> Notice(getString(R.string.notice_env_full, env))
        }
    }

    fun openClock() {
        try {
            startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            notice = Notice(getString(R.string.notice_no_clock))
        }
    }

    fun editCurrentSpace() {
        val envId = viewModel.currentEnvId() ?: return
        overlay = Overlay.None
        settingsStack.open(SettingsRoute.EnvEditor(envId))
    }

    fun onHomeDrawing(points: List<Offset>, width: Float) {
        if (overlay != Overlay.None || points.size < 2 || width <= 0f) return
        val density = resources.displayMetrics.density
        val rowHeight = KeyboardDimens.rowHeight.value * density
        val padding = KeyboardDimens.verticalPadding.value * density
        val centers = glideCenters(width, rowHeight, padding, KeyboardDimens.gap.value * density)
        val shift = (points.minOf { it.y } + points.maxOf { it.y }) / 2 -
            (padding + rowHeight * KeyboardLayouts.LETTER_ROWS / 2)
        val settings = viewModel.settings.value
        val decoder = GestureDecoder(
            centers.mapValues { (_, center) -> GesturePoint(center.x, center.y + shift) },
            settings.gestureShiftPenalty,
            settings.gestureSuggestMargin,
        )
        openSearch(SearchMode.TYPING)
        if (!search.applyGesture(decoder, points.map { GesturePoint(it.x, it.y) })) {
            closeOverlays()
            window.decorView.performHapticFeedback(HapticFeedbackConstants.REJECT)
        }
    }

    fun openNotificationAccess() {
        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                container.notificationListener.flattenToString(),
            )
        try {
            startActivity(detail)
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    fun switchLayout() {
        val settings = viewModel.settings.value
        val enabled = settings.keyboardLayouts.ifEmpty { listOf(KeyboardLayouts.EN.id) }
        val index = enabled.indexOf(settings.activeKeyboardLayout)
        viewModel.setActiveLayout(enabled[(index + 1).mod(enabled.size)])
    }

    private fun handleHardwareKey(key: HardwareKey) {
        if (overlay != Overlay.Search) {
            if (key is HardwareKey.Text) openSearch(SearchMode.BROWSING, key.char)
            return
        }
        when (key) {
            is HardwareKey.Text -> if (!search.type(key.char)) {
                window.decorView.performHapticFeedback(HapticFeedbackConstants.REJECT)
            }

            HardwareKey.Backspace -> search.backspace()

            HardwareKey.Enter -> launchSearchTarget()

            HardwareKey.Escape -> closeOverlays()

            HardwareKey.Up -> search.moveSelection(1)

            HardwareKey.Down -> search.moveSelection(-1)

            HardwareKey.SwitchLayout -> switchLayout()
        }
    }

    companion object {
        const val SEARCH_TRACE = "search_open"
        private const val SEARCH_TRACE_COOKIE = 7
    }
}
