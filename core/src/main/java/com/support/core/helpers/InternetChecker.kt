package com.support.core.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LifecycleOwner
import com.support.core.Inject
import com.support.core.extension.isInternetAvailable
import com.support.core.extension.isNetworkConnected
import com.support.core.functional.FullLifecycleObserver

@Inject(true)
class InternetChecker(private val context: Context) {
    val isAccessed: Boolean get() = isEnabled && isAvailable
    val isEnabled: Boolean get() = context.isNetworkConnected
    val isAvailable: Boolean get() = isInternetAvailable

    fun check() {
        if (!isEnabled) throw NetworkDisableException()
        if (!isAvailable) throw NoInternetException()
    }

    fun subscribe(owner: LifecycleOwner, function: (Boolean) -> Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                function(isEnabled)
            }

        }
        context.registerReceiver(receiver, IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))
        owner.lifecycle.addObserver(object : FullLifecycleObserver {
            override fun onDestroy() {
                owner.lifecycle.removeObserver(this)
                context.unregisterReceiver(receiver)
            }
        })
    }
}

class NetworkDisableException : Throwable("No network connection.")
class NoInternetException : Throwable("Internet interrupted.")