package com.support.core.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LifecycleOwner
import com.support.core.AppExecutors
import com.support.core.Inject
import com.support.core.extension.block
import com.support.core.extension.isNetworkConnected
import com.support.core.functional.FullLifecycleObserver
import java.net.InetAddress
import java.util.*

@Inject(true)
class InternetChecker(private val context: Context) {
    private val mNetworkListeners = arrayListOf<(Boolean) -> Unit>()

    val isAccessed: Boolean get() = isEnabled && isAvailable
    val isEnabled: Boolean get() = context.isNetworkConnected
    val isAvailable: Boolean
        get() = isOnline.also {
            synchronized(this) { mAvailable = it }
        }

    private var mAvailable: Boolean? = null
    private var mScheduler: Timer? = null


    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isEnabled) AppExecutors.launchIO.submit(createLoop())
            else notifyAvailable(false)
        }
    }

    private fun createLoop() = object : TimerTask() {
        override fun run() {
            val oldAvailable = mAvailable
            val available = isAvailable
            if (oldAvailable != available) notifyAvailable(available)
        }
    }

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

    private fun notifyAvailable(available: Boolean) {
        synchronized(mNetworkListeners) {
            mNetworkListeners.forEach { it(available) }
        }
    }

    fun addOnAvailableListener(function: (Boolean) -> Unit) {
        if (mNetworkListeners.contains(function)) return
        synchronized(mNetworkListeners) { mNetworkListeners.add(function) }
    }

    fun removeOnAvailableListener(function: (Boolean) -> Unit) {
        mNetworkListeners.remove(function)
        synchronized(mNetworkListeners) { mNetworkListeners.remove(function) }
    }

    fun start() {
        stop()

        mScheduler = Timer()
        mScheduler!!.scheduleAtFixedRate(createLoop(), 1000, 2000)
        try {
            context.unregisterReceiver(mReceiver)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        context.registerReceiver(
            mReceiver,
            IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        )
    }

    fun stop() = block(mScheduler) {
        cancel()
        purge()
        mScheduler = null
        try {
            context.unregisterReceiver(mReceiver)
        } catch (e: Throwable) {
        }
    }

    private val isOnline: Boolean
        get() {
            return try {
                InetAddress.getByName("google.com").isReachable(300)
            } catch (t: Throwable) {
                false
            }
        }
}

class NetworkDisableException : Throwable("No network connection.")
class NoInternetException : Throwable("Internet interrupted.")