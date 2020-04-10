package com.support.location

import com.google.android.gms.maps.model.LatLng
import java.util.ArrayList

object PolyUtils {

    /**
     * Returns true if the provided list of points is a closed polygon (i.e., the first and last
     * points are the same), and false if it is not
     *
     * @param poly polyline or polygon
     * @return true if the provided list of points is a closed polygon (i.e., the first and last
     * points are the same), and false if it is not
     */
    fun isClosedPolygon(poly: List<LatLng>): Boolean {
        val firstPoint = poly[0]
        val lastPoint = poly[poly.size - 1]
        return firstPoint == lastPoint
    }

    /**
     * Decodes an encoded path string into a sequence of LatLngs.
     */
    fun decode(encodedPath: String): List<LatLng> {
        val len = encodedPath.length

        // For speed we preallocate to an upper bound on the final length, then
        // truncate the array before returning.
        val path = ArrayList<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < len) {
            var result = 1
            var shift = 0
            var b: Int
            do {
                b = encodedPath[index++].toInt() - 63 - 1
                result += b shl shift
                shift += 5
            } while (b >= 0x1f)
            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            result = 1
            shift = 0
            do {
                b = encodedPath[index++].toInt() - 63 - 1
                result += b shl shift
                shift += 5
            } while (b >= 0x1f)
            lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            path.add(LatLng(lat * 1e-5, lng * 1e-5))
        }

        return path
    }

    /**
     * Encodes a sequence of LatLngs into an encoded path string.
     */
    fun encode(path: List<LatLng>): String {
        var lastLat: Long = 0
        var lastLng: Long = 0

        val result = StringBuffer()

        for (point in path) {
            val lat = Math.round(point.latitude * 1e5)
            val lng = Math.round(point.longitude * 1e5)

            val dLat = lat - lastLat
            val dLng = lng - lastLng

            encode(dLat, result)
            encode(dLng, result)

            lastLat = lat
            lastLng = lng
        }
        return result.toString()
    }

    private fun encode(point: Long, result: StringBuffer) {
        var v = point
        v = if (v < 0) (v shl 1).inv() else v shl 1
        while (v >= 0x20) {
            result.append(Character.toChars((0x20 or ((v and 0x1f).toInt())) + 63))
            v = v shr 5
        }
        result.append(Character.toChars((v + 63).toInt()))
    }
}
