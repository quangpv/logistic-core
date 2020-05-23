package com.support.location.engine.loader

import android.content.Context
import com.support.location.engine.LocationOptions
import com.support.location.engine.OnLocationUpdateListener

abstract class LocationLoader(val next: LocationLoader?, val options: LocationOptions) {
    companion object {
        fun getDefault(context: Context, options: LocationOptions = LocationOptions.DEFAULT): LocationLoader {
            return FusedLoader(context, NetworkLoader(context, GPSLoader(context, options = options), options), options)
        }
    }

    fun requestNext(listener: OnLocationUpdateListener) {
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