package com.support.location.engine

class LocationOptions(
        val minDistance: Float = 0f,
        val interval: Long = 1000L
) {
    companion object {
        val DEFAULT = LocationOptions(0f, 1000L)
    }
}