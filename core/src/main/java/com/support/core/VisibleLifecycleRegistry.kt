package com.support.core

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.support.core.base.BaseFragment
import com.support.core.base.isVisibleOnScreen
import java.lang.ref.WeakReference

abstract class ViewLifecycleRegistry(provider: LifecycleOwner, fragment: Fragment) : LifecycleRegistry(provider) {
    companion object {
        const val STATE_NONE = -1
        const val STATE_CREATED = 1
        const val STATE_DESTROYED = 2
        const val STATE_STARTED = 3
        const val STATE_STOPPED = 4
        const val STATE_RESUMED = 5
        const val STATE_PAUSED = 6

        private val EVENT = hashMapOf(
                STATE_CREATED to Event.ON_CREATE,
                STATE_DESTROYED to Event.ON_DESTROY,
                STATE_STARTED to Event.ON_START,
                STATE_STOPPED to Event.ON_STOP,
                STATE_RESUMED to Event.ON_RESUME,
                STATE_PAUSED to Event.ON_PAUSE
        )
        private const val KEY_IS_ACTIVATED = "key:view:lifecycle:registry"
    }

    private var mCurrentState = STATE_NONE
    protected abstract var isActivated: Boolean

    private val fragmentRef = WeakReference(fragment)

    val fragment: Fragment
        get() = fragmentRef.get()
                ?: error("Fragment of this LifecycleRegistry is already garbage collected")

    val isVisibleOnScreen get() = fragment.isVisibleOnScreen

    fun create(savedInstanceState: Bundle?) {
        isActivated = savedInstanceState?.getBoolean(KEY_IS_ACTIVATED, false) ?: false
        next(STATE_CREATED)
    }

    fun destroy() {
        next(STATE_DESTROYED)
    }

    abstract fun start()

    abstract fun stop()

    abstract fun resume()

    abstract fun pause()

    fun saveInstance(outState: Bundle) {
        outState.putBoolean(KEY_IS_ACTIVATED, isActivated)
    }

    fun hide(hidden: Boolean) {
        if (!fragment.lifecycle.currentState.isAtLeast(State.STARTED)) return
        isActivated = !hidden
        if (hidden) {
            sendHiddenState(STATE_PAUSED)
            sendHiddenState(STATE_STOPPED)
        } else {
            sendHiddenState(STATE_STARTED)
            sendHiddenState(STATE_RESUMED)
        }
    }

    protected fun next(state: Int) {
        if (mCurrentState == state) return
        val event = EVENT[state] ?: error("Not accept state $state")
        if (event != Event.ON_RESUME && event != Event.ON_PAUSE)
            Log.i("LifecycleEvent", "${fragment.javaClass.simpleName}- $event")
        handleLifecycleEvent(event)
        mCurrentState = state
    }

    private fun sendHiddenState(state: Int) {
        if (state == STATE_STARTED || state == STATE_RESUMED) {
            next(state)
            dispatchState(state)
        } else {
            dispatchState(state)
            next(state)
        }
    }

    private fun dispatchState(state: Int) {
        fragment.childFragmentManager.fragments.forEach { child ->
            val registry = (child as? BaseFragment)?.visibleOwner?.lifecycle as? ViewLifecycleRegistry
            if (registry != null && registry.isActivated) registry.sendHiddenState(state)
        }
    }
}

class VisibleLifecycleRegistry(
        provider: LifecycleOwner,
        fragment: Fragment
) : ViewLifecycleRegistry(provider, fragment) {

    override var isActivated: Boolean = false

    override fun start() {
        if (!isVisibleOnScreen) return
        isActivated = true
        next(STATE_STARTED)
    }

    override fun stop() {
        if (!isVisibleOnScreen) return
        isActivated = false
        next(STATE_STOPPED)
    }

    override fun resume() {
        if (isVisibleOnScreen) next(STATE_RESUMED)
    }

    override fun pause() {
        if (isVisibleOnScreen) next(STATE_PAUSED)
    }

}

class CurrentResumeLifecycleRegistry(
        provider: LifecycleOwner,
        fragment: Fragment
) : ViewLifecycleRegistry(provider, fragment) {
    override var isActivated: Boolean = false

    override fun start() {}

    override fun stop() {}

    override fun resume() {
        if (!isVisibleOnScreen) return
        isActivated = true
        doResume()
    }

    override fun pause() {
        if (!isVisibleOnScreen) return
        isActivated = false
        doPause()
    }

    private fun doResume() {
        next(STATE_STARTED)
        next(STATE_RESUMED)
    }

    private fun doPause() {
        next(STATE_PAUSED)
        next(STATE_STOPPED)
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
