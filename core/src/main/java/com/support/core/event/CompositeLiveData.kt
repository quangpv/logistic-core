package com.support.core.event

import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

class CompositeLiveData<T> : MediatorLiveData<T>() {
    private val mSources = hashMapOf<LocalEvent<*>, Source<*>>()

    @MainThread
    fun <S> addSource(
            source: LocalEvent<S>,
            onChanged: Observer<in S>
    ) {
        if (mSources.containsKey(source)) return
        val e = Source(source, onChanged)
        mSources[source] = e
        if (hasActiveObservers()) e.plug()
    }

    @MainThread
    fun <S> addSource(
            source: LocalEvent<S>,
            function: (S?) -> Unit
    ) {
        addSource(source, Observer(function))
    }

    @MainThread
    fun <S> addSourceNotNull(
            source: LocalEvent<S>,
            function: (S) -> Unit
    ) {
        addSource(source, Observer(function))
    }

    @MainThread
    fun <S> removeSource(toRemote: LocalEvent<S>) {
        val source = mSources.remove(toRemote)
        source?.unplug()
    }

    @CallSuper
    override fun onActive() {
        super.onActive()
        for ((_, value) in mSources) {
            value.plug()
        }
    }

    @CallSuper
    override fun onInactive() {
        super.onInactive()
        for ((_, value) in mSources) {
            value.unplug()
        }
    }

    private class Source<V> internal constructor(
            val mEvent: LocalEvent<V>,
            val mObserver: Observer<in V>
    ) : Observer<V> {
        var mVersion = mEvent.version

        fun plug() {
            mEvent.observeForever(this)
            if (mVersion != mEvent.version) onChanged(mEvent.value)
        }

        fun unplug() {
            mEvent.removeObserver(this)
        }

        override fun onChanged(v: V?) {
            mVersion = mEvent.version
            mObserver.onChanged(v)
        }

    }
}