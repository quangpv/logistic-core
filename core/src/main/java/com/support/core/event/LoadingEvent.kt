package com.support.core.event

import androidx.lifecycle.MutableLiveData

open class LoadingEvent : MutableLiveData<Boolean>() {
    private var mNumOfLoading = 0

    override fun postValue(value: Boolean?) {
        synchronized(this) {
            if (value!!) {
                mNumOfLoading++
                if (shouldPost(true)) super.postValue(true)
            } else {
                mNumOfLoading--
                if (mNumOfLoading < 0) mNumOfLoading = 0
                if (mNumOfLoading == 0) super.postValue(false)
            }
        }
    }

    protected open fun shouldPost(b: Boolean) = this.value != b
}