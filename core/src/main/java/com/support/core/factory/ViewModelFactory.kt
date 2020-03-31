package com.support.core.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.support.core.base.BaseViewModel
import com.support.core.dependenceContext

class ViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!BaseViewModel::class.java.isAssignableFrom(modelClass))
            error("${modelClass.simpleName} should be extended from BaseViewModel")
        val viewModel= dependenceContext.get(modelClass)
        (viewModel as BaseViewModel).onCreate()
        return viewModel
    }
}