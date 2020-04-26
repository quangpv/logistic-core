package com.support.core.functional

import android.util.Log
import androidx.annotation.CallSuper

interface WorkRunnable : Runnable {
    @CallSuper
    override fun run() {
        try {
            doWork()
        } catch (e: Throwable) {
            Log.d("WorkRunnable", e.message ?: "Unknown")
        }
    }

    fun doWork()
}