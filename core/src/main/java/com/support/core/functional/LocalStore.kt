package com.support.core.functional

class LocalStore {
    private val mCache = hashMapOf<String, Any>()

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
        mCache.clear()
    }

    operator fun set(key: String, value: Any) {
        mCache[key] = value
    }
}

interface LocalStoreOwner {
    val localStore: LocalStore
}