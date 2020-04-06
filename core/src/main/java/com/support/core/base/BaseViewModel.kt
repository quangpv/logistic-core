package com.support.core.base

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.support.core.AppExecutors
import com.support.core.ConcurrentContext
import com.support.core.ConcurrentScope
import com.support.core.event.LoadingEvent
import com.support.core.event.PostAble
import com.support.core.event.SingleLiveEvent
import com.support.core.extension.LoadCacheLiveData
import com.support.core.extension.asMutable
import com.support.core.extension.doAsync
import com.support.core.extension.post
import com.support.core.factory.ViewModelFactory
import com.support.core.functional.Form
import com.support.core.isOnMainThread
import java.util.concurrent.Executor

abstract class BaseViewModel : ViewModel() {

    val refresh = MutableLiveData<Any>()
    val error = SingleLiveEvent<Throwable>()
    val loading = LoadingEvent()
    val viewLoading = LoadingEvent()
    private val mConcurrent = ConcurrentContext()

    open fun onCreate() {}

    override fun onCleared() {
        super.onCleared()
        mConcurrent.cancel()
    }

    fun <T, V> LiveData<T>.async(
            loadingEvent: LoadingEvent? = loading,
            errorEvent: SingleLiveEvent<Throwable>? = error,
            function: ConcurrentScope.(T) -> V?
    ): LiveData<V> {
        val next = MediatorLiveData<V>()
        next.addSource(this) {
            next.doAsync(it, mConcurrent, loadingEvent, errorEvent, function)
        }
        return next
    }

    fun <T, V> LoadCacheLiveData<T, V>.orAsync(
            loadingEvent: LoadingEvent? = loading,
            errorEvent: SingleLiveEvent<Throwable>? = error,
            function: ConcurrentScope.(T) -> V?
    ): LiveData<V> {
        val next = MediatorLiveData<V>()
        next.addSource(this) {
            if (it.second != null) {
                next.value = it.second
                return@addSource
            }
            next.doAsync(it.first, mConcurrent, loadingEvent, errorEvent, function)
        }
        return next
    }

    fun <T, V> LoadCacheLiveData<T, V>.thenAsync(
            loadingEvent: LoadingEvent? = loading,
            errorEvent: SingleLiveEvent<Throwable>? = error,
            function: ConcurrentScope.(T) -> V?
    ): LiveData<V> {
        val next = MediatorLiveData<V>()
        next.addSource(this) {
            if (it.second != null) next.value = it.second
            next.doAsync(it.first, mConcurrent, loadingEvent, errorEvent, function)
        }
        return next
    }

    fun async(
            loadingEvent: LoadingEvent? = loading,
            errorEvent: PostAble<Throwable>? = error,
            function: ConcurrentScope.() -> Unit
    ) {
        loadingEvent?.post(true)

        mConcurrent.launch {
            try {
                function()
            } catch (t: Throwable) {
                errorEvent?.postValue(t)
                t.printStackTrace()
            } finally {
                loadingEvent?.postValue(false)
            }
        }
    }

    private fun io(force: Boolean = true, executor: Executor, function: () -> Unit) {
        val callable = {
            try {
                function()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
        if (force) executor.execute(callable)
        else {
            if (isOnMainThread) executor.execute(callable)
            else callable()
        }
    }

    fun diskIO(force: Boolean = true, function: () -> Unit) = io(force, AppExecutors.diskIO, function)

    fun networkIO(force: Boolean = true, function: () -> Unit) = io(force, AppExecutors.networkIO, function)

    fun <T> LiveData<T>.validate(function: (T) -> Unit): LiveData<T> {
        val next = MediatorLiveData<T>()
        next.addSource(this) {
            try {
                function(it)
                next.value = it
            } catch (t: Throwable) {
                error.value = t
            }
        }
        return next
    }

    fun validate(form: Form): Any? = validate(form::validate)

    fun validate(function: () -> Unit): Any? {
        return try {
            function()
            null
        } catch (t: Throwable) {
            error.value = t
            this
        }
    }
}

inline fun <reified T : ViewModel> AppCompatActivity.viewModel(): Lazy<T> =
        lazy(LazyThreadSafetyMode.NONE) {
            ViewModelProvider(this, ViewModelFactory()).get(T::class.java)
        }


inline fun <reified T : ViewModel> Fragment.viewModel(): Lazy<T> =
        lazy(LazyThreadSafetyMode.NONE) {
            ViewModelProvider(this, ViewModelFactory()).get(T::class.java)
        }

inline fun <reified T : ViewModel> Fragment.shareViewModel(): Lazy<T> =
        lazy(LazyThreadSafetyMode.NONE) {
            ViewModelProvider(requireActivity(), ViewModelFactory()).get(T::class.java)
        }
