package com.support.core

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.support.core.base.BaseFragment

abstract class ViewLifecycleRegistry(provider: LifecycleOwner) : LifecycleRegistry(provider) {
    abstract fun create()

    abstract fun destroy()

    abstract fun start()

    abstract fun stop()

    abstract fun resume()

    abstract fun pause()
}

class VisibleLifecycleRegistry(provider: LifecycleOwner, private val fragment: Fragment) :
    ViewLifecycleRegistry(provider) {
    companion object {
        private const val STATE_NONE = -1
        private const val STATE_CREATED = 1
        private const val STATE_DESTROYED = 2
        private const val STATE_STARTED = 3
        private const val STATE_STOPPED = 4
        private const val STATE_RESUMED = 5
        private const val STATE_PAUSED = 6

        private val EVENT = hashMapOf(
            STATE_CREATED to Event.ON_CREATE,
            STATE_DESTROYED to Event.ON_DESTROY,
            STATE_STARTED to Event.ON_START,
            STATE_STOPPED to Event.ON_STOP,
            STATE_RESUMED to Event.ON_RESUME,
            STATE_PAUSED to Event.ON_PAUSE
        )
    }

    private var mCurrentState = STATE_NONE

    private val Fragment.isVisibleOnScreen: Boolean
        get() = !isHidden && (parentFragment?.isVisibleOnScreen ?: true)

    private val childVisibleLifecycle
        get() = (fragment.childFragmentManager.fragments
            .find { !it.isHidden } as? BaseFragment)
            ?.visibleOwner?.lifecycle as? VisibleLifecycleRegistry

    override fun create() {
        next(STATE_CREATED)
    }

    override fun destroy() {
        next(STATE_DESTROYED)
    }

    override fun start() {
        if (fragment.isVisibleOnScreen) next(STATE_STARTED)
    }

    override fun stop() {
        if (fragment.isVisibleOnScreen) next(STATE_STOPPED)
    }

    override fun resume() {
        if (fragment.isVisibleOnScreen) next(STATE_RESUMED)
    }

    override fun pause() {
        if (fragment.isVisibleOnScreen) next(STATE_PAUSED)
    }

    fun hide(hidden: Boolean) {
        if (hidden) {
            hidePause()
            hideStop()
        } else {
            showStart()
            showResume()
        }
    }

    private fun hidePause() {
        childVisibleLifecycle?.hidePause()
        next(STATE_PAUSED)
    }

    private fun hideStop() {
        childVisibleLifecycle?.hideStop()
        next(STATE_STOPPED)
    }

    private fun showStart() {
        next(STATE_STARTED)
        childVisibleLifecycle?.showStart()
    }

    private fun showResume() {
        next(STATE_RESUMED)
        childVisibleLifecycle?.showResume()
    }

    private fun next(state: Int): VisibleLifecycleRegistry {
        if (mCurrentState == state) return this
        val event = EVENT[state] ?: error("Not accept state $state")
        Log.i("LifecycleEvent", "${fragment.javaClass.simpleName} - $event")
        handleLifecycleEvent(event)
        mCurrentState = state
        return this
    }

}

class VisibleHintLifecycleRegistry(provider: LifecycleOwner) : ViewLifecycleRegistry(provider) {
    override fun create() {
        handleLifecycleEvent(Event.ON_CREATE)
    }

    override fun destroy() {
        handleLifecycleEvent(Event.ON_DESTROY)
    }

    override fun start() {
    }

    override fun stop() {
    }

    override fun resume() {
        handleLifecycleEvent(Event.ON_START)
        handleLifecycleEvent(Event.ON_RESUME)
    }

    override fun pause() {
        handleLifecycleEvent(Event.ON_PAUSE)
        handleLifecycleEvent(Event.ON_STOP)
    }
}

class VisibleLifecycleOwner(fragment: Fragment) : LifecycleOwner {
    private val mLifecycle = VisibleLifecycleRegistry(this, fragment)

    override fun getLifecycle(): Lifecycle {
        return mLifecycle
    }
}

class VisibleHintLifecycleOwner : LifecycleOwner {
    private val mLifecycle = VisibleHintLifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle {
        return mLifecycle
    }
}
