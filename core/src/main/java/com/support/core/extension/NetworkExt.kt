package com.support.core.extension

import android.content.Context
import android.net.ConnectivityManager
import java.net.InetAddress


val Context.isNetworkConnected: Boolean
    get() {
        val connectivityMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifi = connectivityMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        val mobile = connectivityMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
        if (wifi != null && wifi.isConnected) return true
        if (mobile != null && mobile.isConnected) return true
        return false
    }

val isInternetAvailable: Boolean
    get() = try {
        val address = InetAddress.getByName("google.com")
        address != null && address.address.isNotEmpty()
    } catch (e: Throwable) {
        false
    }