package com.support.core.extension

import android.content.Context
import android.net.ConnectivityManager
import java.net.InetAddress


val Context.isNetworkConnected: Boolean
    get() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
    }

val isInternetAvailable: Boolean
    get() = try {
        val address = InetAddress.getByName("google.com")
        address != null && address.address.isNotEmpty()
    } catch (e: Throwable) {
        false
    }