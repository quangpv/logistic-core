package com.support.location.engine

import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.android.gms.common.util.zzc
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class LastLocationEngine(private val context: Context) {
    companion object {
        private const val LOCATION_REFRESH_TIME = 1000L
        private const val LOCATION_REFRESH_DISTANCE = 100f
    }

    private var mLastLocation: LastLocation? = null

    private val mLock = ReentrantLock()
    private val mCondition = mLock.newCondition()
    private val mHandler = Handler(Looper.getMainLooper())
    private val mListeners = arrayListOf<(LatLng) -> Unit>()

    private val mLocationLoader = FuseLoader(next = NetworkLoader(next = GPSLoader()))

    private val mOnLocationChangedListener: (it: LatLng) -> Unit = {
        mListeners.forEach { listener -> listener(it) }
        mListeners.clear()
    }

    private fun launch(function: () -> Unit) {
        if (zzc.isMainThread()) function() else mHandler.post(function)
    }

    @RequiresPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    fun loadLastLocation(function: (LatLng) -> Unit) = launch {
        mListeners.add(function)
        mLocationLoader.loadLastLocation()
    }

    @RequiresPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    fun await(): LatLng {
        var location: LatLng? = null
        loadLastLocation {
            location = it
            mLock.withLock { mCondition.signal() }
        }
        if (location != null) return location!!
        mLock.withLock { mCondition.await(10, TimeUnit.SECONDS) }
        return (location ?: mLastLocation?.latLng ?: LatLng(0.0, 0.0)).also {
            Log.e("LastLocation", it.toString())
        }
    }

    private fun setLocation(location: Location) {
        mLastLocation = LastLocation(location).also {
            mOnLocationChangedListener(it.latLng)
        }
    }

    private class LastLocation(private val location: Location) {
        companion object {
            private val HCM_CENTER = Location("HCM Center").apply {
                latitude = 10.806439
                longitude = 106.696710
            }
            private val FAKE_POSITION = LatLng(1.3504308, 103.72091)
            private val FAKE_US_POSITION = LatLng(41.879370, -87.629558)
        }

        val latLng: LatLng get() = LatLng(location.latitude, location.longitude)
    }

    private abstract inner class LocationLoader(private val next: LocationLoader? = null) {
        abstract fun loadLastLocation()

        fun next() {
            next?.loadLastLocation()
        }

        fun handle(location: Location?) {
            if (location == null) next?.loadLastLocation()
            else setLocation(location)
        }
    }

    private inner class FuseLoader(next: LocationLoader? = null) : LocationLoader(next) {
        private val mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        private val mRequestUpdate = LocationRequest().also {
            it.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            it.smallestDisplacement = LOCATION_REFRESH_DISTANCE
            it.interval = LOCATION_REFRESH_TIME
            it.fastestInterval = LOCATION_REFRESH_TIME
        }

        override fun loadLastLocation() {
            mFusedLocationClient.requestLocationUpdates(mRequestUpdate, object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult?) {
                    mFusedLocationClient.removeLocationUpdates(this)
                    if (p0 == null) next()
                    else handle(if (p0.locations.isNotEmpty()) p0.locations.first() else p0.lastLocation)
                }
            }, null).addOnCanceledListener {
                next()
            }
        }
    }

    private abstract inner class HardwareLoader(next: LocationLoader?) : LocationLoader(next) {
        abstract val provider: String

        protected val mLocationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager

        private val mCallback = object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                mLocationManager.removeUpdates(this)
                handle(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String?) {}

            override fun onProviderDisabled(provider: String?) {}

        }

        @RequiresPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        override fun loadLastLocation() {
            if (!mLocationManager.isProviderEnabled(provider)) {
                next()
                return
            }
            val location = mLocationManager.getLastKnownLocation(provider)

            if (location != null) {
                setLocation(location)
                return
            }

            mLocationManager.removeUpdates(mCallback)
            mLocationManager.requestLocationUpdates(
                    provider, LOCATION_REFRESH_TIME,
                    LOCATION_REFRESH_DISTANCE, mCallback
            )
        }

    }

    private inner class GPSLoader(next: LocationLoader? = null) : HardwareLoader(next) {
        override val provider: String
            get() = LocationManager.GPS_PROVIDER
    }

    private inner class NetworkLoader(next: LocationLoader? = null) : HardwareLoader(next) {
        override val provider: String
            get() = LocationManager.NETWORK_PROVIDER
    }

}

