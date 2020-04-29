package com.support.core.base

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.support.core.*
import com.support.core.functional.LocalStore
import com.support.core.functional.LocalStoreOwner

abstract class BaseActivity(contentLayoutId: Int) : AppCompatActivity(contentLayoutId),
    Dispatcher, ResultOwner, LocalStoreOwner {
    private val mResultRegistry = ResultRegistry()
    override val resultLife: ResultLifecycle get() = mResultRegistry
    override val localStore: LocalStore by lazy(LazyThreadSafetyMode.NONE) { LocalStore() }

    val permissionChecker: PermissionChecker by lazy(LazyThreadSafetyMode.NONE) {
        PermissionChecker(this)
    }

    val preventOpenFromIconApp: Boolean
        get() = !isTaskRoot
                && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
                && Intent.ACTION_MAIN == intent.action

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mResultRegistry.handleActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        localStore.clear()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mResultRegistry.handlePermissionsResult(requestCode, permissions, grantResults)
    }

    fun notSupportYet() {
        Toast.makeText(this, "Not support yet!", Toast.LENGTH_SHORT).show()
    }
}