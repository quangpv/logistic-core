package com.support.location.engine.loader

import android.content.Context
import android.location.Location
import com.support.location.engine.LocationOptions
import com.support.location.engine.OnLocationUpdateListener
import com.support.location.isGPSEnabled
import com.support.location.location

abstract class LocationLoader(
        val context: Context,
        val next: LocationLoader?,
        val options: LocationOptions
) {

    companion object {
        fun getDefault(context: Context, options: LocationOptions = LocationOptions.DEFAULT): LocationLoader {
            return FusedLoader(context, NetworkLoader(context, GPSLoader(context, options = options), options), options)
        }

        private const val TIMEOUT = 1000L
    }

    private var mLastLocation: Pair<Location, Long>? = null

    fun notifyDefaultIfGPSNotAvailable(listener: OnLocationUpdateListener): Boolean {
        if (!context.isGPSEnabled) {
            if (next == null) {
                listener.onLocationUpdated(options.default.location)
                return true
            }
        }
        return false
    }


    protected fun nextLoadLast(listener: OnLocationUpdateListener) {
        if (next == null) listener.onLocationUpdated(options.default.location)
        else next.loadLastLocation(listener)
    }

    protected fun canReuseLastLocation(listener: OnLocationUpdateListener): Boolean {
        if (mLastLocation != null) {
            val last = mLastLocation!!
            if (System.currentTimeMillis() - last.second <= TIMEOUT) {
                listener.onLocationUpdated(last.first)
                return true
            }
        }
        return false
    }

    protected fun notifyLocationUpdated(location: Location, listener: OnLocationUpdateListener) {
        mLastLocation = location to System.currentTimeMillis()
        listener.onLocationUpdated(location)
    }

    fun nextRequest(listener: OnLocationUpdateListener) {
        removeCallback(listener)
        next?.requestUpdate(listener)
    }

    fun requestLastNext(listener: OnLocationUpdateListener) {
        removeCallback(listener)
        next?.loadLastLocation(listener)
    }

    fun requestUpdate(listener: OnLocationUpdateListener) {
        requestCallback(listener)
    }

    fun removeUpdate(listener: OnLocationUpdateListener) {
        if (!removeCallback(listener)) next?.removeUpdate(listener)
    }

    abstract fun loadLastLocation(listener: OnLocationUpdateListener)
    abstract fun contains(listener: OnLocationUpdateListener): Boolean
    protected abstract fun requestCallback(listener: OnLocationUpdateListener)
    protected abstract fun removeCallback(listener: OnLocationUpdateListener): Boolean
    abstract fun getLastLocation(function: OnLocationUpdateListener)
}