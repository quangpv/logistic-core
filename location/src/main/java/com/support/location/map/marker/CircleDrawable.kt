package com.support.location.map.marker

import android.graphics.*
import android.graphics.drawable.Drawable

class CircleDrawable(
    private val child: Drawable? = null,
    private val padding: Int = 0,
    private val color: Int = Color.WHITE
) : Drawable() {

    private val mCirclePaint = Paint().also {
        it.color = color
        it.style = Paint.Style.FILL
        it.isAntiAlias = true
    }

    private val mClipPath = Path()

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
            bounds.width().toFloat() / 2,
            Path.Direction.CW
        )
    }

    override fun draw(canvas: Canvas) {
        canvas.clipPath(mClipPath)
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