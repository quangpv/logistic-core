package com.support.core.extension

fun <T> block(any: T?, function: T.() -> Unit) {
    if (any != null) function(any)
}