package com.support.core.helpers

import android.content.Context
import android.telephony.TelephonyManager


open class CountryDetector(private val context: Context, private val def: String = "us") {
    val countryCode
        get() = simCountry ?: networkCountry ?: localeCountry ?: def

    private val simCountry: String?
        get() {
            try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                return telephonyManager.simCountryIso
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

    private val networkCountry: String?
        get() {
            try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                return telephonyManager.networkCountryIso
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

    private val localeCountry: String?
        get() {
            try {
                return context.resources.configuration.locale.country
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
}