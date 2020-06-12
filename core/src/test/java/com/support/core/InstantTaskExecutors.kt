package com.support.core

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService

class InstantTaskExecutors : TaskExecutors {
    override val scheduler: ScheduledExecutorService
        get() = InstantExecutor()
    override val diskIO: Executor
        get() = InstantExecutor()
    override val mainIO: Executor
        get() = InstantExecutor()
    override val launchIO: ExecutorService
        get() = InstantExecutor()
    override val concurrentIO: ExecutorService
        get() = InstantExecutor()
    override val isOnMainThread: Boolean
        get() = true

}