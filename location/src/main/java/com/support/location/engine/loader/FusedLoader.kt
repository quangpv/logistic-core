package com.support.location.engine.loader

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.support.location.engine.LocationOptions
import com.support.location.engine.OnLocationUpdateListener

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

    @SuppressLint("MissingPermission")
    override fun loadLastLocation(listener: OnLocationUpdateListener) {
        if (canReuseLastLocation(listener)) return

        mFusedLocationClient.lastLocation.addOnSuccessListener {
            if (it != null) listener.onLocationUpdated(it)
            else nextLoadLast(listener)
        }.addOnCanceledListener {
            next?.loadLastLocation(listener)
        }.addOnFailureListener {
            next?.loadLastLocation(listener)
        }

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                mFusedLocationClient.removeLocationUpdates(this)
                when {
                    p0 == null -> nextLoadLast(listener)
                    p0.locations.isEmpty() -> notifyLocationUpdated(p0.lastLocation, listener)
                    else -> notifyLocationUpdated(p0.locations.first(), listener)
                }
            }
        }, null).addOnCanceledListener {
            next?.loadLastLocation(listener)
        }.addOnFailureListener {
            next?.loadLastLocation(listener)
        }
    }

    @SuppressLint("MissingPermission")
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

    @SuppressLint("MissingPermission")
    override fun requestCallback(listener: OnLocationUpdateListener) {
        if (mCallbacks.containsKey(listener)) return
        val callback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                when {
                    p0 == null -> nextRequest(listener)
                    p0.locations.isEmpty() -> notifyLocationUpdated(p0.lastLocation, listener)
                    else -> notifyLocationUpdated(p0.locations.first(), listener)
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