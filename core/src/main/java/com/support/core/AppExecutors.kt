package com.support.core

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

interface TaskExecutors {
    val diskIO: Executor
    val scheduler: ScheduledExecutorService
    val mainIO: Executor

    val launchIO: ExecutorService
    val concurrentIO: ExecutorService
}

class AppExecutors : TaskExecutors {

    override val diskIO: Executor = Executors.newSingleThreadExecutor()
    override val launchIO: ExecutorService = Executors.newFixedThreadPool(3)
    override val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
    override val mainIO: Executor = MainExecutor()
    override val concurrentIO: ExecutorService = Executors.newCachedThreadPool()

    companion object {
        private val sInstance: AppExecutors by lazy { AppExecutors() }
        private var mDelegate: TaskExecutors? = null

        val diskIO: Executor get() = mDelegate?.diskIO ?: sInstance.diskIO
        val scheduler: ScheduledExecutorService get() = mDelegate?.scheduler ?: sInstance.scheduler
        val mainIO: Executor get() = mDelegate?.mainIO ?: sInstance.mainIO

        val launchIO: ExecutorService get() = mDelegate?.launchIO ?: sInstance.launchIO
        val concurrentIO: ExecutorService get() = mDelegate?.concurrentIO ?: sInstance.concurrentIO

        fun setDelegate(delegate: TaskExecutors?) {
            mDelegate = delegate
        }

        fun <T> loadInBackGround(function: () -> T): ExecutorConcurrent<T> {
            return ExecutorConcurrent(function)
        }
    }
}

class ExecutorConcurrent<T>(private val function: () -> T) {

    fun postOnUi(uiFunction: (T) -> Unit) {
        AppExecutors.diskIO.execute {
            val result = function()
            AppExecutors.mainIO.execute {
                uiFunction(result)
            }
        }
    }
}

class MainExecutor : Executor {
    private val mHandler = Handler(Looper.getMainLooper())
    override fun execute(command: Runnable) {
        if (!isOnMainThread) mHandler.post(command)
        else command.run()
    }
}

val isOnMainThread get() = Looper.getMainLooper() == Looper.myLooper()

