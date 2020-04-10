package com.support.location.place

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName
import com.support.location.PolyUtils

class Direction(
        private val routes: List<Route>? = null,
        private val status: String? = null
) {

    val isStatusOk: Boolean
        get() = status == "OK"

    val points: List<LatLng>
        get() = routes?.get(0)
                ?.overviewPolyline
                ?.points ?: arrayListOf()

    val distance: Double
        get() = routes?.get(0)
                ?.legs?.get(0)
                ?.distance
                ?.value ?: 0.0

    val duration: Double
        get() = routes?.get(0)
                ?.legs?.get(0)
                ?.duration
                ?.value ?: 0.0

    val hasRoute
        get() = routes?.isNotEmpty() ?: false

    val decode
        get() = routes?.get(0)
                ?.overviewPolyline
                ?.decode() ?: false
}

data class Legs(
        val steps: List<Steps>? = null,
        var distance: TextValue? = null,
        var duration: TextValue? = null
)

data class TextValue(
        var text: String? = null,
        var value: Double = 0.0
)

class Polyline(
        @SerializedName("points")
        private val mPoints: String? = null
) {

    @Transient
    lateinit var points: List<LatLng>

    fun decode(): Boolean {
        points = PolyUtils.decode(mPoints!!)
        return true
    }
}

class Route(
        val legs: List<Legs>? = null,

        @SerializedName("overview_polyline")
        val overviewPolyline: Polyline? = null
)

class Steps(
        @SerializedName("polyline")
        val polyline: Polyline? = null
)