package com.github.nayasis.kotlin.basica

import java.util.*
import kotlin.reflect.KProperty

class FieldProperty<R, T: Any?>(
    val initialValue: (R) -> T
) {
    private val map = WeakHashMap<R,T>()

    operator fun getValue(thisRef: R, property: KProperty<*>): T =
        map[thisRef] ?: setValue(thisRef, property, initialValue(thisRef))

    operator fun setValue(thisRef: R, property: KProperty<*>, value: T): T {
        map[thisRef] = value
        return value
    }

}

infix fun <T> Boolean.then( param: T? ): T? = if(this) param else null