package com.mushinlab.launcher

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.util.SizeF
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun HostedWidgetView(id: Int, host: MushinWidgetHost, height: Dp, maxWidth: Dp) {
    val context = LocalContext.current
    val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(id) ?: return
    key(id) {
        AndroidView(
            factory = { WidgetFrame(it, host.createView(it, id, info)) },
            modifier = Modifier.width(maxWidth).height(height),

        )
    }
}
