package com.support.core

import androidx.arch.core.executor.ArchTaskExecutor
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class CoreTestRule : TestWatcher() {

    override fun starting(description: Description?) {
        super.starting(description)
        AppExecutors.setDelegate(InstantTaskExecutors())
        ArchTaskExecutor.getInstance().setDelegate(InstantArchTaskExecutor())
    }

    override fun finished(description: Description?) {
        super.finished(description)
        AppExecutors.setDelegate(null)
        ArchTaskExecutor.getInstance().setDelegate(null)
    }
}
