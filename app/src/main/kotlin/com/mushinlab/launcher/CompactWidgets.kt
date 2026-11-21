package com.mushinlab.launcher

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import com.mushinlab.launcher.core.model.WidgetCell
import kotlin.math.ceil

internal fun compactProviders(context: Context): List<AppWidgetProviderInfo> =
    AppWidgetManager.getInstance(context).installedProviders.filter { fitsCompactWidget(context, it) }
        .sortedBy { it.loadLabel(context.packageManager).lowercase() }

internal fun fitsCompactWidget(context: Context, info: AppWidgetProviderInfo): Boolean =
    compactSpan(context, info) != null

internal fun compactSpan(context: Context, info: AppWidgetProviderInfo): WidgetCell? {
    val density = context.resources.displayMetrics.density
    val gapsWidth = CELL_GAP_DP * (WidgetCell.COLUMNS - 1)
    val cellWidth = (context.resources.configuration.screenWidthDp - PANEL_INSETS_DP - gapsWidth) / WidgetCell.COLUMNS
    val panelHeight = COARSE_HEIGHT_DP * 2 + CELL_GAP_DP
    val cellHeight = (panelHeight - CELL_GAP_DP * (WidgetCell.ROWS - 1)) / WidgetCell.ROWS
    val padding = AppWidgetHostView.getDefaultPaddingForWidget(context, info.provider, null)
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
    val columns = cells((minWidth + padding.left + padding.right) / density, cellWidth.toFloat())
    val rows = cells((minHeight + padding.top + padding.bottom) / density, cellHeight.toFloat())
    return WidgetCell(0, 0, columns, rows).takeIf { it.fits }
}

private fun cells(sizeDp: Float, cellDp: Float): Int =
    ceil((sizeDp + CELL_GAP_DP) / (cellDp + CELL_GAP_DP)).toInt().coerceAtLeast(1)

private const val PANEL_INSETS_DP = 48
private const val CELL_GAP_DP = 8
private const val COARSE_HEIGHT_DP = 112
