package com.support.location

import android.location.Location
import com.google.android.gms.maps.model.LatLng


val LatLng.location: Location
    get() = Location("").also {
        it.latitude = latitude
        it.longitude = longitude
    }

fun List<LatLng>.findNearestIndex(pos: LatLng): Int {
    var nearestPoint: LatLng = first()
    var nearestDistance: Float = Float.MAX_VALUE

    for (latLng in this) {
        val newNearestDistance = latLng.distanceTo(pos).coerceAtMost(nearestDistance)
        if (newNearestDistance != nearestDistance) {
            nearestDistance = newNearestDistance
            nearestPoint = latLng
        }
    }
    return indexOf(nearestPoint)
}

fun LatLng.distanceTo(latLng: LatLng): Float {
    return this.location.distanceTo(latLng.location)
}

val Location.latLng: LatLng
    get() = LatLng(latitude, longitude)