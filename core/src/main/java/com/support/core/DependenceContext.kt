package com.support.core

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Inject(
    val singleton: Boolean = false
)

interface Bean<T> {
    val value: T
}

interface Scope {
    fun contains(clazz: Class<*>): Boolean
    fun dispose()

    fun <T> factory(clazz: Class<T>, function: () -> T)
    fun <T> lookup(clazz: Class<T>): Bean<T>
}

private open class SimpleBean<T>(
    val isSingleton: Boolean,
    val function: () -> T
) : Bean<T> {
    private var mValue: T? = null

    override val value: T
        get() {
            return if (isSingleton) {
                if (mValue == null) synchronized(this) {
                    if (mValue == null) mValue = function()
                }
                mValue!!
            } else function()
        }
}

private class ScopeBean<T>(private val function: () -> T) : Bean<T> {
    private var mValue: T? = null

    override val value: T
        get() {
            if (mValue == null) mValue = function()
            return mValue!!
        }

    fun dispose() {
        mValue = null
    }
}

private class SimpleScope(private val context: DependenceContext) :
    Scope {
    private val mBean = hashMapOf<Class<*>, ScopeBean<*>>()

    override fun contains(clazz: Class<*>): Boolean {
        return mBean.containsKey(clazz)
    }

    override fun <T> factory(clazz: Class<T>, function: () -> T) {
        mBean[clazz] = ScopeBean(function)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> lookup(clazz: Class<T>): Bean<T> {
        if (!mBean.containsKey(clazz)) factory(clazz) {
            context.create(clazz)
        }
        return mBean[clazz] as Bean<T>
    }

    override fun dispose() {
        mBean.values.forEach { it.dispose() }
        mBean.clear()
    }
}

private class ApplicationBean(application: Application) :
    Bean<Application> {
    override val value: Application = application
}

abstract class ProvideContext {

    abstract fun getBean(clazz: Class<*>): Bean<*>?

    abstract fun modules(vararg module: Module)

    abstract fun <T> single(clazz: Class<T>, function: () -> T)

    abstract fun <T> factory(clazz: Class<T>, function: () -> T)

    abstract fun <T> scope(scopeId: String, clazz: Class<T>, function: () -> T)

    inline fun <reified T> single(noinline function: () -> T) {
        return single(T::class.java, function)
    }

    inline fun <reified T> factory(noinline function: () -> T) {
        return factory(T::class.java, function)
    }

    inline fun <reified T> scope(scopeId: String, noinline function: () -> T) {
        return scope(scopeId, T::class.java, function)
    }

    abstract fun <T> get(clazz: Class<T>): T

    inline fun <reified T> get(): T {
        return get(T::class.java)
    }
}

class DependenceContext : ProvideContext() {

    private val mScopeBean = hashMapOf<String, Scope>()
    private val mBean = hashMapOf<Class<*>, Bean<*>>()
    private lateinit var mApplication: ApplicationBean

    internal fun set(application: Application) {
        mApplication = ApplicationBean(application)
    }

    override fun getBean(clazz: Class<*>): Bean<*>? {
        if (mBean.containsKey(clazz)) return mBean[clazz]
        error("Not found ${clazz.javaClass.name}")
    }

    override fun <T> single(clazz: Class<T>, function: () -> T) {
        if (mBean.containsKey(clazz)) error("Class  ${clazz.simpleName} defined")
        mBean[clazz] = SimpleBean(true, function)
    }

    override fun <T> factory(clazz: Class<T>, function: () -> T) {
        if (mBean.containsKey(clazz)) error("Class  ${clazz.simpleName} defined")
        mBean[clazz] = SimpleBean(false, function)
    }

    override fun <T> scope(scopeId: String, clazz: Class<T>, function: () -> T) {
        val scope = getScope(scopeId)
        if (scope.contains(clazz)) error("Class ${clazz.simpleName} exist in scope $scopeId")
        scope.factory(clazz, function)
    }

    fun getScope(scopeId: String): Scope {
        return if (!mScopeBean.containsKey(scopeId))
            SimpleScope(this).also { mScopeBean[scopeId] = it }
        else mScopeBean[scopeId]!!
    }

    override fun modules(vararg module: Module) {
        module.forEach { it.provide() }
    }

    @Suppress("unchecked_cast")
    fun <T> lookup(clazz: Class<T>): Bean<T> {
        if (!mBean.containsKey(clazz)) {
            if (clazz.isAssignableFrom(Application::class.java)
                || clazz.isAssignableFrom(Context::class.java)
            ) return mApplication as Bean<T>
            reflectProvideIfNeeded(clazz)
        }
        return getBean(clazz) as Bean<T>
    }

    private fun <T> reflectProvideIfNeeded(clazz: Class<T>) {
        when {
            ViewModel::class.java.isAssignableFrom(clazz) -> factory(clazz) {
                create(clazz)
            }

            else -> {
                val annotation = clazz.getAnnotation(Inject::class.java)
                    ?: error("Not found provider for ${clazz.simpleName}")
                if (annotation.singleton) single(clazz) {
                    create(clazz)
                } else factory(clazz) {
                    create(clazz)
                }
            }
        }
    }

    fun <T> lookupByScope(scopeId: String, clazz: Class<T>): T {
        return getScope(scopeId).lookup(clazz).value
    }

    override fun <T> get(clazz: Class<T>): T {
        val bean = (lookup(clazz) as? Bean<T>) ?: error("Not found bean ${clazz.simpleName}")
        return bean.value
    }

    @Suppress("unchecked_cast")
    fun <T> create(clazz: Class<T>): T {
        val constructor = clazz.constructors.firstOrNull()
            ?: clazz.declaredConstructors.firstOrNull()
            ?: error("Not found constructor for ${clazz.simpleName}")

        val paramTypes = constructor.genericParameterTypes
        return constructor.newInstance(*paramTypes.map { lookup(it as Class<*>).value }.toTypedArray()) as T
    }

}

class Module(
    private val context: DependenceContext,
    private val provide: (DependenceContext) -> Unit
) : ProvideContext() {
    private var mModules: Array<out Module>? = null

    override fun getBean(clazz: Class<*>): Bean<*>? {
        return context.getBean(clazz)
    }

    override fun modules(vararg module: Module) {
        mModules = module
    }

    override fun <T> single(clazz: Class<T>, function: () -> T) {
        context.single(clazz, function)
    }

    override fun <T> factory(clazz: Class<T>, function: () -> T) {
        context.factory(clazz, function)
    }

    override fun <T> scope(scopeId: String, clazz: Class<T>, function: () -> T) {
        context.scope(scopeId, clazz, function)
    }

    override fun <T> get(clazz: Class<T>): T {
        return context.get(clazz)
    }

    fun provide() {
        mModules?.forEach { it.provide() }
        provide(context)
    }
}

fun module(function: ProvideContext.() -> Unit): Module {
    return Module(dependenceContext, function)
}

inline fun <reified T> inject() = lazy(LazyThreadSafetyMode.NONE) {
    dependenceContext.get(T::class.java)
}

val dependenceContext = DependenceContext()

fun Application.dependencies(function: DependenceContext.() -> Unit) {
    dependenceContext.set(this)
    function(dependenceContext)
}