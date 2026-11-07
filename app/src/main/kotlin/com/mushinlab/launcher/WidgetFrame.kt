package com.mushinlab.launcher

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.FrameLayout

class WidgetFrame(context: Context, private val widget: AppWidgetHostView) : FrameLayout(context) {
    init {
        addView(widget)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val height = View.MeasureSpec.getSize(heightMeasureSpec)
        val info = widget.appWidgetInfo
        val scale = minOf(
            1f,
            width.toFloat() / info.minWidth.coerceAtLeast(1),
            height.toFloat() / info.minHeight.coerceAtLeast(1),
        )
            .coerceAtLeast(MIN_SCALE)
        val contentWidth = (width / scale).toInt().coerceAtLeast(1)
        val contentHeight = (height / scale).toInt().coerceAtLeast(1)
        widget.measure(
            View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(contentHeight, View.MeasureSpec.EXACTLY),
        )
        widget.pivotX = 0f
        widget.pivotY = 0f
        widget.scaleX = scale
        widget.scaleY = scale
        val density = resources.displayMetrics.density
        widget.updateAppWidgetSize(Bundle(), listOf(SizeF(contentWidth / density, contentHeight / density)))
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        widget.layout(0, 0, widget.measuredWidth, widget.measuredHeight)
    }

    private companion object {
        const val MIN_SCALE = 0.01f
    }
}
