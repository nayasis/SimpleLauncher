package com.github.nayasis.kotlin.basica

class Classes {
}

fun Class<*>.extends( klass: Class<*> ): Boolean {
    return this.isAssignableFrom(klass) || klass.isAssignableFrom(this)
}