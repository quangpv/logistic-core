package com.support.location.engine

import android.content.Context
import com.support.location.engine.loader.LocationLoader

class MapLocationEngine(
        private val context: Context,
        private val loader: LocationLoader
) : LocationEngine {
    override val delegate: LocationEngine by lazy(LazyThreadSafetyMode.NONE) {
        object : LifecycleLocationDelegate(context) {
            override fun onRequest(listener: OnLocationUpdateListener) {
                loader.requestUpdate(listener)
            }

            override fun onRemove(listener: OnLocationUpdateListener) {
                loader.removeUpdate(listener)
            }
        }
    }

    fun requestUpdate(listener: OnLocationUpdateListener) {
        loader.requestUpdate(listener)
    }

    fun removeUpdate(listener: OnLocationUpdateListener) {
        loader.removeUpdate(listener)
    }
}
