package com.support.core.event

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.support.core.AppExecutors

class LocalEvent<T> : Event<T> {
    private val mObservers = hashSetOf<IObserver>()
    private var mValue: T? = null

    override fun set(value: T?) {
        mValue = value
        notifyChange()
    }

    override fun observe(owner: LifecycleOwner, function: (T?) -> Unit) {
        synchronized(mObservers) { mObservers.add(ObserverWrapper(owner, function)) }
    }

    private fun notifyChange() = synchronized(mObservers) {
        mObservers.forEach { it.notifyChange() }
    }

    fun listen(event: LocalEvent<T>) {
        event.observeForever {
            set(it)
        }
    }

    fun observeForever(function: (T?) -> Unit) {
        synchronized(mObservers) { mObservers.add(ForeverWrapper(function)) }
    }

    private interface IObserver {
        fun notifyChange()
    }

    private inner class ForeverWrapper(private val function: (T?) -> Unit) : IObserver {
        override fun notifyChange() {
            AppExecutors.mainIO.execute { function(mValue) }
        }
    }

    private inner class ObserverWrapper(
        private val owner: LifecycleOwner,
        private val function: (T?) -> Unit
    ) : IObserver {

        private var shouldNotify: Boolean = false

        init {
            owner.lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    owner.lifecycle.removeObserver(this)
                    mObservers.remove(this@ObserverWrapper)
                }

                @OnLifecycleEvent(Lifecycle.Event.ON_START)
                fun onStart() {
                    notifyChangeIfNeeded()
                }
            })
        }

        override fun notifyChange() = synchronized(this) {
            shouldNotify = true
            notifyChangeIfNeeded()
        }

        private fun notifyChangeIfNeeded() = synchronized(this) {
            if (!shouldNotify) return
            if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
            AppExecutors.mainIO.execute { function(mValue) }
            shouldNotify = false
        }
    }
}