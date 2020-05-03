package com.support.location.map

import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Handler
import android.os.SystemClock
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
import com.support.location.engine.LocationEngine
import com.support.location.engine.OnLocationUpdateListener
import com.support.location.isEmpty
import com.support.location.latLng
import com.support.location.location
import com.support.location.map.marker.CircleDrawable


abstract class MapAdapter(private val fragment: SupportMapFragment) {

    companion object {
        const val DEFAULT_ZOOM = 15f
    }

    protected open val shouldUseBearing: Boolean get() = false
    protected open val shouldUseMarkerMyLocation: Boolean get() = true

    val hasLocationEngine: Boolean get() = mEngine != null

    private var mLayoutAlready = false
    private val mCallQueue = arrayListOf<(GoogleMap) -> Unit>()
    private var mEngine: LocationEngine? = null
    private var mMap: GoogleMap? = null

    private val handler = Handler()
    private var mLocationMarker: Marker? = null
    val view get() = fragment.requireView()

    private var mOnLocationUpdateListener = object : OnLocationUpdateListener {
        override fun onLocationUpdated(location: Location) {
            setMyLocation(location.latLng)
            onMyLocationChanged(location)
        }
    }

    protected val context get() = fragment.requireContext()
    private val isReady get() = mMap != null && mLayoutAlready && fragment.isVisible

    init {
        fragment.requireView().addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            if (!mLayoutAlready) {
                mLayoutAlready = right > left && bottom > top
                notifyMapLoadedIfCan()
            }
        }
        fragment.getMapAsync {
            mMap = it
            mMap!!.setOnMapLoadedCallback {
                notifyMapLoadedIfCan()
            }
        }

        fragment.viewLifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @Suppress("unused")
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onEvent() {
                mEngine = null
                mMap = null
                onDestroy()
            }
        })
    }

    private fun notifyMapLoadedIfCan() {
        if (!isReady) return
        onMapLoaded(mMap!!)
        synchronized(mCallQueue) {
            mCallQueue.forEach { it(mMap!!) }
            mCallQueue.clear()
        }
    }

    protected fun launch(function: (GoogleMap) -> Unit) {
        if (isReady) function(mMap!!)
        else synchronized(mCallQueue) { mCallQueue.add(function) }
    }

    protected open fun onDestroy() {}

    protected open fun onMapLoaded(map: GoogleMap) {}

    fun setLocationEngine(engine: LocationEngine) {
        if (mEngine == engine) return
        mEngine?.unsubscribe(mOnLocationUpdateListener)
        mEngine = engine
        engine.subscribe(fragment.viewLifecycleOwner, mOnLocationUpdateListener)
    }

    protected open fun onMyLocationChanged(location: Location) {
    }

    fun setMyLocation(latLng: LatLng) = launch {
        if (!shouldUseMarkerMyLocation) return@launch
        if (latLng.isEmpty) return@launch
        if (mLocationMarker == null) {
            mLocationMarker = it.addMarker(
                    MarkerOptions()
                            .flat(true)
                            .icon(onCreateMyLocationIcon())
                            .anchor(0.5f, 0.5f)
                            .position(latLng)
            )
            onMyLocationFirstDetected(latLng.location)
        } else mLocationMarker!!.position = latLng
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
