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
        editor().onPicked(result.resultCode == Activity.RESULT_OK)
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

    fun bind(id: Int, info: AppWidgetProviderInfo) {
        val manager = AppWidgetManager.getInstance(activity)
        try {
            if (manager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null)) {
                editor().onPicked(true)
            } else {
                picker.launch(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
                    },
                )
            }
        } catch (_: ActivityNotFoundException) {
            editor().unavailable()
        } catch (_: SecurityException) {
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
        const val CONFIGURE_REQUEST = 9081
    }
}
