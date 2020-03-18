package com.support.core.functional

interface Parser {
    fun <T> fromJson(string: String?, type: Class<T>): T?
    fun <T> toJson(value: T?): String
}