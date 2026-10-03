package com.bluecubelabs.env

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Trace
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bluecubelabs.env.core.apps.LaunchResult
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.GestureAction
import com.bluecubelabs.env.core.model.Palette
import com.bluecubelabs.env.core.model.SwipeDirection
import com.bluecubelabs.env.core.search.KeyboardLayouts
import com.bluecubelabs.env.feature.search.HardwareKey
import com.bluecubelabs.env.feature.search.KeyboardRouter
import com.bluecubelabs.env.feature.search.SearchController
import com.bluecubelabs.env.feature.search.SearchMode
import com.bluecubelabs.env.feature.settings.SettingsStack

class MainActivity : ComponentActivity() {
    private val container get() = (application as EnvApp).container
    private lateinit var viewModel: LauncherViewModel

    val minute = mutableLongStateOf(System.currentTimeMillis())
    var overlay by mutableStateOf<Overlay>(Overlay.None)
    var searchMode by mutableStateOf(SearchMode.TYPING)
    var renameTarget by mutableStateOf<AppKey?>(null)
    var notice by mutableStateOf<Notice?>(null)
    val search = SearchController()
    val settingsStack = SettingsStack()
    var searchTracePending = false
    private var shadeWarned = false
    private var lastBarsDark: Boolean? = null

    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshRole()
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
            LauncherRoot(this, viewModel, LauncherIconProvider(container.icons, container.catalog))
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
        refreshRole()
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(timeReceiver)
        if (overlay != Overlay.None) closeOverlays()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleHomeIntent(intent)
    }

    fun handleHomeIntent(intent: Intent) {
        val isHome = intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)
        if (isHome && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) closeAll()
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

    private fun refreshRole() {
        val held = getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) ?: true
        viewModel.setRoleHeld(held)
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
            overlay is Overlay.Popup -> overlay = Overlay.None
            overlay == Overlay.Search -> closeOverlays()
            settingsStack.isOpen -> settingsStack.pop()
        }
    }

    fun launch(key: AppKey, fromSearch: Boolean) {
        val entry = viewModel.entry(key)
        when (viewModel.launch(key, fromSearch)) {
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
    }

    fun launchSearchTarget() {
        val target = search.target()
        if (target == null) {
            window.decorView.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            launch(target.key, fromSearch = true)
        }
    }

    fun onSwipe(direction: SwipeDirection) {
        when (val action = viewModel.gestureBindings.value[direction] ?: GestureAction.None) {
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
                launch(action.key, fromSearch = false)
            }
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
