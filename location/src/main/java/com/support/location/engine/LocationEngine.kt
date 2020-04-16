package com.support.location.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.support.location.WaitUntil

interface LocationEngine {
    val delegate: LocationEngine? get() = null

    fun isAllowed(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun waitPermission(context: Context) = WaitUntil { isAllowed(context) }

    @MainThread
    fun subscribe(owner: LifecycleOwner, function: (Location) -> Unit) {
        subscribe(owner, object : OnLocationUpdateListener {
            override fun onLocationUpdated(location: Location) {
                function(location)
            }
        })
    }

    @MainThread
    fun subscribe(owner: LifecycleOwner, listener: OnLocationUpdateListener) {
        delegate?.subscribe(owner, listener)
    }

    @MainThread
    fun unsubscribe(listener: OnLocationUpdateListener) {
        delegate?.unsubscribe(listener)
    }

}