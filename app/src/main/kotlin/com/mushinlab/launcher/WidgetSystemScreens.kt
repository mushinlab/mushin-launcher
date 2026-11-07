package com.mushinlab.launcher

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class WidgetSystemScreens(
    private val activity: ComponentActivity,
    private val host: () -> MushinWidgetHost,
    private val editor: () -> WidgetEditor,
) {
    private val picker = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        editor().onPicked(result.resultCode == Activity.RESULT_OK, result.data?.getBooleanExtra(CLOCK, false) == true)
    }

    init {
        activity.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                activity.lifecycleScope.launch { editor().refresh() }
            }

            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                host().releaseViews()
            }
        })
    }

    fun pick(id: Int) {
        val clock = AppWidgetProviderInfo().apply {
            provider = ComponentName(activity, "builtin.clock")
            label = activity.getString(R.string.widget_time)
            icon = com.mushinlab.launcher.core.ui.R.drawable.ic_clock
        }
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, arrayListOf(clock))
            putParcelableArrayListExtra(
                AppWidgetManager.EXTRA_CUSTOM_EXTRAS,
                arrayListOf(Bundle().apply { putBoolean(CLOCK, true) }),
            )
        }
        try {
            picker.launch(intent)
        } catch (_: ActivityNotFoundException) {
            editor().unavailable()
        }
    }

    fun configure(id: Int) {
        try {
            host().startAppWidgetConfigureActivityForResult(activity, id, 0, CONFIGURE_REQUEST, null)
        } catch (_: ActivityNotFoundException) {
            editor().unavailable()
        } catch (_: SecurityException) {
            editor().unavailable()
        }
    }

    fun onResult(requestCode: Int, resultCode: Int) {
        if (requestCode == CONFIGURE_REQUEST) editor().onConfigured(resultCode == Activity.RESULT_OK)
    }

    private companion object {
        const val CLOCK = "mushin_clock"
        const val CONFIGURE_REQUEST = 9081
    }
}
