package com.support.core

import android.os.Looper
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

private val networkIO get() = AppExecutors.concurrentIO
private val launchIO get() = AppExecutors.networkIO

class ConcurrentContext {
    private val mScopes = arrayListOf<ConcurrentScope>()

    fun cancel() {
        mScopes.forEach { it.cancel() }
        mScopes.clear()
    }

    fun launch(function: ConcurrentScope.() -> Unit) {
        mScopes.add(newScope(function))
    }

    private fun newScope(function: ConcurrentScope.() -> Unit) = ConcurrentScope().apply {
        val executor = Runnable {
            try {
                function()
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                mScopes.remove(this)
            }
        }
        if (Looper.getMainLooper() != Looper.myLooper()) executor.run()
        else launchIO.execute(executor)
    }
}

class ConcurrentScope {

    private val mTasks = arrayListOf<Promise<*>>()

    fun cancel(ignore: PromiseError) {
        mTasks.forEach { if (it != ignore.promise) it.cancel(ignore.error) }
        mTasks.clear()
    }

    fun cancel() {
        mTasks.forEach { it.cancel() }
        mTasks.clear()
    }

    fun <T> task(function: () -> T): Promise<T> {
        return Promise(this, function).also { mTasks.add(it) }
    }

    fun <T> ConcurrentExecutable<T>.await(): T {
        return execute(this@ConcurrentScope)
    }
}

fun <T> concurrentScope(function: ConcurrentScope. () -> T): ConcurrentExecutable<T> {
    return ConcurrentExecutable(function)
}

class ConcurrentExecutable<T>(private val function: ConcurrentScope.() -> T) {
    fun execute(concurrentScope: ConcurrentScope): T {
        return function(concurrentScope)
    }
}

class PromiseError(val promise: Promise<*>, val error: Throwable)

class Promise<T>(private val scope: ConcurrentScope, function: () -> T) {
    private var mScopeError: Throwable? = null
    private val mFuture = networkIO.submit(Callable<T> {
        try {
            function()
        } catch (e: Throwable) {
            if (e is InterruptedException || e is CancellationException) throw e
            scope.cancel(PromiseError(this, e))
            throw e
        }
    })

    fun await(): T {
        return try {
            mFuture.get()
        } catch (e: Throwable) {
            if ((e is InterruptedException || e is CancellationException)
                && mScopeError != null
            ) throw mScopeError!!

            if (e is ExecutionException) throw e.cause ?: e
            throw e
        }
    }

    fun cancel(error: Throwable? = null) {
        if (mFuture.isCancelled || mFuture.isDone) return
        mScopeError = error
        mFuture.cancel(true)
    }
}

operator fun Promise<out Any>.plus(task: Promise<Unit>): List<Promise<out Any>> {
    return arrayListOf(this, task)
}

fun <E : Promise<out Any>> List<E>.await(): List<Any> {
    return map { it.await() }
}