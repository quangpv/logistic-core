package com.support.core.functional

import android.app.Dialog
import android.widget.PopupMenu
import android.widget.PopupWindow

class LocalStore {
    private val mCache = hashMapOf<String, Any>()

    @Suppress("unchecked_cast")
    fun <T : Any> get(key: String, function: () -> T): T {
        if (mCache.containsKey(key)) return mCache[key] as T
        val data = function()
        mCache[key] = data
        return data
    }

    inline fun <reified T : Any> get(noinline function: () -> T): T {
        return get(T::class.java.name, function)
    }

    fun clear() {
        mCache.values.forEach {
            when (it) {
                is Disposable -> it.dispose()
                is Dialog -> it.dismiss()
                is PopupWindow -> it.dismiss()
                is PopupMenu -> it.dismiss()
            }
        }
        mCache.clear()
    }

    operator fun set(key: String, value: Any) {
        mCache[key] = value
    }
}

interface Disposable {
    fun dispose()
}

interface LocalStoreOwner {
    val localStore: LocalStore
}