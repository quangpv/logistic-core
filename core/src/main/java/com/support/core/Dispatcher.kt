package com.support.core

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

interface Dispatcher

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

fun Dispatcher.close() {
    when (this) {
        is AppCompatActivity -> finish()
        is Fragment -> requireActivity().finish()
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