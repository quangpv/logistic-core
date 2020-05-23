package com.support.location.engine

import com.google.android.gms.maps.model.LatLng

class LocationOptions(
    val minDistance: Float = 0f,
    val interval: Long = 1000L,
    val default: LatLng = LatLng(0.0, 0.0)
) {
    companion object {
        val DEFAULT = LocationOptions(0f, 1000L)
    }
}