package com.support.core

import java.util.concurrent.Delayed
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class InstantFuture<T>(private val result: T) : ScheduledFuture<T> {

    override fun isDone(): Boolean = true
    override fun get(): T {
        return result
    }

    override fun get(timeout: Long, unit: TimeUnit): T {
        return get()
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = true
    override fun isCancelled(): Boolean = false
    override fun compareTo(other: Delayed?): Int = 0
    override fun getDelay(unit: TimeUnit): Long = 0
}