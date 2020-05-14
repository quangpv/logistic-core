package com.support.core.base

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.support.core.*
import com.support.core.extension.lazyNone
import com.support.core.functional.FragmentVisibleObserver
import com.support.core.functional.LocalStore
import com.support.core.functional.LocalStoreOwner

abstract class BaseFragment(contentLayoutId: Int) : Fragment(contentLayoutId), ResultOwner,
    LocalStoreOwner, Dispatcher {
    private val mResultRegistry = ResultRegistry()
    private val visibleRegistry get() = visibleOwner.lifecycle as ViewLifecycleRegistry

    override val localStore: LocalStore by lazyNone { LocalStore() }

    open val visibleOwner: LifecycleOwner by lazyNone {
        VisibleLifecycleOwner(this)
    }

    override val resultLife: ResultLifecycle get() = mResultRegistry

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        visibleRegistry.create()
        if (this is FragmentVisibleObserver) visibleRegistry.addObserver(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        visibleRegistry.destroy()
        localStore.clear()
    }

    override fun onStart() {
        super.onStart()
        visibleRegistry.start()
    }

    override fun onStop() {
        super.onStop()
        visibleRegistry.stop()
    }

    override fun onResume() {
        super.onResume()
        visibleRegistry.resume()
    }

    override fun onPause() {
        super.onPause()
        visibleRegistry.pause()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        val registry = visibleRegistry as? VisibleLifecycleRegistry ?: return
        registry.hide(hidden)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mResultRegistry.handleActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mResultRegistry.handlePermissionsResult(requestCode, permissions, grantResults)
    }

}
