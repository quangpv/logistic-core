package com.support.location.engine

import android.content.Context
import android.location.Location
import androidx.annotation.MainThread
import com.support.location.engine.loader.LocationLoader

class RealTimeLocationEngine(
        private val context: Context,
        private val loader: LocationLoader
) : LocationEngine {
    private var mLastLocation: Location? = null
    private var mStarted: Boolean = false
    private val mListeners = arrayListOf<OnLocationUpdateListener>()
    private val mConstraint = waitPermission(context)

    override val delegate: LocationEngine by lazy(LazyThreadSafetyMode.NONE) {
        object : LifecycleLocationDelegate(context) {
            override fun onRequest(listener: OnLocationUpdateListener) {
                if (mLastLocation != null) listener.onLocationUpdated(mLastLocation!!)
                mListeners.add(listener)
            }

            override fun onRemove(listener: OnLocationUpdateListener) {
                mListeners.remove(listener)
            }
        }
    }

    private var mCallback = object : OnLocationUpdateListener {
        override fun onLocationUpdated(location: Location) {
            mLastLocation = location
            mListeners.forEach { it.onLocationUpdated(location) }
        }
    }

    @MainThread
    fun start() {
        if (mStarted) return
        mConstraint.start {
            loader.requestUpdate(mCallback)
            mStarted = true
        }
    }

    @MainThread
    fun stop() {
        if (!mStarted) {
            mConstraint.cancel()
            return
        }
        loader.removeUpdate(mCallback)
        mStarted = false
    }

    @MainThread
    fun addUpdatedListener(listener: OnLocationUpdateListener) {
        if (mListeners.contains(listener)) return
        mListeners.add(listener)
    }

    @MainThread
    fun removeUpdatedListener(listener: OnLocationUpdateListener) {
        mListeners.remove(listener)
    }
}