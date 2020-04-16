package com.support.location.engine

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

abstract class LifecycleLocationDelegate(private val context: Context) : LocationEngine {
    private val mCallbacks = hashMapOf<OnLocationUpdateListener, CallbackWrapper>()

    fun contains(listener: OnLocationUpdateListener): Boolean {
        return mCallbacks.containsKey(listener)
    }

    override fun subscribe(owner: LifecycleOwner, listener: OnLocationUpdateListener) {
        if (mCallbacks.containsKey(listener)) return
        mCallbacks[listener] = CallbackWrapper(owner, listener).apply { onAttached() }
    }

    override fun unsubscribe(listener: OnLocationUpdateListener) {
        mCallbacks.remove(listener)?.apply { onDetached() }
    }

    inner class CallbackWrapper(private val owner: LifecycleOwner, private val listener: OnLocationUpdateListener) {
        private var mRequest = false
        private val mObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_DESTROY -> unsubscribe(listener)
                Lifecycle.Event.ON_START -> requestUpdateIfNeeded()
                else -> {
                }
            }
        }

        fun onDetached() {
            owner.lifecycle.removeObserver(mObserver)
            onRemove(listener)
        }

        fun onAttached() {
            owner.lifecycle.addObserver(mObserver)
            requestUpdateIfNeeded()
        }

        private fun requestUpdateIfNeeded() {
            if (!mRequest && isAllowed(context)) {
                onRequest(listener)
                mRequest = true
            }
        }
    }

    protected open fun onRequest(listener: OnLocationUpdateListener) {}

    protected open fun onRemove(listener: OnLocationUpdateListener) {}
}