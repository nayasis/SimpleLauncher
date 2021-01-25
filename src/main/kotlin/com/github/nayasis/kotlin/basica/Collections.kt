package com.github.nayasis.kotlin.basica

fun <T> Iterator<T>.toList(): List<T> {
    return ArrayList<T>().apply {
        while ( hasNext() )
            this += next()
    }
}

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum = 0L
    for (element in this) {
        sum += selector(element)
    }
    return sum
}