package com.support.location

import android.annotation.SuppressLint
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.lifecycle.LifecycleOwner

class WaitUntil(private val accept: () -> Boolean) {
    private var mTask: RoundTask? = null

    fun start(function: () -> Unit) {
        cancel()

        mTask = object : RoundTask() {
            override val isDone: Boolean
                get() = accept()

            @SuppressLint("RestrictedApi")
            override fun onDone() {
                ArchTaskExecutor.getMainThreadExecutor().execute(function)
            }
        }
        RoundRobin.push(mTask!!)
    }

    fun subscribe(owner: LifecycleOwner, function: () -> Unit) {
        mTask = object : LifeRoundTask(owner) {
            override val isDone: Boolean
                get() = accept()

            @SuppressLint("RestrictedApi")
            override fun onDone() {
                super.onDone()
                ArchTaskExecutor.getMainThreadExecutor().execute(function)
            }
        }
        RoundRobin.push(mTask!!)
    }

    fun cancel() {
        mTask?.cancel()
        mTask = null
    }
}
