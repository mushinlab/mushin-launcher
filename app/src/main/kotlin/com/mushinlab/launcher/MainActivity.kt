package com.mushinlab.launcher

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mushinlab.launcher.feature.search.KeyboardRouter
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val container get() = (application as MushinApp).container
    private val systemScreens = SystemScreens(this) { launcher.navigator.notice = it }
    private val systemBars = SystemBars(this)

    internal val launcher: LauncherViewModel by viewModels {
        viewModelFactory {
            initializer {
                LauncherViewModel(
                    container = container,
                    contentResolver = applicationContext.contentResolver,
                    initial = container.snapshots.read(),
                    versionName = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty(),
                    savedState = createSavedStateHandle(),
                )
            }
        }
    }

    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshAccess()
    }

    private val backgroundPicker = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            launcher.preferences.setBackground(uri) {
                launcher.navigator.notice = Notice(R.string.notice_background_failed)
            }
        }
    }

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            launcher.minute.longValue = System.currentTimeMillis()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        systemBars.apply(launcher.palette.value)
        window.isNavigationBarContrastEnforced = false
        setContent { LauncherRoot(launcher, container.iconProvider) }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { launcher.effects.collect(::perform) }
                launch { launcher.palette.collect(systemBars::apply) }
            }
        }
        if (intent?.action == Intent.ACTION_ASSIST) launcher.navigator.onHomeIntent(assist = true)
    }

    override fun onStart() {
        super.onStart()
        launcher.minute.longValue = System.currentTimeMillis()
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
        launcher.homeInput.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleHomeIntent(intent)
    }

    fun handleHomeIntent(intent: Intent) {
        val home = intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)
        val assist = intent.action == Intent.ACTION_ASSIST
        if (home || assist) launcher.navigator.onHomeIntent(assist)
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val key = KeyboardRouter.map(event.keyCode, event.getUnicodeChar(event.metaState), event.isCtrlPressed)
        if (key == null || !launcher.searchInput.accepts(key)) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_DOWN) launcher.searchInput.onHardwareKey(key)
        return true
    }

    private fun perform(effect: LauncherEffect) {
        when (effect) {
            LauncherEffect.Reject -> window.decorView.performHapticFeedback(HapticFeedbackConstants.REJECT)

            LauncherEffect.RequestHomeRole -> requestHomeRole()

            LauncherEffect.PickBackground -> backgroundPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))

            LauncherEffect.OpenClock -> systemScreens.openClock()

            LauncherEffect.OpenNotificationAccess -> systemScreens.openNotificationAccess(
                container.notificationListener,
            )

            LauncherEffect.OpenAssistantSettings -> systemScreens.openAssistantSettings()

            LauncherEffect.RequestSearchTile -> systemScreens.requestSearchTile()
        }
    }

    private fun requestHomeRole() {
        val roleManager = getSystemService(RoleManager::class.java) ?: return
        roleRequest.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
    }

    private fun refreshAccess() {
        val held = getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) ?: true
        launcher.setAccess(roleHeld = held, notificationAccess = container.hasNotificationAccess())
    }
}
