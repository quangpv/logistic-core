package com.support.location.engine.loader

import android.content.Context
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.support.location.engine.LocationOptions
import com.support.location.engine.OnLocationUpdateListener
import com.support.location.isGPSEnabled
import com.support.location.location

class FusedLoader(
    context: Context,
    next: LocationLoader? = null,
    options: LocationOptions = LocationOptions.DEFAULT
) : LocationLoader(context, next, options) {
    private val mCallbacks = hashMapOf<OnLocationUpdateListener, LocationCallback>()
    private val mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val mLocationRequest = LocationRequest().also {
        it.interval = options.interval
        it.fastestInterval = options.interval
        it.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        it.smallestDisplacement = options.minDistance
    }

    override fun loadLastLocation(listener: OnLocationUpdateListener) {

        if (!context.isGPSEnabled) {
            if (next == null) {
                listener.onLocationUpdated(options.default.location)
                return
            }
            mFusedLocationClient.lastLocation.addOnSuccessListener {
                if (it != null) listener.onLocationUpdated(it)
                else next.loadLastLocation(listener)
            }.addOnCanceledListener {
                next.loadLastLocation(listener)
            }.addOnFailureListener {
                next.loadLastLocation(listener)
            }
            return
        }

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
        }.addOnFailureListener {
            next?.loadLastLocation(listener)
        }
    }

    override fun getLastLocation(function: OnLocationUpdateListener) {
        mFusedLocationClient.lastLocation.addOnSuccessListener {
            if (it != null) function.onLocationUpdated(it)
            else next?.getLastLocation(function)
        }.addOnCanceledListener {
            next?.getLastLocation(function)
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