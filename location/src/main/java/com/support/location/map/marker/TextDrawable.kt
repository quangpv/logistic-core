package com.support.location.map.marker

import android.graphics.*
import android.graphics.drawable.Drawable
import android.text.TextPaint

class TextDrawable(
    private val text: String = "",
    private val textColor: Int = Color.BLACK,
    private val textSize:Float = 30f
) : Drawable() {
    private var mStartY: Float = 0f
    private var mStartX: Float = 0f

    private val mTextPaint: TextPaint = TextPaint().apply {
        color = textColor
        isAntiAlias = true
        textSize = this@TextDrawable.textSize
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        mStartX = bounds.exactCenterX() - mTextPaint.measureText(text) / 2
        mStartY = bounds.exactCenterY() - (mTextPaint.descent() + mTextPaint.ascent()) / 2
    }

    override fun draw(canvas: Canvas) {
        if (text.isNotBlank()) canvas.drawText(
            text,
            mStartX,
            mStartY,
            mTextPaint
        )
    }

    override fun setAlpha(alpha: Int) {
        mTextPaint.alpha = alpha
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mTextPaint.colorFilter = colorFilter
    }
}