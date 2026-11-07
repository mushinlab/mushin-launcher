package com.mushinlab.launcher

import android.appwidget.AppWidgetHost
import android.content.Context

class MushinWidgetHost(context: Context) : AppWidgetHost(context, HOST_ID) {
    fun releaseViews() = clearViews()

    private companion object {
        const val HOST_ID = 1
    }
}
