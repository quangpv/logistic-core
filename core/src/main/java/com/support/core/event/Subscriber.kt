package com.support.core.event

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

class Subscriber<T> : MutableLiveData<T>() {

    fun subscribe(owner: LifecycleOwner, function: (T) -> Unit) {
        super.observe(owner, object : Observer<T> {
            override fun onChanged(t: T) {
                function(t)
                removeObserver(this)
            }
        })
    }

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner, object : Observer<T> {
            override fun onChanged(t: T) {
                observer.onChanged(t)
                removeObserver(this)
            }
        })
    }
}

fun <T> subscriber(function: Subscriber<T>.() -> Unit): Subscriber<T> {
    return Subscriber<T>().apply(function)
}