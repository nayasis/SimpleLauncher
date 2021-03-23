package com.github.nayasis.coding

import org.junit.jupiter.api.Test

class AntSeries {

    @Test
    fun test() {
        for( i in 1..10 )
            println( calc(i) )
    }

    fun calc(n: Int): List<Int> {
        var series = emptyList<Int>()
        for( i in 1..n ) {
            series = if( i == 1 ) {
                listOf(1)
            } else {
                calc(series)
            }
        }
        return series
    }

    private fun calc(series: List<Int>): List<Int> {

        var slot: Slot? = null
        var next = ArrayList<Slot>()

        for( c in series ) {
            when {
                slot == null -> slot = Slot(c)
                slot.ch != c -> {
                    next.add(slot)
                    slot = Slot(c)
                }
                else -> slot.count++
            }
        }

        if( slot != null )
            next.add(slot)

        return next.flatMap { listOf(it.ch, it.count) }

    }

}

data class Slot( val ch: Int = 0, var count: Int = 1 )