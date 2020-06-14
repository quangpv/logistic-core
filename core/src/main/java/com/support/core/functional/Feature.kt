package com.support.core.functional

import android.content.Context
import android.view.View
import com.support.core.R

abstract class Feature {
    private var mView: View? = null
    val view: View? get() = mView
    val requireView: View
        get() = mView ?: error("Feature ${this.javaClass.simpleName} not attached to view yet!")

    val context: Context get() = requireView.context

    companion object {
        internal val TAG_ID = R.id.feature
    }

    internal fun attach(view: View) {
        mView = view
        view.setTag(TAG_ID, this)
        onAttached(view)
    }

    internal fun detach(view: View) {
        view.setTag(TAG_ID, null)
        onDetached(view)
        mView = null
    }

    protected open fun onAttached(view: View) {}
    protected open fun onDetached(view: View) {}
}


fun View.set(feature: Feature) {
    val oldFeature = getTag(Feature.TAG_ID) as? Feature
    if (oldFeature == feature) return
    oldFeature?.detach(this)
    feature.attach(this)
}
