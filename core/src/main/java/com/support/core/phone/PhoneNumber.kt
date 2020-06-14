package com.support.core.phone

import android.os.Parcelable
import com.support.core.helpers.PhoneUtils
import kotlinx.android.parcel.Parcelize

@Parcelize
open class PhoneNumber(val code: IPhoneCode, val body: String) : Parcelable {
    val isValid: Boolean get() = PhoneUtils.isValid(formattedValue)
    val isBlank: Boolean get() = body.isBlank()

    val formattedValue get() = "${code.dialCode} $body"

    val value get() = "${code.dialCode}${PhoneUtils.getPhone(body)}"
}

@Parcelize
class EmptyPhoneNumber : PhoneNumber(PhoneCode("", "", ""), "")