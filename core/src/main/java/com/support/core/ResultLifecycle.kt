package com.support.core

import android.app.Activity
import android.content.Intent

interface ResultOwner {
    val resultLife: ResultLifecycle
}

const val REQUEST_FOR_RESULT_INSTANTLY = 17491

interface ResultLifecycle {
    fun onActivityResult(requestCode: Int = REQUEST_FOR_RESULT_INSTANTLY, callback: (resultCode: Int, data: Intent?) -> Unit)

    fun onActivitySuccessResult(requestCode: Int = REQUEST_FOR_RESULT_INSTANTLY, callback: (data: Intent?) -> Unit)

    fun onPermissionsResult(callback: (requestCode: Int, permissions: Array<out String>, grantResults: IntArray) -> Unit)

}

class ResultRegistry : ResultLifecycle {

    private val mPermissions = hashSetOf<(Int, Array<out String>, IntArray) -> Unit>()
    private val mActivityResults = hashMapOf<Int, (Int, Intent?) -> Unit>()
    private val mActivitySuccessResults = hashMapOf<Int, (Intent?) -> Unit>()

    override fun onPermissionsResult(callback: (requestCode: Int, permissions: Array<out String>, grantResults: IntArray) -> Unit) {
        mPermissions.add(callback)
    }

    override fun onActivitySuccessResult(requestCode: Int, callback: (data: Intent?) -> Unit) {
        mActivitySuccessResults[requestCode] = callback
    }

    override fun onActivityResult(requestCode: Int, callback: (resultCode: Int, data: Intent?) -> Unit) {
        mActivityResults[requestCode] = callback
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        mActivityResults.filter { requestCode == it.key }.forEach {
            it.value(resultCode, data)
            mActivityResults.remove(it.key)
        }
        if (resultCode != Activity.RESULT_OK) return

        mActivitySuccessResults.filter { requestCode == it.key }.forEach {
            it.value(data)
            mActivitySuccessResults.remove(it.key)
        }
    }

    fun handlePermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        mPermissions.forEach { it(requestCode, permissions, grantResults) }
        mPermissions.clear()
    }
}
