package com.support.core.event

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

class Subscriber<T>(private val owner: LifecycleOwner, private val function: (T) -> Unit) {
    fun post(value: T) {
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        function(value)
    }
}