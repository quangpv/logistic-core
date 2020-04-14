package com.support.core.event

import android.annotation.SuppressLint
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.support.core.base.BaseFragment

class LocalEvent<T> : Event<T> {
    private val mObservers = hashMapOf<Observer<*>, ObserverWrapper>()
    private var mValue: T? = null

    var version: Int = START_VERSION
        private set

    val value: T? get() = mValue

    override fun set(value: T?) {
        post(value)
    }

    @SuppressLint("RestrictedApi")
    fun post(value: T? = null) {
        ArchTaskExecutor.getInstance().executeOnMainThread { doPost(value) }
    }

    private fun doPost(value: T?) {
        mValue = value
        version += 1
        notifyChanges()
    }

    private fun notifyChanges() {
        mObservers.values.forEach { it.notifyChange() }
    }

    override fun observe(owner: LifecycleOwner, function: (T?) -> Unit) {
        val lifeOwner = when (owner) {
            is BaseFragment -> owner.visibleOwner
            is Fragment -> owner.viewLifecycleOwner
            else -> owner
        }
        observe(lifeOwner, Observer { function(it) })
    }

    fun observeNotNull(owner: LifecycleOwner, function: (T) -> Unit) {
        observe(owner, Observer(function))
    }

    fun observe(owner: LifecycleOwner, observer: Observer<T>) {
        mObservers[observer] = LifeObserver(owner, observer).apply { onAttached() }
    }

    fun observeForever(observer: Observer<T>) {
        mObservers[observer] = ForeverObserver(observer).apply { onAttached() }
    }

    fun observeForever(function: (T?) -> Unit) {
        observeForever(Observer { function(it) })
    }

    fun observeForeverNotNull(function: (T) -> Unit) {
        observeForever(Observer(function))
    }

    fun removeObserver(observer: Observer<T>) {
        mObservers.remove(observer)?.onDetached()
    }

    private abstract class ObserverWrapper {
        open fun onAttached() {}
        open fun onDetached() {}
        abstract fun notifyChange()
    }

    private inner class LifeObserver(
            private val owner: LifecycleOwner,
            private val observer: Observer<T>
    ) : ObserverWrapper(), LifecycleEventObserver {
        private var mVersion = version

        override fun onAttached() {
            owner.lifecycle.addObserver(this)
        }

        override fun onDetached() {
            owner.lifecycle.removeObserver(this)
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                removeObserver(observer)
            } else if (event == Lifecycle.Event.ON_START) {
                if (mVersion != version) notifyChange()
            }
        }

        override fun notifyChange() {
            if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
            mVersion = version
            observer.onChanged(value)
        }
    }

    private inner class ForeverObserver(private val observer: Observer<T>) : ObserverWrapper() {

        override fun notifyChange() {
            observer.onChanged(value)
        }
    }

    companion object {
        const val START_VERSION = -1
    }

}