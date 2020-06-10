package com.support.core.extension

import android.content.Context
import android.net.ConnectivityManager


val Context.isNetworkConnected: Boolean
    get() {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return connectivityManager != null && (connectivityManager
            .activeNetworkInfo?.isConnectedOrConnecting ?: false)
    }