package com.support.core

import java.util.concurrent.*

class InstantExecutor : ExecutorService, ScheduledExecutorService {
    override fun shutdown() {}

    override fun schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        return InstantFuture(Any()).also { command.run() }
    }

    override fun <V : Any?> schedule(callable: Callable<V>, delay: Long, unit: TimeUnit): ScheduledFuture<V> {
        return InstantFuture(callable.call())
    }

    override fun <T : Any?> submit(task: Callable<T>): Future<T> = InstantFuture(task.call())
    override fun <T : Any?> submit(task: Runnable, result: T): Future<T> = InstantFuture(result).also { task.run() }
    override fun submit(task: Runnable): Future<*> = InstantFuture(Any()).also { task.run() }
    override fun shutdownNow(): MutableList<Runnable> = mutableListOf()
    override fun isShutdown(): Boolean = true
    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = true

    override fun scheduleWithFixedDelay(command: Runnable, initialDelay: Long, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        return InstantFuture(Any())
    }

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>): T {
        return tasks.first().call()
    }

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit): T {
        return tasks.first().call()
    }

    override fun scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit): ScheduledFuture<*> {
        return InstantFuture(Any())
    }

    override fun isTerminated(): Boolean = true

    override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>): MutableList<Future<T>> {
        return tasks.map { InstantFuture<T>(it.call()) } as MutableList<Future<T>>
    }

    override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit): MutableList<Future<T>> {
        return tasks.map { InstantFuture<T>(it.call()) } as MutableList<Future<T>>
    }

    override fun execute(command: Runnable) {
        command.run()
    }
}