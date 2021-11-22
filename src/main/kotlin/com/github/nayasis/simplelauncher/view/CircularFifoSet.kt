package com.github.nayasis.simplelauncher.view

import com.github.nayasis.kotlin.basica.cache.implement.FifoCache
import com.github.nayasis.kotlin.basica.core.extention.isEmpty

class CircularFifoSet<T>(val capacity: Int = 128): Collection<T> {

    private val map = FifoCache<T,Boolean>(capacity)
    private var currentIndex : Int? = null

    constructor(collection: Collection<T>): this(collection.size) {
        addAll(collection)
    }

    override val size: Int
        get() = map.size()

    override fun contains(element: T): Boolean {
        return map.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        for( e in elements ) {
            if( contains(e) ) return true
        }
        return false
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    fun add(element: T) {
        map.put(element,true)
    }

    fun addAll(elements: Collection<T>) {
        elements.forEach { map.put(it,true) }
    }

    fun clear() {
        map.evict()
        currentIndex = null
    }

    override fun iterator(): Iterator<T> = map.keySet().iterator()

    fun remove(element: T): Boolean {
        if( ! contains(element) ) return false
        map.evict(element)
        if( isEmpty() )
            currentIndex = null
        return true
    }

    fun removeAll(elements: Collection<T>) {
        elements.forEach { remove(it) }
    }

    fun get(index: Int): T? {
        if( isEmpty() ) return null
        var idx = index % size
        if( idx < 0 ) {
            idx += size
        }
        return map.keySet().elementAt(idx)
    }

    fun next(): T? {
        if( isEmpty() ) return null
        currentIndex = if( currentIndex == null ) 0 else currentIndex!! + 1
        return get(currentIndex!!)
    }

    fun prev(): T? {
        if( isEmpty() ) return null
        currentIndex = if( currentIndex == null ) 0 else currentIndex!! - 1
        return get(currentIndex!!)
    }

}