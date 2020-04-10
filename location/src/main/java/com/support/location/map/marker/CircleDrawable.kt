package com.support.location.map.marker

import android.graphics.*
import android.graphics.drawable.Drawable

class CircleDrawable(
    private val child: Drawable? = null,
    private val padding: Int = 0,
    private val color: Int = Color.WHITE,
    private val shadow: ShadowOptions? = null
) : Drawable() {
    private val mCirclePaint = Paint().also {
        it.color = color
        it.style = Paint.Style.FILL
        it.isAntiAlias = true
        if (hasShadow) it.setShadowLayer(shadow!!.radius, 0f, 0f, shadow.color)
    }

    private val mClipPath = Path()
    private val hasShadow get() = shadow != null

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, top + right - left)
    }

    override fun onBoundsChange(bounds: Rect) {
        child?.setBounds(
            bounds.left + padding,
            bounds.top + padding,
            bounds.right - padding,
            bounds.bottom - padding
        )
        mClipPath.reset()
        mClipPath.addCircle(
            bounds.centerX().toFloat(),
            bounds.centerY().toFloat(),
            bounds.width().toFloat() / 2 - (shadow?.radius ?: 0f),
            Path.Direction.CW
        )
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPath(mClipPath, mCirclePaint)
        child?.draw(canvas)
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setAlpha(alpha: Int) {
        mCirclePaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mCirclePaint.colorFilter = colorFilter
    }
}