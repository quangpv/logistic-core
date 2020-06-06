package com.support.core.functional

import android.app.Activity
import android.app.Application
import android.os.Bundle

class OnForegroundListener(private val function: (foreground: Boolean) -> Unit) :
        ApplicationLifecycleObserver() {
    override fun onBackground() {
        function(false)
    }

    override fun onForeground() {
        function(true)
    }
}

abstract class ApplicationLifecycleObserver :
        Application.ActivityLifecycleCallbacks {
    private var numOfForeground = 0
    private var numOfStart = 0
    private var isConfigChanging = false
    private var isStartConfigChanging = false

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        numOfForeground++
        if (numOfForeground == 1 && !isConfigChanging) {
            onForeground()
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        isConfigChanging = activity.isChangingConfigurations
        numOfForeground -= 1
        if (numOfForeground == 0 && !isConfigChanging) {
            onBackground()
        }
    }

    override fun onActivityStarted(activity: Activity) {
        numOfStart++
        if (numOfStart == 1 && !isStartConfigChanging) {
            onStart()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        isStartConfigChanging = activity.isChangingConfigurations
        numOfStart -= 1
        if (numOfStart == 0 && !isStartConfigChanging) {
            onStop()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {

    }

    open fun onStart() {}

    open fun onStop() {}

    open fun onBackground() {}

    open fun onForeground() {}

}

class ForegroundDetector : ApplicationLifecycleObserver() {
    override fun onBackground() {
        foreground = false
    }

    override fun onStart() {
        start = true
    }

    override fun onStop() {
        start = false
    }

    override fun onForeground() {
        foreground = true
    }

    var start: Boolean = false
        private set
    var foreground: Boolean = false
        private set

}