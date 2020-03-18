package com.support.core.event

import androidx.lifecycle.LifecycleOwner

interface Event<T> {

    fun set(value: T?)

    fun observe(owner: LifecycleOwner, function: (T?) -> Unit)
}