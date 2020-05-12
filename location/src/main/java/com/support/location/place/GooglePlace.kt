package com.support.location.place

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class GooglePlace(context: Context) {

    private var mLastTask: Task<FindAutocompletePredictionsResponse>? = null
    private val mPlacesClient = Places.createClient(context)
    private val mToken = AutocompleteSessionToken.newInstance()
    private var geocoder = Geocoder(context, Locale.getDefault())
    private val mLock = ReentrantLock()

    private inner class Continuation<T> {
        private val condition = mLock.newCondition()
        private var result: T? = null
        private var error: Throwable? = null

        fun execute(): T = mLock.withLock {
            if (error != null) throw error!!
            if (result != null) return result!!

            condition.await(20L, TimeUnit.SECONDS)
            if (error != null) throw error!!
            if (result == null) throw TimeoutException()
            return result!!
        }

        fun success(result: T) = mLock.withLock {
            this.result = result
            condition.signal()
        }

        fun fail(exception: Throwable) = mLock.withLock {
            this.error = exception
            condition.signal()
        }
    }

    private fun <T> continuation(function: (Continuation<T>) -> Unit): T {
        return Continuation<T>().apply(function).execute()
    }

    fun search(query: String): AutocompletePrediction = continuation { con ->
        val request = FindAutocompletePredictionsRequest.builder()
                .setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(mToken)
                .setQuery(query)
                .build()

        mLastTask = mPlacesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            val result = response.autocompletePredictions.first()
                    ?: return@addOnSuccessListener
            con.success(result)
        }.addOnFailureListener { exception ->
            con.fail(exception)
        }.addOnCanceledListener {
            con.fail(CancellationException())
        }
    }

    fun getLatLng(placeId: String): LatLng = continuation { con ->
        val placeFields = listOf(Place.Field.ID, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)
        mPlacesClient.fetchPlace(request).addOnSuccessListener { response ->
            val latLng = response.place.latLng
            if (latLng != null) con.success(latLng)
            else con.fail(Throwable("Not found location on map"))
        }.addOnFailureListener { exception ->
            con.fail(exception)
        }.addOnCanceledListener {
            con.fail(CancellationException())
        }
    }

    fun getAddress(lat: Double, lng: Double): String {
        return try {
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            val address = addresses[0].getAddressLine(0)
//        var city = addresses[0].locality
//        var state = addresses[0].adminArea
//        var zip = addresses[0].postalCode
//        var country = addresses[0].countryName
            address
        } catch (e: Throwable) {
            "Unknown"
        }
    }
}