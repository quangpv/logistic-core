package com.support.core.base

import android.content.Context
import android.content.SharedPreferences
import com.support.core.functional.Parser
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class Caching(context: Context, private val parser: Parser) {
    private val mShared = context.getSharedPreferences("logistics:cache", Context.MODE_PRIVATE)

    fun <T> reference(key: String) = object : CacheProperty<T?> {
        @Suppress("unchecked_cast")
        private val KProperty<*>.type: Class<T>
            get() = (returnType.classifier as KClass<*>).javaObjectType as Class<T>

        private var mValue: T? = null

        override fun getValue(thisRef: Caching, property: KProperty<*>): T? {
            if (mValue == null) mValue =
                parser.fromJson(thisRef.mShared.getString(key, ""), property.type)
            return mValue
        }

        override fun setValue(thisRef: Caching, property: KProperty<*>, value: T?) {
            mValue = value
            thisRef.mShared.edit().putString(key, parser.toJson(value)).apply()
        }
    }

    protected fun string(key: String, def: String): CacheProperty<String> =
        Primitive(get = { getString(key, def) ?: def }, set = { putString(key, it) })

    protected fun long(key: String, def: Long): CacheProperty<Long> =
        Primitive(get = { getLong(key, def) }, set = { putLong(key, it) })

    protected fun int(key: String, def: Int): CacheProperty<Int> =
        Primitive(get = { getInt(key, def) }, set = { putInt(key, it) })

    protected fun float(key: String, def: Float): CacheProperty<Float> =
        Primitive(get = { getFloat(key, def) }, set = { putFloat(key, it) })

    protected fun boolean(key: String, def: Boolean): CacheProperty<Boolean> =
        Primitive(get = { getBoolean(key, def) }, set = { putBoolean(key, it) })

    protected fun double(key: String, def: Float) = float(key, def)

    interface CacheProperty<T> : ReadWriteProperty<Caching, T>

    private inner class Primitive<T>(
        private val get: SharedPreferences.() -> T,
        private val set: SharedPreferences.Editor.(T) -> Unit
    ) : CacheProperty<T> {
        private var mValue: T? = null

        override fun getValue(thisRef: Caching, property: KProperty<*>): T {
            if (mValue == null) mValue = get(mShared)
            return mValue!!
        }

        override fun setValue(thisRef: Caching, property: KProperty<*>, value: T) {
            mValue = value
            mShared.edit().apply { set(value) }.apply()
        }
    }

}