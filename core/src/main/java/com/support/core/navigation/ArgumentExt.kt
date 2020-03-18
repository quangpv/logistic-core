package com.support.core.navigation

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import java.io.Serializable


inline fun <reified T : Serializable> Fragment.params() = FragmentParams.from<T>(arguments)

inline fun <reified T> Fragment.args() = arguments?.get(T::class.java.name) as? T

inline fun <reified T> Fragment.args(key: String) = arguments?.get(key) as? T

inline fun <reified T> Fragment.get(crossinline function: () -> T) = object : Lazy<T> {
    private var mValue: T? = null

    init {
        val lifecycleObserver = Observer<LifecycleOwner> {
            it.lifecycle.onStop { mValue = null }
        }
        lifecycle.onDestroy {
            viewLifecycleOwnerLiveData.removeObserver(lifecycleObserver)
        }
        viewLifecycleOwnerLiveData.observeForever(lifecycleObserver)
    }

    override val value: T
        get() {
            if (!isInitialized()) mValue = function()
            return mValue!!
        }

    override fun isInitialized() = mValue != null

}
