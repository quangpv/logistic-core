package com.support.core.phone

import android.content.Context
import android.text.Editable

open class PhoneNumberFormatter(
    context: Context?,
    countryNameCode: String?,
    countryPhoneCode: Int,
    internationalOnly: Boolean
) : InternationalPhoneTextWatcher(context, countryNameCode, countryPhoneCode, internationalOnly) {
    private val plusReg = Regex("\\+")

    override fun afterTextChanged(s: Editable) {
        val string = s.toString()
        if (string.contains(plusReg)) {
            s.replace(0, s.length, string.replace(plusReg, ""))
        } else if (string.startsWith("0")) s.replace(0, s.length, string.removePrefix("0"))
        super.afterTextChanged(s)
    }
}
