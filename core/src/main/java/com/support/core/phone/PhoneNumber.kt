package com.support.core.phone

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
open class PhoneNumber(val code: IPhoneCode, val body: String) : Parcelable {

    val isBlank: Boolean get() = body.isBlank()

    val formattedValue get() = "${code.dialCode} $body"

    val value get() = "${code.dialCode}${PhoneValidator.getNumber(body)}"
}

@Parcelize
class EmptyPhoneNumber : PhoneNumber(PhoneCode("", "", ""), "")