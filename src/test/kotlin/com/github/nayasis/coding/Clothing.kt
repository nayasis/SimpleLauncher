package com.github.nayasis.coding

import org.junit.jupiter.api.Test

class Clothing {

    @Test
    fun test() {
        for( i in 1..20 ) {
            println( "clothes:${i}, count:${count(i)}, enhanced:${countEnhanced(i)}")
            assert( count(i) == countEnhanced(i) )
        }
    }

    private fun count( clothes: Int ): Int {
        for( i in clothes/5 downTo 0 ) {
            var remain = clothes - 5*i
            when {
                remain     == 0 -> return i
                remain % 3 == 0 -> return i + remain / 3
            }
        }
        return -1
    }

    private fun countEnhanced( clothes: Int ): Int {

        if( clothes < 8 ) {
            return when( clothes ) {
                3 -> 1
                5 -> 1
                6 -> 2
                else -> -1
            }
        }

        var remain = clothes % 5;
        var cnt    = clothes / 5;

        return when (remain) {
            0 -> cnt
            1 -> cnt-1 + 2 // 5*1 + 1 -> 6
            2 -> cnt-2 + 4 // 5*2 + 2 -> 4
            3 -> cnt   + 1
            4 -> cnt-1 + 3 // 5*1 + 4 = 9
            else -> -1
        }

    }

}