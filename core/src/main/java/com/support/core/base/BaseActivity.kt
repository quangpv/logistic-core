package com.support.core.base

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.support.core.*

abstract class BaseActivity(contentLayoutId: Int) : AppCompatActivity(contentLayoutId),
    Dispatcher, ResultOwner {
    private val mResultRegistry = ResultRegistry()
    override val resultLife: ResultLifecycle get() = mResultRegistry
    val permissionChecker: PermissionChecker by lazy(LazyThreadSafetyMode.NONE) {
        PermissionChecker(this)
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

    fun notSupportYet() {
        Toast.makeText(this, "Not support yet!", Toast.LENGTH_SHORT).show()
    }
}