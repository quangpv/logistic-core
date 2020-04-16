package com.support.location.engine

import android.location.Location

interface OnLocationUpdateListener {
    fun onLocationUpdated(location: Location)
}