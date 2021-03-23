package com.github.nayasis.kotlin.basica

import com.github.nayasis.basica.model.NList
import java.io.InputStream

fun isEmpty(value: Any?): Boolean {
    return when (value) {
        null -> true
        is CharSequence -> value.isEmpty()
        is Collection<*> -> value.isEmpty()
        is Array<*> -> value.isEmpty()
        is NList -> value.size() == 0
        is InputStream -> value.available() == 0
        else -> false
    }
}

fun isNotEmpty(value: Any?): Boolean = ! isEmpty(value)

fun <T> nvl(value: T?, other: T ): T {
    if( isNotEmpty(value) ) return value!!
    return other
}

fun <T> nvl(value: T?, other: T?, another: T ): T {
    if (isNotEmpty(value)) return value!!
    if (isNotEmpty(other)) return other!!
    return another
}

fun nvl(value: String?): String {
    return value ?: ""
}