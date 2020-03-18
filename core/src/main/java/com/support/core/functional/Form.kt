package com.support.core.functional


interface Form {

    fun validate() {}

    fun buildRequestBody(): Map<String, Any> {
        error("Not implement build request body yet!")
    }

    fun buildBody(): Map<String, String> {
        error("Not implement build body yet!")
    }
}