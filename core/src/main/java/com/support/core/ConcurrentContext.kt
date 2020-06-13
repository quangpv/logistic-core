package com.support.core

import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val asyncIO get() = AppExecutors.concurrentIO
private val launchIO get() = AppExecutors.launchIO

class ConcurrentContext {
    private val mScopes = arrayListOf<ConcurrentScope>()

    fun cancel() = synchronized(this) {
        mScopes.forEach { it.cancel() }
    }

    fun launch(function: ConcurrentScope.() -> Unit) = synchronized(this) {
        val scope = ConcurrentScope(this)
        mScopes.add(scope)
        scope.execute(function)
    }

    fun remove(scope: ConcurrentScope) = synchronized(this) {
        mScopes.remove(scope)
    }
}

class ConcurrentScope(private val context: ConcurrentContext) {
    private var mLaunch: Future<*>? = null
    private val mTasks = arrayListOf<Deferred<*>>()

    internal fun cancel(ignore: DeferredError) = synchronized(this) {
        mTasks.forEach { if (it != ignore.deferred) it.cancel(ignore.error) }
    }

    fun cancel():Unit = synchronized(this) {
        mTasks.forEach { it.cancel() }
        mLaunch?.cancel(true)
    }

    fun <T> task(function: () -> T): Deferred<T> = synchronized(this) {
        Deferred(this, function).also { mTasks.add(it) }
    }

    fun <T> ConcurrentExecutable<T>.await(): T {
        return execute(this@ConcurrentScope)
    }

    internal fun remove(deferred: Deferred<*>) = synchronized(this) {
        mTasks.remove(deferred)
        if (mTasks.isEmpty()) {
            mLaunch = null
            context.remove(this)
        }
    }

    internal fun execute(function: ConcurrentScope.() -> Unit) {
        if (mLaunch != null) return

        mLaunch = launchIO.submit {
            try {
                function()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}

class ConcurrentContinue<T>(private val lock: ReentrantLock) {
    private var mDone: Boolean = false
    private var mResult: Any? = null
    private val mCondition = lock.newCondition()

    fun success(result: T) {
        lock.withLock {
            mResult = result
            mDone = true
            mCondition.signal()
        }
    }

    fun error(exception: Throwable) {
        lock.withLock {
            mDone = true
            mResult = exception
            mCondition.signal()
        }
    }

    fun await(): T {
        if (mDone) return handleResult()
        lock.withLock { mCondition.await() }
        return handleResult()
    }

    @Suppress("unchecked_cast")
    private fun handleResult(): T {
        val result = mResult
        if (result is Throwable) throw result
        return result as T
    }
}

fun <T> continuation(
        lock: ReentrantLock = ReentrantLock(),
        function: (ConcurrentContinue<T>) -> Unit
): T {
    val con = ConcurrentContinue<T>(lock)
    function(con)
    return con.await()
}

fun <T> concurrentScope(function: ConcurrentScope. () -> T): ConcurrentExecutable<T> {
    return ConcurrentExecutable(function)
}

class ConcurrentExecutable<T>(private val function: ConcurrentScope.() -> T) {
    fun execute(concurrentScope: ConcurrentScope): T {
        return function(concurrentScope)
    }
}

class DeferredError(val deferred: Deferred<*>, val error: Throwable)

class Deferred<T>(private val scope: ConcurrentScope, function: () -> T) {

    private var mScopeError: Throwable? = null
    private val mFuture = asyncIO.submit(Callable<T> {
        try {
            function()
        } catch (e: Throwable) {
            if (e is InterruptedException || e is CancellationException) throw e
            scope.cancel(DeferredError(this, e))
            throw e
        } finally {
            scope.remove(this)
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

operator fun Deferred<out Any>.plus(task: Deferred<Unit>): List<Deferred<out Any>> {
    return arrayListOf(this, task)
}

fun <T, E : Deferred<T>> List<E>.await(): List<T> {
    return map { it.await() }
}