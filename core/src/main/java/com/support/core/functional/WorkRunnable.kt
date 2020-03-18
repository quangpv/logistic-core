package com.support.core.functional

import android.util.Log

interface WorkRunnable : Runnable {
    override fun run() {
        try {
            doWork()
        } catch (e: Throwable) {
            Log.d("WorkRunnable", e.message ?: "Unknown")
        }
    }

    fun doWork()
}