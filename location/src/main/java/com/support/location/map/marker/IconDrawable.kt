package com.support.location.map.marker

import android.graphics.*
import android.graphics.drawable.Drawable

class IconDrawable(
        private val child: Drawable,
        private val padding: Int = 0,
        tint: Int = -1
) : Drawable() {
    init {
        if (tint != -1) child.colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_ATOP)
    }

    override fun draw(canvas: Canvas) {
        child.draw(canvas)
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        child.scaleFit(bounds, padding)
    }

    override fun setAlpha(alpha: Int) {
        child.alpha = alpha
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        child.colorFilter = colorFilter
    }

    private fun Drawable.scaleFit(bounds: Rect, padding: Int) {
        val maxBoundSize = bounds.width().coerceAtLeast(bounds.height())
        val maxSize = intrinsicWidth.coerceAtLeast(intrinsicHeight)
        val ratio = maxBoundSize.toFloat() / maxSize
        val width = (intrinsicWidth * ratio).toInt()
        val height = (intrinsicHeight * ratio).toInt()
        setBounds(
                bounds.centerX() - width / 2 + padding,
                bounds.centerY() - height / 2 + padding,
                bounds.centerX() + width / 2 - padding,
                bounds.centerY() + height / 2 - padding
        )
    }
}


