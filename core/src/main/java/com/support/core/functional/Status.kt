package com.support.core.functional


interface Status {
    val value: String
    val sequence: Int get() = -1
    val groupBy: Array<String> get() = arrayOf(value)

    operator fun compareTo(status: Status): Int {
        return sequence - status.sequence
    }

    operator fun contains(status: String): Boolean {
        return status in groupBy
    }
}
