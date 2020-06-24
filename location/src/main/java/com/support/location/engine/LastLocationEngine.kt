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
    private val loader: LocationLoader
) : LocationEngine {
    companion object {
        const val TIME_TO_UPDATE = 5000L
    }

    private var mLastLocation: Location? = null
    private var mLastUpdate: Long = 0

    private val mLock = ReentrantLock()
    private val mCondition = mLock.newCondition()

    override val delegate: LocationEngine by lazy(LazyThreadSafetyMode.NONE) {
        object : LifecycleLocationDelegate(context) {
            private var mRemoved: Boolean = false

            @SuppressLint("MissingPermission")
            override fun onRequest(listener: OnLocationUpdateListener) {
                mRemoved = false

                loadLastLocation(object : OnLocationUpdateListener {
                    override fun onLocationUpdated(location: Location) {
                        if (mRemoved) return
                        listener.onLocationUpdated(location)
                    }
                })
            }

            override fun onRemove(listener: OnLocationUpdateListener) {
                mRemoved = true
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    fun loadLastLocation(function: OnLocationUpdateListener) =
        ArchTaskExecutor.getInstance().executeOnMainThread {
            if (System.currentTimeMillis() - mLastUpdate < TIME_TO_UPDATE && mLastLocation != null) {
                function.onLocationUpdated(mLastLocation!!)
                return@executeOnMainThread
            }
            loader.loadLastLocation(object : OnLocationUpdateListener {
                override fun onLocationUpdated(location: Location) {
                    mLastLocation = location
                    mLastUpdate = System.currentTimeMillis()
                    function.onLocationUpdated(location)
                }
            })
        }

    fun getLastLocation(function: OnLocationUpdateListener) {
        loader.getLastLocation(function)
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
        return result ?: mLastLocation?.latLng ?: loader.options.default
    }

}