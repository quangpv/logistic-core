package com.support.core.extension

import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.support.core.AppExecutors
import com.support.core.ConcurrentContext
import com.support.core.ConcurrentScope
import com.support.core.base.BaseFragment
import com.support.core.event.LoadingEvent
import com.support.core.event.SingleLiveEvent
import com.support.core.functional.Form
import com.support.core.isOnMainThread


fun <T> LiveData<T>.observe(owner: LifecycleOwner, function: (T?) -> Unit) {
    observe(owner, Observer(function))
}

fun <T, V> LiveData<T>.map(function: (T) -> V): LiveData<V> {
    val next = MediatorLiveData<V>()
    next.addSource(this) {
        next.value = function(it)
    }
    return next
}

fun <T> MutableLiveData<T>.load(background: Boolean = true, function: () -> T): LiveData<T> {
    if (background) AppExecutors.diskIO.execute {
        postValue(function())
    } else value = function()
    return this
}

fun <T> MutableLiveData<T>.loadNotNull(
    background: Boolean = true,
    function: () -> T?
): LiveData<T> {
    if (background) AppExecutors.diskIO.execute {
        function()?.also { postValue(it) }
    } else function()?.also { value = it }
    return this
}

fun MutableLiveData<*>.call() {
    this.post(null)
}

fun <T> MutableLiveData<T>.refresh() {
    this.post(value)
}

fun <T> MutableLiveData<T>.post(t: T?) {
    if (isOnMainThread) value = t
    else postValue(t)
}

fun <T> LiveData<T>.subscribe(
    owner: LifecycleOwner,
    function: (T) -> Unit
) {
    observe(
        when (owner) {
            is BaseFragment -> owner.visibleOwner
            is Fragment -> owner.viewLifecycleOwner
            else -> owner
        }, Observer(function)
    )
}

fun <T> LiveData<T>.toSingle(): LiveData<T> {
    return SingleLiveEvent<T>().also { next ->
        next.addSource(this) {
            next.value = it
        }
    }
}

fun <T> LiveData<T>.filter(function: (T) -> Boolean): LiveData<T> {
    return MediatorLiveData<T>().also { next ->
        next.addSource(this) {
            if (function(it)) next.value = it
        }
    }
}

fun <T> LiveData<T>.asMutable(): MutableLiveData<T> {
    return this as? MutableLiveData<T> ?: error("$this is not mutable")
}

class LoadCacheLiveData<T, V> : MediatorLiveData<Pair<T, V>>()


fun <T, V> LiveData<T>.loadCache(
    background: Boolean = false,
    function: (T) -> V
): LoadCacheLiveData<T, V> {
    return LoadCacheLiveData<T, V>().also { next ->
        next.addSource(this) {
            if (!background) next.value = it to function(it)
            else AppExecutors.diskIO.execute {
                next.postValue(it to function(it))
            }
        }
    }
}

fun <T, V> MutableLiveData<V>.doAsync(
    it: T,
    concurrent: ConcurrentContext,
    loadingEvent: LoadingEvent?,
    errorEvent: SingleLiveEvent<Throwable>?,
    function: ConcurrentScope.(T) -> V?
) {
    if (it is Form) {
        try {
            it.validate()
        } catch (e: Throwable) {
            errorEvent?.postValue(e)
            return
        }
    }
    loadingEvent?.value = true
    concurrent.launch {
        try {
            postValue(function(it))
        } catch (t: Throwable) {
            errorEvent?.postValue(t)
            t.printStackTrace()
        } finally {
            loadingEvent?.postValue(false)
        }
    }
}
