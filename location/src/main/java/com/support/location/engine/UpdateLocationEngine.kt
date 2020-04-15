package com.support.location.engine

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.support.location.WaitUntil

class UpdateLocationEngine(
        private val context: Context,
        private val interval: Long = 1000L,
        private val minDistance: Float = 0f
) : LocationEngine {
    private var mStarted: Boolean = false
    private val mListeners = arrayListOf<OnLocationUpdateListener>()
    private val mEngine = FusedEngine(NetworkEngine(GPSEngine(null)))

    private val mConstraint = WaitUntil {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun subscribe(owner: LifecycleOwner, function: (Location) -> Unit) {
        val listener = object : OnLocationUpdateListener {
            override fun onLocationUpdated(location: Location) {
                function(location)
            }
        }
        mListeners.add(listener)
        owner.lifecycle.addObserver(object : LifecycleObserver {
            @Suppress("unused")
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                mListeners.remove(listener)
                owner.lifecycle.removeObserver(this)
            }
        })
    }

    private fun notifyChange(location: Location) {
        Log.e("Location", location.toString())
        mListeners.forEach { it.onLocationUpdated(location) }
    }

    fun start() {
        if (mStarted) return
        mConstraint.start {
            mEngine.requestUpdate()
            mStarted = true
        }
    }

    fun stop() {
        if (!mStarted) {
            mConstraint.cancel()
            return
        }
        mStarted = false
        mEngine.removeUpdate()
    }

    fun addUpdatedListener(listener: OnLocationUpdateListener) {
        if (mListeners.contains(listener)) return
        mListeners.add(listener)
    }

    fun removeUpdatedListener(listener: OnLocationUpdateListener) {
        mListeners.remove(listener)
    }

    private abstract inner class Engine(private val next: Engine?) {

        private var mUsed: Boolean = false

        fun next() {
            removeUpdate()
            next?.requestUpdate()
        }

        fun handle(location: Location?) {
            if (location == null) next()
            else notifyChange(location)
        }

        fun requestUpdate() {
            Log.e("Request-Location", this.javaClass.name)
            synchronized(Engine::class) { mUsed = true }
            doRequestUpdate()
        }

        fun removeUpdate() {
            Log.e("Remove-Location", this.javaClass.name)

            if (!mUsed) {
                next?.removeUpdate()
                return
            }
            synchronized(Engine::class) { mUsed = false }
            doRemoveUpdate()
        }

        protected abstract fun doRequestUpdate()

        protected abstract fun doRemoveUpdate()
    }

    private inner class FusedEngine(next: Engine?) : Engine(next) {
        private val mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        private val mCallback: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                if (p0 == null) next()
                else {
                    if (p0.locations.isEmpty()) handle(p0.lastLocation)
                    else handle(p0.locations.first())
                }
            }
        }

        private val mLocationRequest = LocationRequest().also {
            it.interval = interval
            it.fastestInterval = interval
            it.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            it.smallestDisplacement = minDistance
        }

        override fun doRequestUpdate() {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mCallback, null)
        }

        override fun doRemoveUpdate() {
            mFusedLocationClient.removeLocationUpdates(mCallback)
        }
    }

    private abstract inner class HardwareEngine(next: Engine?) : Engine(next) {
        abstract val provider: String

        protected val mLocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        private val mCallback = object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                handle(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String?) {}

            override fun onProviderDisabled(provider: String?) {}

        }

        @SuppressLint("MissingPermission")
        override fun doRequestUpdate() {
            if (!mLocationManager.isProviderEnabled(provider)) {
                next()
                return
            }
            mLocationManager.removeUpdates(mCallback)
            mLocationManager.requestLocationUpdates(
                    provider, interval, minDistance, mCallback
            )
        }

        override fun doRemoveUpdate() {
            mLocationManager.removeUpdates(mCallback)
        }
    }

    private inner class NetworkEngine(next: Engine?) : HardwareEngine(next) {
        override val provider: String
            get() = LocationManager.NETWORK_PROVIDER
    }

    private inner class GPSEngine(next: Engine? = null) : HardwareEngine(next) {
        override val provider: String
            get() = LocationManager.GPS_PROVIDER
    }
}

interface OnLocationUpdateListener {
    fun onLocationUpdated(location: Location)
}
