package com.support.core.functional

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

interface FragmentVisibleObserver : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_DESTROY -> source.lifecycle.removeObserver(this)
            Lifecycle.Event.ON_START -> onFragmentStarted()
            Lifecycle.Event.ON_STOP -> onFragmentStopped()
            else -> {
            }
        }
    }

    fun onFragmentStopped() {}

    fun onFragmentStarted() {}
}