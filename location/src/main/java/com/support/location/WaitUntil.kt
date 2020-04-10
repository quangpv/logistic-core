package com.support.location

import android.os.Handler
import android.os.Looper
import java.util.*

class WaitUntil(private val accept: () -> Boolean) {
    private var mTimer: Timer? = null
    private var mHandler = Handler(Looper.getMainLooper())

    fun start(function: () -> Unit) {
        mTimer?.purge()
        mTimer?.cancel()

        if (accept()) {
            function()
            return
        }

        mTimer = Timer()
        mTimer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (accept()) {
                    mHandler.post(function)
                    cancel()
                }
            }
        }, 1000, 1000)
    }

    private fun cancel() {
        mTimer?.purge()
        mTimer?.cancel()
    }
}
