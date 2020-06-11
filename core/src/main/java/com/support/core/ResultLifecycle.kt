package com.support.core

import android.app.Activity
import android.content.Intent

interface ResultOwner {
    val resultLife: ResultLifecycle
}

const val REQUEST_FOR_RESULT_INSTANTLY = 17491

interface ResultLifecycle {
    /**
     * @see onInstantResult for alternate
     */
    @Deprecated("unused")
    fun onActivityResult(requestCode: Int = REQUEST_FOR_RESULT_INSTANTLY, callback: (resultCode: Int, data: Intent?) -> Unit)

    /**
     * @see onInstantSuccessResult for alternate
     */
    @Deprecated("unused")
    fun onActivitySuccessResult(requestCode: Int = REQUEST_FOR_RESULT_INSTANTLY, callback: (data: Intent?) -> Unit)


    /**
     * Registry onActivityResult for fragment or activity
     * It should be registry onCreate or onViewCreated if at fragment
     * and alive until onDestroy (if activity) and onViewDestroyed (if fragment) called
     *
     * @param requestCode Request code to request an activity
     * @param resultCode Result code return from activity finished
     * @param data Result data return from activity finished
     */
    fun onPeriodResult(requestCode: Int, callback: (resultCode: Int, data: Intent?) -> Unit)

    /**
     * Registry onActivityResult with result Activity.RESULT_OK received
     * @see onPeriodResult for the same behavior
     */
    fun onPeriodSuccessResult(requestCode: Int, callback: (data: Intent?) -> Unit)

    /**
     * Registry onActivityResult for fragment or activity
     * It should be registry once at event for request such as onClick ...
     * and will be un-registered after result received
     *
     * @param requestCode Request code to request an activity
     * @param resultCode Result code return from activity finished
     * @param data Result data return from activity finished
     */
    fun onInstantResult(requestCode: Int = REQUEST_FOR_RESULT_INSTANTLY, callback: (resultCode: Int, data: Intent?) -> Unit)

    /**
     * Registry onActivityResult with result Activity.RESULT_OK received
     * @see onInstantResult for the same behavior
     */
    fun onInstantSuccessResult(requestCode: Int = REQUEST_FOR_RESULT_INSTANTLY, callback: (data: Intent?) -> Unit)

    fun onPermissionsResult(requestCode: Int, callback: (permissions: Array<out String>, grantResults: IntArray) -> Unit)

}

class ResultRegistry : ResultLifecycle {

    private val mPermissions = hashMapOf<Int, (Array<out String>, IntArray) -> Unit>()
    private val mActivityResults = hashMapOf<Int, ActivityResultCallback>()

    fun clear() {
        mActivityResults.clear()
        mPermissions.clear()
    }

    override fun onPermissionsResult(requestCode: Int, callback: (permissions: Array<out String>, grantResults: IntArray) -> Unit) {
        mPermissions[requestCode] = callback
    }

    override fun onActivitySuccessResult(requestCode: Int, callback: (data: Intent?) -> Unit) {
        mActivityResults[requestCode] = OnActivityInstantSuccessResult(callback)
    }

    override fun onActivityResult(requestCode: Int, callback: (resultCode: Int, data: Intent?) -> Unit) {
        mActivityResults[requestCode] = OnActivityInstantResult(callback)
    }

    override fun onInstantResult(requestCode: Int, callback: (resultCode: Int, data: Intent?) -> Unit) {
        mActivityResults[requestCode] = OnActivityInstantResult(callback)
    }

    override fun onInstantSuccessResult(requestCode: Int, callback: (data: Intent?) -> Unit) {
        mActivityResults[requestCode] = OnActivityInstantSuccessResult(callback)
    }

    override fun onPeriodResult(requestCode: Int, callback: (resultCode: Int, data: Intent?) -> Unit) {
        mActivityResults[requestCode] = OnActivityResult(callback)
    }

    override fun onPeriodSuccessResult(requestCode: Int, callback: (data: Intent?) -> Unit) {
        mActivityResults[requestCode] = OnActivitySuccessResult(callback)
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        mActivityResults.filter { requestCode == it.key }.forEach {
            val callback = it.value
            if (callback is ActivityInstantResultCallback) {
                mActivityResults.remove(it.key)
            }

            val shouldCallback = (callback is SuccessResultCallback && resultCode == Activity.RESULT_OK)
                    || callback !is SuccessResultCallback

            if (shouldCallback) callback(resultCode, data)
        }
    }

    fun handlePermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        mPermissions.filter { it.key == requestCode }.forEach {
            it.value(permissions, grantResults)
            mPermissions.remove(it.key)
        }
    }

    private interface SuccessResultCallback

    private interface ActivityResultCallback {
        operator fun invoke(resultCode: Int, data: Intent?)
    }

    private interface ActivityInstantResultCallback : ActivityResultCallback

    private class OnActivityResult(val function: (resultCode: Int, data: Intent?) -> Unit) : ActivityResultCallback {
        override fun invoke(resultCode: Int, data: Intent?) {
            function(resultCode, data)
        }
    }

    private class OnActivitySuccessResult(val function: (data: Intent?) -> Unit) : ActivityResultCallback, SuccessResultCallback {
        override fun invoke(resultCode: Int, data: Intent?) {
            function(data)
        }
    }

    private class OnActivityInstantResult(val callback: (resultCode: Int, data: Intent?) -> Unit) : ActivityInstantResultCallback {
        override fun invoke(resultCode: Int, data: Intent?) {
            callback(resultCode, data)
        }
    }

    private class OnActivityInstantSuccessResult(val callback: (data: Intent?) -> Unit) : ActivityInstantResultCallback, SuccessResultCallback {
        override fun invoke(resultCode: Int, data: Intent?) {
            callback(data)
        }
    }

}
