package com.support.location.engine.loader

import android.content.Context
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.support.location.engine.LocationOptions
import com.support.location.engine.OnLocationUpdateListener

class FusedLoader(context: Context,
                  next: LocationLoader? = null,
                  options: LocationOptions = LocationOptions.DEFAULT) : LocationLoader(next) {
    private val mCallbacks = hashMapOf<OnLocationUpdateListener, LocationCallback>()
    private val mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val mLocationRequest = LocationRequest().also {
        it.interval = options.interval
        it.fastestInterval = options.interval
        it.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        it.smallestDisplacement = options.minDistance
    }

    override fun loadLastLocation(listener: OnLocationUpdateListener) {
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                mFusedLocationClient.removeLocationUpdates(this)
                when {
                    p0 == null -> next?.loadLastLocation(listener)
                    p0.locations.isEmpty() -> listener.onLocationUpdated(p0.lastLocation)
                    else -> listener.onLocationUpdated(p0.locations.first())
                }
            }
        }, null).addOnCanceledListener {
            next?.loadLastLocation(listener)
        }
    }

    override fun contains(listener: OnLocationUpdateListener): Boolean {
        return mCallbacks.containsKey(listener)
    }

    override fun requestCallback(listener: OnLocationUpdateListener) {
        if (mCallbacks.containsKey(listener)) return
        val callback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                when {
                    p0 == null -> requestNext(listener)
                    p0.locations.isEmpty() -> listener.onLocationUpdated(p0.lastLocation)
                    else -> listener.onLocationUpdated(p0.locations.first())
                }
            }
        }
        mCallbacks[listener] = callback
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, callback, null)
    }

    override fun removeCallback(listener: OnLocationUpdateListener): Boolean {
        val callback = mCallbacks.remove(listener) ?: return false
        mFusedLocationClient.removeLocationUpdates(callback)
        return true
    }
}