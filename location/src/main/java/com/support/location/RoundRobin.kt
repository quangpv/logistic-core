package com.support.location

import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object RoundRobin {
    private val mLock = ReentrantLock()
    private var mThread: RoundThread? = null
    private val mQueue = Queue()

    fun push(task: RoundTask) {
        if (task.isDone) {
            task.onDone()
            return
        }
        mQueue.push(task)
        if (mThread == null) {
            mThread = RoundThread().apply { start() }
        }
    }

    fun remove(task: RoundTask) {
        mQueue.remove(task)
    }

    private class Queue {
        val head: RoundTask? get() = mHead
        val isEmpty: Boolean get() = size == 0
        var size: Int = 0
            private set

        private var mHead: RoundTask? = null
        private var mLast: RoundTask? = null

        fun push(task: RoundTask) {
            if (task.isCancel) return
            size++
            if (mHead == null) {
                mHead = task
                mLast = task
            } else {
                mLast!!.linkNext(task)
                mLast = task
            }
        }

        fun remove(task: RoundTask) {
            size -= 1
            when {
                mHead == task && mLast == task -> {
                    mHead = null
                    mLast = null
                }
                mHead == task -> {
                    mHead = task.next
                    task.unLink()
                }
                mLast == task -> {
                    mLast = task.previous
                    task.unLink()
                }
            }
        }
    }

    private class RoundThread : Thread() {
        private val mCondition = mLock.newCondition()
        override fun run() {
            try {
                mainLoop()
            } finally {
                mThread = null
            }
        }

        private fun mainLoop() {
            while (true) {
                try {
                    mLock.withLock { mCondition.await(1, TimeUnit.SECONDS) }
                    if (mQueue.isEmpty) break
                    var task = mQueue.head
                    while (task != null) {
                        when {
                            synchronized(task) { task!!.isCancel } -> mQueue.remove(task)
                            task.isDone -> {
                                task.onDone()
                                mQueue.remove(task)
                            }
                        }
                        task = task.next
                    }

                } catch (t: InterruptedException) {
                }
            }
        }
    }
}

abstract class RoundTask {
    private var mCancel: Boolean = false
    val isCancel: Boolean get() = mCancel
    var next: RoundTask? = null
    var previous: RoundTask? = null
    abstract val isDone: Boolean
    abstract fun onDone()

    fun linkNext(task: RoundTask) {
        next = task
        task.previous = this
    }

    fun unLink() {
        previous?.next = next
        next?.previous = previous
    }

    @CallSuper
    open fun cancel() = synchronized(this) {
        mCancel = true
    }
}

abstract class LifeRoundTask(owner: LifecycleOwner) : RoundTask() {
    private val lifecycle = owner.lifecycle

    private val observer = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            RoundRobin.remove(this@LifeRoundTask)
            cancel()
        }
    }

    init {
        lifecycle.addObserver(observer)
    }

    @CallSuper
    override fun cancel() {
        super.cancel()
        lifecycle.removeObserver(observer)
    }

    @CallSuper
    override fun onDone() {
        lifecycle.removeObserver(observer)
    }
}