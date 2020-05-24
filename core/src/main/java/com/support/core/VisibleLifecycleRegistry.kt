package com.support.core

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.support.core.base.BaseFragment
import com.support.core.base.childVisible
import com.support.core.base.isVisibleOnScreen

abstract class ViewLifecycleRegistry(provider: LifecycleOwner) : LifecycleRegistry(provider) {

    abstract fun create()

    abstract fun destroy()

    abstract fun start()

    abstract fun stop()

    abstract fun resume()

    abstract fun pause()

    abstract fun hide(hidden: Boolean)

    protected fun Fragment.dispatchHidden(hidden: Boolean) {
        ((childVisible as? BaseFragment)?.visibleOwner?.lifecycle as? ViewLifecycleRegistry)
                ?.hide(hidden)
    }
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

    override fun hide(hidden: Boolean) {
        fragment.dispatchHidden(hidden)

        if (hidden) {
            next(STATE_PAUSED)
            next(STATE_STOPPED)
        } else {
            next(STATE_STARTED)
            next(STATE_RESUMED)
        }
    }

    private fun next(state: Int): VisibleLifecycleRegistry {
        if (mCurrentState == state) return this
        val event = EVENT[state] ?: error("Not accept state $state")
        if (event != Event.ON_RESUME) Log.i("LifecycleEvent", "${fragment.javaClass.simpleName} - $event")
        handleLifecycleEvent(event)
        mCurrentState = state
        return this
    }

}

class CurrentResumeLifecycleRegistry(provider: LifecycleOwner, private val fragment: Fragment) : ViewLifecycleRegistry(provider) {
    private val name get() = fragment.javaClass.simpleName

    var isActivated: Boolean = false
        private set

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
        if (!fragment.isVisibleOnScreen) return
        isActivated = true
        doResume()
    }

    override fun pause() {
        if (!fragment.isVisibleOnScreen) return
        isActivated = false
        doPause()
    }

    private fun doResume() {
        handleLifecycleEvent(Event.ON_START)
        handleLifecycleEvent(Event.ON_RESUME)
        Log.i("LifecycleEvent", "$name - ON_START")
    }

    private fun doPause() {
        handleLifecycleEvent(Event.ON_PAUSE)
        handleLifecycleEvent(Event.ON_STOP)
        Log.i("LifecycleEvent", "$name - ON_STOP")
    }

    override fun hide(hidden: Boolean) {
        fragment.dispatchHidden(hidden)
        if (hidden) {
            doPause()
        } else {
            doResume()
        }
    }
}

class VisibleLifecycleOwner(fragment: Fragment) : LifecycleOwner {
    private val mLifecycle = VisibleLifecycleRegistry(this, fragment)

    override fun getLifecycle(): Lifecycle {
        return mLifecycle
    }
}

class CurrentResumeLifecycleOwner(fragment: Fragment) : LifecycleOwner {
    private val mLifecycle = CurrentResumeLifecycleRegistry(this, fragment)

    override fun getLifecycle(): Lifecycle {
        return mLifecycle
    }
}
