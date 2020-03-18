package com.support.core.navigation

import androidx.lifecycle.*


fun Lifecycle.onEvent(event: Lifecycle.Event, function: () -> Unit): Lifecycle {
    addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, newEvent: Lifecycle.Event) {
            if (event == newEvent) {
                function()
            }
        }
    })
    return this
}

fun Lifecycle.onStart(function: () -> Unit) = onEvent(Lifecycle.Event.ON_START, function)
fun Lifecycle.onStop(function: () -> Unit) = onEvent(Lifecycle.Event.ON_STOP, function)
fun Lifecycle.onDestroy(function: () -> Unit) = onEvent(Lifecycle.Event.ON_DESTROY, function)

fun Lifecycle.onSingleStart(function: () -> Unit): Lifecycle {
    addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onEvent() {
            removeObserver(this)
            function()
        }
    })
    return this
}
