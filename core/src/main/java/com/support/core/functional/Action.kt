package com.support.core.functional

import android.app.Activity
import android.view.View
import androidx.annotation.CallSuper

interface Action {

    @CallSuper
    fun <T> runWith(any: T?, function: T.() -> Unit) {
        any?.apply(function)
    }

    @Suppress("UNCHECKED_CAST")
    @CallSuper
    fun <T : Activity> runActivity(view: View?, function: T.() -> Unit) {
        (view?.context as? T)?.apply(function)
    }

    @CallSuper
    @Suppress("UNCHECKED_CAST")
    fun <T> Any.case(function: T.() -> Unit): T? {
        return (this as? T)?.apply(function)
    }
}


