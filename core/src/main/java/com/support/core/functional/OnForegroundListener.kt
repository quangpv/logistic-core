package com.support.core.functional

import android.app.Activity
import android.app.Application
import android.os.Bundle

class OnForegroundListener(private val function: (isCreate: Boolean) -> Unit) :
    OnAppRunningListener() {
    override fun onBackground() {
        function(false)
    }

    override fun onForeground() {
        function(true)
    }
}

abstract class OnAppRunningListener :
    Application.ActivityLifecycleCallbacks {
    private var numOfStart = 0
    private var isConfigChanging = false

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityDestroyed(activity: Activity) {
        isConfigChanging = activity.isChangingConfigurations
        numOfStart -= 1
        if (numOfStart == 0 && !isConfigChanging) {
            onBackground()
        }
    }


    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        numOfStart++
        if (numOfStart == 1 && !isConfigChanging) {
            onForeground()
        }
    }

    abstract fun onBackground()

    abstract fun onForeground()

    override fun onActivityResumed(activity: Activity) {
    }

}

class ForegroundDetector : OnAppRunningListener() {
    override fun onBackground() {
        foreground = false
    }

    override fun onForeground() {
        foreground = true
    }

    var foreground: Boolean = false
        private set

}