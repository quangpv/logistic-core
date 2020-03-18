package com.support.core

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

interface Dispatcher

fun Dispatcher.asResult(): ResultLifecycle {
    return (this as ResultOwner).resultLife
}

inline fun <reified T : FragmentActivity> Dispatcher.open(
    args: Bundle? = null,
    edit: Intent.() -> Unit = {}
): Dispatcher {
    when (this) {
        is AppCompatActivity -> startActivity(Intent(this, T::class.java).apply {
            if (args != null) putExtras(args)
            edit()
        })
        is Fragment -> startActivity(Intent(requireContext(), T::class.java).apply {
            if (args != null) putExtras(args)
            edit()
        })
    }
    return this
}

inline fun <reified T : FragmentActivity> Dispatcher.openForResult(args: Bundle? = null): Dispatcher {
    when (this) {
        is AppCompatActivity -> startActivityForResult(Intent(this, T::class.java)
                .putArgs(args), REQUEST_FOR_RESULT_INSTANTLY)
        is Fragment -> startActivityForResult(Intent(requireContext(), T::class.java)
                .putArgs(args), REQUEST_FOR_RESULT_INSTANTLY)
    }
    return this
}

fun Intent.putArgs(args: Bundle?): Intent {
    args ?: return this
    putExtras(args)
    return this
}

fun Dispatcher.close() {
    when (this) {
        is AppCompatActivity -> finish()
        is Fragment -> requireActivity().finish()
    }
}

fun Dispatcher.close(code: Int, data: Bundle? = null) {
    fun FragmentActivity.doClose() {
        if (data != null) setResult(code, Intent().apply { putExtras(data) })
        else setResult(code)
        finish()
    }
    when (this) {
        is AppCompatActivity -> doClose()
        is Fragment -> requireActivity().doClose()
    }
}

fun Dispatcher.clear() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        when (this) {
            is AppCompatActivity -> finishAffinity()
            is Fragment -> requireActivity().finishAffinity()
        }
    } else {
        when (this) {
            is AppCompatActivity -> finish()
            is Fragment -> requireActivity().finish()
        }
    }
}