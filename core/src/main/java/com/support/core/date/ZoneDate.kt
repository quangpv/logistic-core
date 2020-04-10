package com.support.core.date

import java.text.SimpleDateFormat
import java.util.*

const val RESPONSE_TIME_FORMAT_1 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
const val RESPONSE_TIME_FORMAT_2 = "yyyy-MM-dd"

class ZoneDate(private val timestampUTC: Long, zone: String) :
    Date(timestampOf(timestampUTC, zone)) {

    val device get() = Date(timestampOf(timestampUTC))
    val utc get() = Date(timestampUTC)

    val deviceString get() = device.simpleFormat()

    private fun Date.simpleFormat(): String {
        return SimpleDateFormat("yyyy/MM/dd hh:mm:ss a", Locale.getDefault()).format(this)
    }

    override fun toString(): String {
        return simpleFormat()
    }

    companion object {

        private fun timestampOf(timestampUTC: Long, zone: String): Long {
            return timestampOf(timestampUTC, TimeZone.getTimeZone(zone))
        }

        private fun timestampOf(
            timestampUTC: Long,
            zone: TimeZone = TimeZone.getDefault()
        ): Long {
            return timestampUTC + zone.rawOffset
        }

        private fun parse(dateStr: String): Date {
            return try {
                utcFormat(RESPONSE_TIME_FORMAT_1).parse(dateStr)!!
            } catch (e: Throwable) {
                utcFormat(RESPONSE_TIME_FORMAT_2).parse(dateStr)!!
            }
        }

        private fun utcFormat(format: String) = SimpleDateFormat(format, Locale.US)

        fun fromUTC(date: Date, zoneName: String = TimeZone.getDefault().id): ZoneDate {
            return ZoneDate(date.time, zoneName)
        }

        fun fromDevice(date: Date, zoneName: String = TimeZone.getDefault().id): ZoneDate {
            val zoneOffset = TimeZone.getDefault().rawOffset
            return ZoneDate(date.time - zoneOffset, zoneName)
        }

        fun fromUTC(dateStr: String, zoneName: String = TimeZone.getDefault().id): ZoneDate {
            return ZoneDate(parse(dateStr).time, zoneName)
        }
    }
}