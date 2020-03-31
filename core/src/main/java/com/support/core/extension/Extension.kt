package com.support.core.extension

fun <T> block(any: T?, function: T.() -> Unit) {
    if (any != null) function(any)
}

fun <T> tryCall(function: () -> T): Pair<T?, Throwable?> {
    return try {
        function() to null
    } catch (t: Throwable) {
        null to t
    }
}