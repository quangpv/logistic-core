package com.support.location.engine

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.arch.core.executor.ArchTaskExecutor
import com.google.android.gms.maps.model.LatLng
import com.support.location.engine.loader.LocationLoader
import com.support.location.latLng
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class LastLocationEngine(
        private val context: Context,
        private val loader: LocationLoader = LocationLoader.getDefault(context)
) : LocationEngine {
    private var mLastLocation: Location? = null

    private val mLock = ReentrantLock()
    private val mCondition = mLock.newCondition()

    override val delegate: LocationEngine by lazy(LazyThreadSafetyMode.NONE) {
        object : LifecycleLocationDelegate(context) {
            @SuppressLint("MissingPermission")
            override fun onRequest(listener: OnLocationUpdateListener) {
                loadLastLocation(listener)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    fun loadLastLocation(function: OnLocationUpdateListener) = ArchTaskExecutor.getInstance().executeOnMainThread {
        loader.loadLastLocation(function)
    }

    @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    fun await(): LatLng {
        var result: LatLng? = null
        loadLastLocation(object : OnLocationUpdateListener {
            override fun onLocationUpdated(location: Location) {
                result = location.latLng
                mLock.withLock { mCondition.signal() }
            }
        })
        if (result != null) return result!!
        mLock.withLock { mCondition.await(10, TimeUnit.SECONDS) }
        return result ?: mLastLocation?.latLng ?: LatLng(0.0, 0.0)
    }

}