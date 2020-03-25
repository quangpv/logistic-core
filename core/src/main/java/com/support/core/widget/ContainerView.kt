package com.support.core.widget

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.support.core.R

interface Visible {
    fun enter(view: View) {}

    fun exit(view: View) {}

    fun popEnter(view: View) {
        enter(view)
    }

    fun popExit(view: View) {
        exit(view)
    }

    fun locate(view: View) {}
}

abstract class AnimationVisible : Visible {
    private val mAnim = hashMapOf<View, Any>()

    fun View.start(animator: ValueAnimator) {
        (mAnim[this] as? ValueAnimator)?.cancel()
        mAnim[this] = this
        animator.start()
    }

    fun View.playAll(vararg animator: ValueAnimator) {
        (mAnim[this] as? AnimatorSet)?.cancel()
        mAnim[this] = AnimatorSet().apply {
            playTogether(*animator)
            start()
        }
    }

    fun ValueAnimator.onUpdate(function: (Any) -> Unit): ValueAnimator {
        addUpdateListener {
            function(it.animatedValue)
        }
        return this
    }

    fun ValueAnimator.onEnd(function: () -> Unit): ValueAnimator {
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                function()
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
            }

        })
        return this
    }
}

class DefaultVisible : Visible {
    override fun enter(view: View) {
        super.enter(view)
        view.visibility = View.VISIBLE
    }

    override fun exit(view: View) {
        super.exit(view)
        view.visibility = View.GONE
    }
}

class ContainerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var mVisible: Visible = DefaultVisible()
    private var mVisibleId: Int = 0
    private val mViewCache = hashMapOf<Int, View>()

    init {
        if (attrs != null) {
            val typed =
                    context.obtainStyledAttributes(attrs, R.styleable.ContainerView, defStyleAttr, 0)
            if (isInEditMode) push(
                    typed.getResourceId(
                            R.styleable.ContainerView_android_layout,
                            0
                    )
            )
            typed.recycle()
        }
    }

    private fun setLayout(@LayoutRes layoutId: Int, pop: Boolean) {
        if (layoutId == 0) return
        if (mVisibleId == layoutId) return
        mViewCache[mVisibleId]?.also {
            hide(it, pop)
        }

        if (mViewCache.containsKey(layoutId)) {
            show(mViewCache[layoutId]!!, pop)
            mVisibleId = layoutId
        } else {
            val isAddFirst = mViewCache.size == 0
            val view = onCreateView(layoutId).also { mViewCache[layoutId] = it }
            addView(view)
            await(view) {
                if (!isAddFirst) {
                    mVisible.locate(view)
                    show(view, pop)
                }
                mVisibleId = layoutId
            }
        }
    }

    private fun hide(view: View, pop: Boolean) {
        if (pop) mVisible.popExit(view)
        else mVisible.exit(view)
    }

    private fun show(view: View, pop: Boolean) {
        if (pop) mVisible.popEnter(view)
        else mVisible.enter(view)
    }

    fun pop(@LayoutRes layoutId: Int) {
        setLayout(layoutId, true)
    }

    fun push(@LayoutRes layoutId: Int) {
        setLayout(layoutId, false)
    }

    private fun await(view: View, function: () -> Unit) {
        if (view.measuredWidth > 0) function()
        else {
            val onLayoutChanged = object : OnLayoutChangeListener {
                override fun onLayoutChange(
                        v: View?,
                        left: Int,
                        top: Int,
                        right: Int,
                        bottom: Int,
                        oldLeft: Int,
                        oldTop: Int,
                        oldRight: Int,
                        oldBottom: Int
                ) {
                    view.removeOnLayoutChangeListener(this)
                    function()
                }
            }
            view.addOnLayoutChangeListener(onLayoutChanged)
        }
    }

    fun setVisible(visible: Visible) {
        mVisible = visible
    }

    private fun onCreateView(layoutId: Int): View {
        return LayoutInflater.from(context).inflate(layoutId, this, false)
    }
}