package com.support.location.map

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.support.location.engine.LastLocationEngine
import com.support.location.engine.LocationEngine
import com.support.location.engine.OnLocationUpdateListener
import com.support.location.engine.UpdateLocationEngine
import com.support.location.latLng
import com.support.location.location
import com.support.location.map.marker.CircleDrawable


abstract class MapAdapter(private val fragment: SupportMapFragment) {

    companion object {
        const val DEFAULT_ZOOM = 15f
    }

    protected open val shouldUseBearing: Boolean get() = false
    val hasLocationEngine: Boolean get() = mEngine != null

    private var mEngine: LocationEngine? = null
    private val handler = Handler()

    private var mLocationMarker: Marker? = null
    private val mCallQueue = arrayListOf<(GoogleMap) -> Unit>()
    private var mMap: GoogleMap? = null
    protected val context get() = fragment.requireContext()

    private var mOnLocationUpdateListener = object : OnLocationUpdateListener {
        override fun onLocationUpdated(location: Location) {
            if (mEngine != null) updateMyLocation(location)
        }
    }

    init {
        fragment.getMapAsync {
            mMap = it
            onMapLoaded(mMap!!)
        }
        fragment.viewLifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @Suppress("all")
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onEvent() {
                val engine=mEngine
                if (engine is UpdateLocationEngine) engine.removeUpdatedListener(mOnLocationUpdateListener)
                mEngine = null
                onDestroy()
            }
        })
    }

    protected open fun onDestroy() {}

    protected open fun onMapLoaded(map: GoogleMap) {
        synchronized(mCallQueue) {
            mCallQueue.forEach { it(map) }
            mCallQueue.clear()
        }
    }

    @SuppressLint("MissingPermission")
    fun setLocationEngine(engine: LocationEngine) {
        mEngine = engine
        when (engine) {
            is LastLocationEngine -> engine.loadLastLocation {
                mOnLocationUpdateListener.onLocationUpdated(it.location)
            }
            is UpdateLocationEngine -> engine.addUpdatedListener(mOnLocationUpdateListener)
        }
    }

    private fun updateMyLocation(location: Location) = launch {
        it.getMyLocationMarker(location).animateTo(location)
        onMyLocationChanged(location)
        Log.e("MyLocation", location.toString())
    }

    protected open fun onMyLocationChanged(location: Location) {
    }

    private fun GoogleMap.getMyLocationMarker(location: Location): Marker {
        if (mLocationMarker == null) {
            mLocationMarker = addMarker(
                MarkerOptions()
                    .flat(true)
                    .icon(onCreateMyLocationIcon())
                    .anchor(0.5f, 0.5f)
                    .position(location.latLng)
            )
            onMyLocationFirstDetected(location)
        }
        return mLocationMarker!!
    }

    protected open fun onMyLocationFirstDetected(location: Location) = launch {
        it.moveCamera(CameraUpdateFactory.newLatLngZoom(location.latLng, DEFAULT_ZOOM))
    }

    private fun Marker.animateTo(location: Location) {
        val start = SystemClock.uptimeMillis()
        val startLatLng = position
        val startRotation = rotation
        val duration: Long = 500
        val interpolator = LinearInterpolator()

        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t = interpolator.getInterpolation(elapsed.toFloat() / duration)

                val lng = t * location.longitude + (1 - t) * startLatLng.longitude
                val lat = t * location.latitude + (1 - t) * startLatLng.latitude


                position = LatLng(lat, lng)

                if (shouldUseBearing) {
                    val rotation = (t * location.bearing + (1 - t) * startRotation)
                    setRotation(rotation)
                }

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16)
                }
            }
        })
    }

    protected open fun onCreateMyLocationIcon(): BitmapDescriptor {
        return BitmapDescriptorFactory
            .fromBitmap(CircleDrawable().toBitmap(100, 100))
    }

    protected fun launch(function: (GoogleMap) -> Unit) {
        if (mMap != null) function(mMap!!)
        else synchronized(mCallQueue) { mCallQueue.add(function) }
    }

    fun getDimen(id: Int): Int {
        return context.resources.getDimensionPixelSize(id)
    }

    fun getDrawable(id: Int): Drawable {
        return ContextCompat.getDrawable(context, id)!!
    }

    fun getColor(id: Int): Int {
        return ContextCompat.getColor(context, id)
    }

}

abstract class MarkerHolder<T>(val item: T) {

    protected var marker: Marker? = null
        private set

    fun bind(map: GoogleMap) {
        unBind()
        marker = map.addMarker(onCreateOptions())
        onBind(marker!!)
    }

    protected abstract fun onCreateOptions(): MarkerOptions

    protected open fun onBind(marker: Marker) {
    }

    protected open fun onUnBind() {
    }

    fun unBind() {
        marker?.remove()
        marker = null
        onUnBind()
    }
}
