package com.support.core.event

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.support.core.AppExecutors
import com.support.core.base.BaseFragment

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

    fun observeNotNull(owner: LifecycleOwner, function: (T) -> Unit) {
        observe(owner) {
            it?.also(function)
        }
    }

    fun observeForeverNotNull(function: (T) -> Unit) {
        observeForever {
            it?.also(function)
        }
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
            owner: LifecycleOwner,
            private val function: (T?) -> Unit
    ) : IObserver {
        private val lifecycle = when (owner) {
            is BaseFragment -> owner.visibleOwner.lifecycle
            is Fragment -> owner.viewLifecycleOwner.lifecycle
            else -> owner.lifecycle
        }
        private var shouldNotify: Boolean = false

        init {
            lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    when (event) {
                        Lifecycle.Event.ON_DESTROY -> {
                            lifecycle.removeObserver(this)
                            mObservers.remove(this@ObserverWrapper)
                        }
                        Lifecycle.Event.ON_START -> notifyChangeIfNeeded()
                        else -> {
                        }
                    }
                }
            })
        }

        override fun notifyChange() = synchronized(this) {
            shouldNotify = true
            notifyChangeIfNeeded()
        }

        private fun notifyChangeIfNeeded() = synchronized(this) {
            if (!shouldNotify) return
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
            AppExecutors.mainIO.execute { function(mValue) }
            shouldNotify = false
        }
    }
}