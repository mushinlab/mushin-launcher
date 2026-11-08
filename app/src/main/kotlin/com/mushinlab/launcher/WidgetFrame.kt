package com.mushinlab.launcher

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.FrameLayout

class WidgetFrame(context: Context, private val widget: AppWidgetHostView) : FrameLayout(context) {
    private var reported: SizeF? = null

    init {
        addView(widget)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val height = View.MeasureSpec.getSize(heightMeasureSpec)
        val contentWidth = width.coerceAtLeast(1)
        val contentHeight = height.coerceAtLeast(1)
        widget.measure(
            View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(contentHeight, View.MeasureSpec.AT_MOST),
        )
        val density = resources.displayMetrics.density
        val size = SizeF(contentWidth / density, contentHeight / density)
        if (size != reported) {
            reported = size
            widget.updateAppWidgetSize(Bundle(), listOf(size))
        }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val left = (width - widget.measuredWidth) / 2
        val top = (height - widget.measuredHeight) / 2
        widget.layout(left, top, left + widget.measuredWidth, top + widget.measuredHeight)
    }
}
