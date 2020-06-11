package com.support.core.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.support.core.AppExecutors
import com.support.core.extension.block
import com.support.core.extension.isNetworkConnected
import java.net.InetAddress

abstract class InternetMonitor(val context: Context) {
    companion object {
        fun create(context: Context): InternetMonitor {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                NetworkMonitor(context)
            } else {
                PingMonitor(context)
            }
        }
    }

    private var mAccessible: Boolean? = null
    private val mObservers = hashMapOf<(Boolean) -> Unit, ObserverWrapper>()

    private fun notifyChange() {
        mObservers.forEach { it.value.notifyChangeIfNeeded() }
    }

    open val isAccessible: Boolean
        get() = mAccessible ?: context.isNetworkConnected

    fun setAccessible(b: Boolean) {
        if (mAccessible == b) return
        mAccessible = b
        notifyChange()
    }

    fun subscribe(owner: LifecycleOwner, function: (Boolean) -> Unit) {
        if (mObservers.containsKey(function)) return
        LifecycleObserverWrapper(owner, function).attach()
    }

    fun subscribe(function: (Boolean) -> Unit) {
        if (mObservers.containsKey(function)) return
        ForeverObserverWrapper(function).attach()
    }

    fun unsubscribe(function: (Boolean) -> Unit) {
        if (mObservers.containsKey(function)) {
            mObservers[function]!!.detach()
        }
    }

    protected open fun onActive() {
    }

    protected open fun onInactive() {
    }

    private abstract inner class ObserverWrapper(val function: (Boolean) -> Unit) {
        private var mCurrentAccessible: Boolean? = null
        open fun attach() {
            mObservers[function] = this
            if (mObservers.size == 1) onActive()
        }

        open fun detach() {
            mObservers.remove(function)
            if (mObservers.size == 0) onInactive()
        }

        @CallSuper
        open fun notifyChangeIfNeeded() {
            if (mAccessible != null && mAccessible != mCurrentAccessible) {
                mCurrentAccessible = mAccessible
                AppExecutors.mainIO.execute { function(mAccessible!!) }
            }
        }
    }

    private inner class ForeverObserverWrapper(function: (Boolean) -> Unit) :
        ObserverWrapper(function)

    private inner class LifecycleObserverWrapper(
        private val owner: LifecycleOwner,
        function: (Boolean) -> Unit
    ) : ObserverWrapper(function), LifecycleEventObserver {

        override fun attach() {
            super.attach()
            owner.lifecycle.addObserver(this)
        }

        override fun detach() {
            super.detach()
            owner.lifecycle.removeObserver(this)
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) detach()
            else if (event == Lifecycle.Event.ON_START) {
                notifyChangeIfNeeded()
            }
        }

        override fun notifyChangeIfNeeded() {
            if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
            super.notifyChangeIfNeeded()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class NetworkMonitor(context: Context) : InternetMonitor(context) {
    private val mManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val mCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            setAccessible(true)
        }

        override fun onUnavailable() {
            setAccessible(false)
        }

        override fun onLost(network: Network) {
            setAccessible(false)
        }
    }

    override fun onActive() {
        mManager.registerNetworkCallback(NetworkRequest.Builder().build(), mCallback)
    }

    override fun onInactive() {
        mManager.unregisterNetworkCallback(mCallback)
    }
}

class PingMonitor(context: Context) : InternetMonitor(context) {
    private var mThread: Thread? = null
    private var mStop = false
    private val isOnline: Boolean
        get() {
            return try {
                InetAddress.getByName("8.8.8.8").isReachable(4000)
            } catch (t: Throwable) {
                false
            }
        }

    private fun start() {
        stop()
        mStop = true
        mThread = Thread {
            while (mStop) {
                try {
                    Thread.sleep(1000)
                } catch (e: Throwable) {
                }
                setAccessible(isOnline)
            }
        }
        mThread!!.start()
    }

    private fun stop() = block(mThread) {
        mStop = false
        interrupt()
    }

    override fun onActive() {
        start()
    }

    override fun onInactive() {
        stop()
    }

}