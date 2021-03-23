package com.github.nayasis.kotlin.basica

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class ExtentionsKtTest {

    @Test
    fun then() {

        println( (1 == 1) then "A" ?: "B" )
        println( (1 == 2) then "A" ?: "B" )

        Assertions.assertEquals( "A", (1 == 1) then "A" ?: "B" )
        Assertions.assertEquals( "B", (1 == 2) then "A" ?: "B" )

    }
}