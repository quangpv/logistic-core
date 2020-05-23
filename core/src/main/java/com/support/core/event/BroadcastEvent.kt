package com.support.core.event

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.support.core.base.BaseFragment
import java.io.Serializable

class BroadcastEvent(val context: Context, uuid: String) :
    BaseBroadcastEvent<Any>(context, uuid) {
    override fun set(value: Any?) {
        context.sendBroadcast(Intent(uuid).also {
            when (value) {
                is Parcelable -> it.putExtra(uuid, value)
                is Serializable -> it.putExtra(uuid, value)
                else -> error("not support")
            }
        })
    }

    override fun shouldNotify(intent: Intent?): Boolean {
        return intent != null
    }

    override fun convert(intent: Intent?): Any? {
        return intent?.extras?.get(uuid)
    }
}

abstract class BaseBroadcastEvent<T>(private val context: Context, val uuid: String) : Event<T> {

    override fun set(value: T?) {
        error("Not support!")
    }

    fun subscribe(owner: LifecycleOwner, function: (T?) -> Unit) {
        observe(
            when (owner) {
                is BaseFragment -> owner.visibleOwner
                is Fragment -> owner.viewLifecycleOwner
                else -> owner
            }, function
        )
    }

    fun subscribeNotNull(owner: LifecycleOwner, function: (T) -> Unit) {
        observe(
            when (owner) {
                is BaseFragment -> owner.visibleOwner
                is Fragment -> owner.viewLifecycleOwner
                else -> owner
            }
        ) {
            if (it != null) function(it)
        }
    }

    fun observeNotNull(owner: LifecycleOwner, function: (T) -> Unit) {
        observe(owner) {
            if (it != null) function(it)
        }
    }

    override fun observe(owner: LifecycleOwner, function: (T?) -> Unit) {
        var data: T? = null
        var shouldNotify = false

        fun notifyIfNeeded() = synchronized(this) {
            if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
            if (!shouldNotify) return

            shouldNotify = false
            function(data)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (!shouldNotify(intent)) return
                data = convert(intent)
                shouldNotify = true
                notifyIfNeeded()
            }
        }

        owner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                owner.lifecycle.removeObserver(this)
                try {
                    context.unregisterReceiver(receiver)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onStart() {
                notifyIfNeeded()
            }
        })
        context.registerReceiver(receiver, IntentFilter(uuid))
    }

    protected open fun shouldNotify(intent: Intent?): Boolean = true
    protected abstract fun convert(intent: Intent?): T?
}
