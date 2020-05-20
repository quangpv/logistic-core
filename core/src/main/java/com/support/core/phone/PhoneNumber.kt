package com.support.core.phone

import com.support.core.helpers.PhoneUtils

open class PhoneNumber(val code: IPhoneCode, val body: String) {
    val isValid: Boolean get() = PhoneUtils.isValid(formattedValue)
    val isBlank: Boolean get() = body.isBlank()

    val formattedValue get() = "${code.dialCode} $body"

    val value get() = "${code.dialCode}${PhoneUtils.getPhone(body)}"
}

class EmptyPhoneNumber : PhoneNumber(PhoneCode("", "", ""), "")