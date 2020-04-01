package com.support.core.helpers

import android.text.TextUtils
import android.util.Patterns


object PhoneUtils {
    fun isValid(number: String): Boolean {
        val realNumber = number.replace(Regex("[^0-9]"), "")
        return !TextUtils.isEmpty(realNumber) && Patterns.PHONE.matcher(realNumber).matches()
    }
}