package com.github.nayasis.coding

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class GCD {

    /**
     * 최대 공약수
     * 
     * @param a Int
     * @param b Int
     * @return 최대공약수
     */
    fun gcd( a: Int, b: Int ): Int {
        return if( b == 0 ) {
            abs( a )
        } else {
            gcd( b, a % b )
        }
    }

}

