package com.support.core.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.sqrt

class TagTextView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val textPaint = Paint().apply {
        textSize = 25f
        color = Color.WHITE
        isAntiAlias = true
    }

    private val rectPaint = Paint().apply {
        color = Color.RED
        isAntiAlias = true
    }

    private val shadowPaint = Paint().apply {
        setShadowLayer(5f, 0f, 0f, Color.GRAY)
        isAntiAlias = true
    }

    private val textRect = Rect()
    private val textBound = Rect()
    private val slideRect = Rect()

    var text: String = ""

    var color: Int
        set(value) {
            rectPaint.color = value
        }
        get() = rectPaint.color

    private val minPaddingLeft get() = max(paddingLeft, paddingTop)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = textPaint.measureText(text).toInt() + minPaddingLeft * 2
        textPaint.getTextBounds(text, 0, text.length, textBound)
        setMeasuredDimension(size, size)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val offset = (bottom - top) / 2 - textBound.height() / 2 - minPaddingLeft
        val textTop = offset + textBound.height() / 2
        textRect.set(
                minPaddingLeft,
                textTop,
                width - minPaddingLeft,
                textTop + textBound.height()
        )
        val slide = sqrt(2f) * width
        val delta = (slide - width) / 2 + 2
        slideRect.set(
                -delta.toInt(),
                offset - textBound.height() / 2 - paddingTop,
                width + delta.toInt(),
                offset + textBound.height() / 2 + paddingTop
        )
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.rotate(45f, measuredWidth.toFloat() / 2, measuredHeight.toFloat() / 2)
        canvas.drawRect(slideRect, shadowPaint)
        canvas.drawRect(slideRect, rectPaint)

        canvas.drawText(
                text,
                textRect.left.toFloat(),
                textRect.top.toFloat(),
                textPaint
        )
        canvas.restore()
    }
}