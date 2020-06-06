package com.support.core.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    private val mObservers = hashMapOf<(Boolean) -> Unit, ObserverWrapper>()

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

    fun observeAvailable(owner: LifecycleOwner, function: (Boolean) -> Unit) {
        doObserveAvailable({ LifecycleObserverWrapper(owner, function) }, function)
    }

    fun observeAvailable(function: (Boolean) -> Unit) {
        doObserveAvailable({ ObserverForeverWrapper(function) }, function)
    }

    fun removeAvailable(function: (Boolean) -> Unit) {
        val observer = mObservers[function] ?: return
        observer.detach()
    }

    private fun doObserveAvailable(get: () -> ObserverWrapper, function: (Boolean) -> Unit) {
        if (mObservers.containsKey(function)) return
        val observer = get()
        mObservers[function] = observer
        observer.attach()
    }

    fun addOnAvailableListener(function: (Boolean) -> Unit) {
        if (mNetworkListeners.contains(function)) return
        synchronized(mNetworkListeners) { mNetworkListeners.add(function) }
    }

    fun removeOnAvailableListener(function: (Boolean) -> Unit) {
        mNetworkListeners.remove(function)
        synchronized(mNetworkListeners) { mNetworkListeners.remove(function) }
    }

    private fun start() {
        stop()

        mScheduler = Timer()
        mScheduler!!.scheduleAtFixedRate(createLoop(), 1000, 2000)
        try {
            context.unregisterReceiver(mReceiver)
        } catch (e: Throwable) {
        }
        context.registerReceiver(
                mReceiver,
                IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        )
    }

    private fun stop() = block(mScheduler) {
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
                InetAddress.getByName("google.com").isReachable(1000)
            } catch (t: Throwable) {
                false
            }
        }

    abstract inner class ObserverWrapper(val function: (Boolean) -> Unit) : (Boolean) -> Unit {
        open fun detach() {
            removeOnAvailableListener(this)
            mObservers.remove(function)
            if (mObservers.size == 0) stop()
        }

        open fun attach() {
            if (mAvailable != null) function(mAvailable!!)
            addOnAvailableListener(this)
            if (mScheduler == null) start()
        }

        override fun invoke(p1: Boolean) {
            AppExecutors.mainIO.execute { function(p1) }
        }
    }

    inner class ObserverForeverWrapper(function: (Boolean) -> Unit) : ObserverWrapper(function)

    inner class LifecycleObserverWrapper(
            private val owner: LifecycleOwner,
            function: (Boolean) -> Unit
    ) : ObserverWrapper(function), LifecycleEventObserver {

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                detach()
            }
        }

        override fun detach() {
            owner.lifecycle.removeObserver(this)
            super.detach()
        }

        override fun attach() {
            super.attach()
            owner.lifecycle.addObserver(this)
        }
    }
}

class NetworkDisableException : Throwable("No network connection.")
class NoInternetException : Throwable("Internet interrupted.")