package com.support.core.phone

import android.content.Context
import com.support.core.Inject
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.michaelrocks.libphonenumber.android.Phonenumber

@Inject(true)
class PhoneValidator(context: Context) {
    private val mPhoneUtil: PhoneNumberUtil = PhoneNumberUtil.createInstance(context)

    fun isValid(code: IPhoneCode, body: String): Boolean {
        val realNumber = getNumber(body)
        val len = realNumber.length
        if (len < 8 || len > 16) return false

        val phoneNumber = Phonenumber.PhoneNumber().apply {
            try {
                nationalNumber = realNumber.toLong()
            } catch (e: Throwable) {
                return false
            }
            countryCode = code.dialCode.removePrefix("+").toInt()
        }
        return mPhoneUtil.isValidNumber(phoneNumber)
    }

    companion object {
        fun getNumber(phoneNumber: String): String {
            return phoneNumber.replace(Regex("[^0-9+]"), "")
        }
    }
}