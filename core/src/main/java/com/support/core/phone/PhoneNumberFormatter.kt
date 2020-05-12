package com.support.core.phone

import android.telephony.PhoneNumberFormattingTextWatcher
import android.text.Editable

class PhoneNumberFormatter : PhoneNumberFormattingTextWatcher() {
    private val plusReg = Regex("\\+")

    override fun afterTextChanged(s: Editable) {
        val string = s.toString()
        if (string.contains(plusReg)) {
            s.replace(0, s.length, string.replace(plusReg, ""))
        }
        super.afterTextChanged(s)
    }
}
