package com.support.core.event

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.support.core.base.BaseFragment


interface PostAble<in T> {
    fun postValue(value: T?)
    fun setValue(value: T?)
}

class Subscriber<T> : MutableLiveData<T>(), PostAble<T> {
    private val LifecycleOwner.subscribeOwner: LifecycleOwner
        get() = when (this) {
            is BaseFragment -> visibleOwner
            is Fragment -> viewLifecycleOwner
            else -> this
        }

    fun subscribe(owner: LifecycleOwner, function: (T) -> Unit) {

        super.observe(owner.subscribeOwner, object : Observer<T> {
            override fun onChanged(t: T) {
                function(t)
                removeObserver(this)
            }
        })
    }

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner.subscribeOwner, object : Observer<T> {
            override fun onChanged(t: T) {
                observer.onChanged(t)
                removeObserver(this)
            }
        })
    }
}

fun <T> subscriber(function: PostAble<T>.() -> Unit): Subscriber<T> {
    return Subscriber<T>().apply(function)
}