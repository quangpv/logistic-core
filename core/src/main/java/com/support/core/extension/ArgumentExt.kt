package com.support.core.extension

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.FragmentActivity
import java.io.Serializable


@Suppress("unchecked_cast")
operator fun <T> Bundle?.invoke(key: String): T? {
    return this?.get(key) as? T ?: error("Can not get $key")
}

@Suppress("unchecked_cast")
operator fun <T> Bundle?.invoke(key: String, def: T): T {
    return this?.get(key) as? T ?: def
}

fun Parcelable.toBundle(key: String): Bundle {
    return Bundle().also { it.putParcelable(key, this) }
}

fun Serializable.toBundle(key: String): Bundle {
    return Bundle().also { it.putSerializable(key, this) }
}


fun <T : Parcelable> FragmentActivity.args(key: String) =
    lazy(LazyThreadSafetyMode.NONE) {
        intent.getParcelableExtra(key) as? T ?: error("Can not cast $key")
    }

@Suppress("UNCHECKED_CAST")
fun <T : Serializable> FragmentActivity.sArgs(key: String) =
    lazy(LazyThreadSafetyMode.NONE) {
        intent.getSerializableExtra(key) as? T ?: error("Can not cast $key")
    }

@Suppress("UNCHECKED_CAST")
fun <T : Serializable> FragmentActivity.sNullableArgs(key: String): Lazy<T?> =
    lazy(LazyThreadSafetyMode.NONE) {
        intent.getSerializableExtra(key) as? T
    }