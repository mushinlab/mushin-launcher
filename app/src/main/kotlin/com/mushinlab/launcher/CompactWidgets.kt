package com.mushinlab.launcher

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

internal fun compactProviders(context: Context): List<AppWidgetProviderInfo> =
    AppWidgetManager.getInstance(context).installedProviders.filter { fitsCompactWidget(context, it) }
        .sortedBy { it.loadLabel(context.packageManager).lowercase() }

internal fun fitsCompactWidget(context: Context, info: AppWidgetProviderInfo): Boolean {
    val density = context.resources.displayMetrics.density
    val width = (context.resources.configuration.screenWidthDp - PANEL_INSETS_DP - CELL_GAP_DP) / 2f
    val padding = android.appwidget.AppWidgetHostView.getDefaultPaddingForWidget(context, info.provider, null)
    val minWidth = if (info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0) {
        info.minResizeWidth
    } else {
        info.minWidth
    }
    val minHeight = if (info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0) {
        info.minResizeHeight
    } else {
        info.minHeight
    }
    return minWidth + padding.left + padding.right <= width * density &&
        minHeight + padding.top + padding.bottom <= CELL_HEIGHT_DP * density
}

private const val PANEL_INSETS_DP = 48
private const val CELL_GAP_DP = 8
private const val CELL_HEIGHT_DP = 112
