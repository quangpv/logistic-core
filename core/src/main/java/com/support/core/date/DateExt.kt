package com.support.core.date

import java.text.SimpleDateFormat
import java.util.*


fun dateOf(dateStr: String): Date {
    return try {
        SimpleDateFormat(RESPONSE_TIME_FORMAT_1, Locale.getDefault()).parse(dateStr)!!
    } catch (e: Throwable) {
        SimpleDateFormat(RESPONSE_TIME_FORMAT_2, Locale.getDefault()).parse(dateStr)!!
    }
}

fun formatTime12(time: String): String {
    if (time.isBlank()) return time
    if (time.contains("AM", true)
        || time.contains("PM", true)
    ) return time
    val timeSegments = time.split(":")
    if (timeSegments.size < 2) return time

    val hourStr = timeSegments.first()
    val minuteStr = timeSegments[1]

    val hourInt = hourStr.toIntOrNull() ?: return time
    if (hourInt > 12) return "${hourInt - 12}:$minuteStr PM"
    return "$hourInt:$minuteStr AM"
}