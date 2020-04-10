package com.support.location.map

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import com.google.android.gms.maps.SupportMapFragment


class FocusableMapFragment : SupportMapFragment() {

    override fun onCreateView(p0: LayoutInflater, p1: ViewGroup?, p2: Bundle?): View? {
        return TouchableWrapper(requireContext()).apply {
            addView(super.onCreateView(p0, p1, p2))
        }
    }

    inner class TouchableWrapper(context: Context) : FrameLayout(context) {
        private var scrollView: ViewGroup? = null

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            scrollView = findScrollerView()
        }

        override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
            scrollView?.requestDisallowInterceptTouchEvent(true)
            return super.dispatchTouchEvent(ev)
        }
    }

    private fun View.findScrollerView(): ViewGroup? {
        if (parent == null) return null
        if (parent is ScrollView || parent is NestedScrollView) return parent as ViewGroup
        return (parent as View).findScrollerView()
    }
}
