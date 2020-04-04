package com.support.core.functional

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

interface FullLifecycleObserver : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> onCreate()
            Lifecycle.Event.ON_DESTROY -> onDestroy()
            Lifecycle.Event.ON_START -> onStart()
            Lifecycle.Event.ON_STOP -> onStop()
            Lifecycle.Event.ON_RESUME -> onResume()
            Lifecycle.Event.ON_PAUSE -> onPause()
            else -> {
            }
        }
    }

    fun onCreate() {
    }

    fun onDestroy() {
    }

    fun onStart() {
    }

    fun onStop() {
    }

    fun onResume() {
    }

    fun onPause() {
    }
}