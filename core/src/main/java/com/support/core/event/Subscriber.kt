package com.support.core.event

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.support.core.AppExecutors

class Subscriber<T>(private val owner: LifecycleOwner, private val function: (T) -> Unit) {
    operator fun invoke(value: T) {
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        AppExecutors.mainIO.execute { function(value) }
    }
}