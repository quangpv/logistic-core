package com.support.location.map.marker

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

class IconDrawable(
    private val child: Drawable,
    private val padding: Int = 0,
    @ColorInt tint: Int? = null
) : Drawable() {
    init {
        if (tint != null) child.colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_ATOP)
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
        val boundWidth = bounds.width() - padding * 2
        val boundHeight = bounds.height() - padding * 2

        val ratio = intrinsicWidth.toFloat() / intrinsicHeight

        var width = boundWidth / ratio
        var height = boundHeight * ratio

        if (width > boundWidth) {
            height = boundHeight.toFloat()
            width = height * ratio

        } else if (height > boundHeight) {
            width = boundWidth.toFloat()
            height = width / ratio
        }

        val hafWidth = width / 2
        val hafHeight = height / 2

        setBounds(
            (bounds.centerX() - hafWidth).toInt(),
            (bounds.centerY() - hafHeight).toInt(),
            (bounds.centerX() + hafWidth).toInt(),
            (bounds.centerY() + hafHeight).toInt()
        )
    }
}


