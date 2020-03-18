package com.support.core.event

import androidx.lifecycle.Observer
import com.support.core.AppExecutors

interface Notifiable {
    fun notifyChange()
}

open class Observable<T> : Notifiable {
    @Transient
    private val mObservers = hashSetOf<Observer<T>>()

    fun subscribe(observer: Observer<T>) {
        if (mObservers.add(observer)) notifyChange(observer)
    }

    fun unsubscribe(observer: Observer<T>) {
        mObservers.remove(observer)
    }

    override fun notifyChange() {
        AppExecutors.mainIO.execute { mObservers.forEach { notifyChange(it) } }
    }

    private fun notifyChange(observer: Observer<T>) {
        @Suppress("unchecked_cast")
        observer.onChanged(this as T)
    }
}

@Suppress("unchecked_cast")
fun <T> T.asObservable(): Observable<T>? {
    return this as? Observable<T>
}