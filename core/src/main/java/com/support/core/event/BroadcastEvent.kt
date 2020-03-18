package com.support.core.event

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Parcelable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import java.io.Serializable
import java.util.*

class BroadcastEvent(private val context: Context) : Event<Any> {
    private val uuid = UUID.randomUUID().toString()

    override fun set(value: Any?) {
        context.sendBroadcast(Intent(uuid).also {
            when (value) {
                is Parcelable -> it.putExtra(uuid, value)
                is Serializable -> it.putExtra(uuid, value)
                else -> error("not support")
            }
        })
    }

    override fun observe(owner: LifecycleOwner, function: (Any?) -> Unit) {
        var data: Any? = null
        var shouldNotify = false

        fun notifyIfNeeded() = synchronized(this) {
            if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
            if (!shouldNotify) return

            shouldNotify = false
            function(data)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent ?: return
                data = intent.extras?.get(uuid)
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

}
