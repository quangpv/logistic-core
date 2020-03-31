package com.support.core.base

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

abstract class LifecycleViewModel : BaseViewModel(), LifecycleOwner {
    private var mRegistry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = mRegistry

    override fun onCreate() {
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onCleared() {
        super.onCleared()
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

}

