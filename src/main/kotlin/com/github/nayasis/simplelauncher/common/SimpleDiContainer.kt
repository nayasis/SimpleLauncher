package com.github.nayasis.simplelauncher.common

import com.github.nayasis.kotlin.basica.core.validator.cast
import tornadofx.DIContainer
import kotlin.reflect.KClass

class SimpleDiContainer: DIContainer {

    private val instances = HashMap<KClass<*>,LinkedHashMap<String,Any>>()

    override fun <T: Any> getInstance(type: KClass<T>): T {
        return instances[type]?.values?.firstOrNull()?.cast(type)
            ?: throw AssertionError("No bean ($type) found in container")
    }

    override fun <T: Any> getInstance(type: KClass<T>, name: String): T {
        return instances[type]?.get(name)?.cast(type)
            ?: throw AssertionError("No bean ($type) found in container")
    }

    fun <T: Any> get(type: KClass<T>): T {
        return getInstance(type)
    }

    fun <T: Any> get(type: KClass<T>, name: String): T {
        return getInstance(type, name)
    }

    fun set(bean: Any) {
        val klass = bean::class
        val name = klass.simpleName ?: "anonymous"
        if(!instances.containsKey(klass))
            instances[klass] = LinkedHashMap()
        instances[klass]!![name] = bean
    }

    fun remove(bean: Any) {
        val klass = bean::class
        val name = klass.simpleName ?: "anonymous"
        instances[klass]?.remove(name)
    }

}