@file:Suppress("UNUSED")

package com.support.core

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.support.core.base.BaseActivity
import com.support.core.extension.safe


open class PermissionChecker(private val activity: BaseActivity) {

    private var mOpenSettingDialog: AlertDialog? = null
    private var mRechecked = hashMapOf<String, Int>()

    protected open var titleDenied = "Permission denied"
    protected open var messageDenied = "You need to allow permission to use this feature"


    fun access(vararg permissions: String, onAccess: () -> Unit) {
        check(*permissions) { if (it) onAccess() }
    }

    fun check(vararg permissions: String, onPermission: (Boolean) -> Unit) {
        if (permissions.isEmpty()) throw RuntimeException("No permission to check")

        if (isAllAllowed(*permissions)) {
            onPermission(true)
            return
        }

        checkOrShowSetting(permissions, true, onPermission)
    }

    fun checkAny(vararg permissions: String, onPermission: (Boolean) -> Unit) {
        if (permissions.isEmpty()) throw RuntimeException("No permission to check")

        if (isAllAllowed(*permissions)) {
            onPermission(true)
            return
        }

        if (isAnyAllowed(*permissions)) {
            if (hasRecheck(permissions)) {
                onPermission(true)
                return
            }

            if (permissions.any {
                        !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
                    }) {
                onPermission(true)
                return
            }

        }

        checkOrShowSetting(permissions, false, onPermission)
    }

    private fun checkOrShowSetting(permissions: Array<out String>, checkAll: Boolean, onPermission: (Boolean) -> Unit) {
        if (shouldShowSettings(permissions)) {
            showSuggestOpenSetting(permissions, checkAll, onPermission)
        } else {
            request(permissions, checkAll, onPermission)
            val key = permissions[0]
            mRechecked[key] = mRechecked[key].safe() + 1
        }
    }

    private fun shouldShowSettings(permissions: Array<out String>): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions.first())
                || hasRecheck(permissions)
    }

    private fun clearRechecked(permissions: Array<out String>) {
        mRechecked.remove(permissions[0])
    }

    private fun hasRecheck(permissions: Array<out String>): Boolean {
        return mRechecked[permissions[0]].safe() > 1
    }

    private fun request(permissions: Array<out String>, checkAll: Boolean, onPermission: (Boolean) -> Unit) {
        val requestCode = requestCodeOf(permissions)
        activity.resultLife.onPermissionsResult(requestCode) { _, grantResults ->
            if (grantResults.isEmpty()) {
                onPermission(false)
                return@onPermissionsResult
            }

            val isAllowed = if (checkAll) grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            else grantResults.any { it == PackageManager.PERMISSION_GRANTED }

            onPermission(isAllowed)
            if (isAllowed) clearRechecked(permissions)
        }
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    private fun requestCodeOf(permissions: Array<out String>): Int {
        return permissions.hashCode() and 0xffff
    }

    private fun isAnyAllowed(vararg permissions: String): Boolean {
        return permissions.any {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isAllAllowed(vararg permissions: String): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun showSuggestOpenSetting(
            permissions: Array<out String>,
            checkAll: Boolean,
            onPermission: (Boolean) -> Unit
    ) {
        if (mOpenSettingDialog == null) {
            mOpenSettingDialog = AlertDialog.Builder(activity)
                    .setTitle(titleDenied)
                    .setMessage(messageDenied)
                    .setPositiveButton("Ok") { _: DialogInterface, _: Int ->
                        openSetting(permissions, checkAll, onPermission)
                    }
                    .create()
        }
        mOpenSettingDialog!!.show()
    }

    private fun openSetting(permissions: Array<out String>, checkAll: Boolean, onPermission: (Boolean) -> Unit) {
        val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + activity.packageName)
        )
        val requestCode = requestCodeOf(permissions)
        activity.resultLife.onActivityResult(requestCode) { _, _ ->
            val isAllowed = if (checkAll) isAllAllowed(*permissions) else isAnyAllowed(*permissions)
            onPermission(isAllowed)
            if (isAllowed) clearRechecked(permissions)
        }
        activity.startActivityForResult(intent, requestCode)
    }
}
