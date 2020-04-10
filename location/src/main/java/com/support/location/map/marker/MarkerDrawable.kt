package com.support.location.map.marker

import android.graphics.*
import android.graphics.drawable.Drawable

class ShadowOptions(
    val radius: Float = 5f,
    val color: Int = Color.GRAY
)

class IconShadowSize(
    val width: Int,
    val height: Int
)

class MarkerIconDrawable(
        private val child: Drawable? = null,
        private val padding: Int = 0,
        private val sweepDegree: Int = 30,
        private val color: Int = Color.WHITE,
        private val shadow: ShadowOptions? = ShadowOptions(),
        private val iconShadowSize: IconShadowSize = IconShadowSize(35, 20)
) : Drawable() {

    private lateinit var mShadowRect: RectF
    private val mShadowPaint = Paint().also {
        it.isAntiAlias = true
        it.color = Color.GRAY //Color.parseColor("#AADDDDDD")
        it.style = Paint.Style.FILL
        if (shadow != null) it.maskFilter =
            BlurMaskFilter(shadow.radius, BlurMaskFilter.Blur.NORMAL)
    }
    private val mPaint = Paint().also {
        it.isAntiAlias = true
        it.color = color
        it.style = Paint.Style.FILL
        if (shadow != null) it.setShadowLayer(shadow.radius, 0f, 0f, shadow.color)
    }

    private var mPath = Path()

    override fun onBoundsChange(bounds: Rect) {
        val translate = shadow?.radius?.toInt() ?: 0
        mPath.makeMarker(
            sweepDegree.toFloat(),
            bounds.left + translate.toFloat(),
            bounds.top + translate.toFloat(),
            bounds.width().toFloat() - translate * 2,
            bounds.height().toFloat() - translate * 3,
            true
        )
        mShadowRect = RectF(
            bounds.exactCenterX() - iconShadowSize.width,
            bounds.bottom - iconShadowSize.height - translate.toFloat(),
            bounds.exactCenterX() + iconShadowSize.width,
            bounds.bottom.toFloat() - translate
        )

        child?.setBounds(
            bounds.left + padding + translate,
            bounds.top + padding + translate,
            bounds.right - padding - translate,
            bounds.bottom - padding - translate
        )
    }

    override fun draw(canvas: Canvas) {
        canvas.drawArc(mShadowRect, 0f, 360f, true, mShadowPaint)
        canvas.drawPath(mPath, mPaint)
        child?.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    private fun Path.makeMarker(
        sweep: Float,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        smoothLine: Boolean
    ) {
        reset()
        val middle = left + width / 2
        val bottom = top + height
        val startAngle = sweep / 2 + 90
        val sweepAngle = 360 - sweep

        arcTo(
            RectF(left, top, left + width, top + width),
            startAngle,
            sweepAngle
        )
        if (smoothLine) {
            lineTo(middle + 3, bottom - 5)
            quadTo(middle, bottom, middle - 3, bottom - 5)
        } else {
            lineTo(middle, bottom)
        }
        close()
    }
}