package com.github.nayasis.kotlin.basica

fun <T> Iterator<T>.toList(): List<T> {
    return ArrayList<T>().apply {
        while ( hasNext() )
            this += next()
    }
}