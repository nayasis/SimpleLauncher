package com.github.nayasis.kotlin.basica

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class StringsKtTest {

    @Test
    fun `glob test`() {
        Assertions.assertTrue( "glob:*.http".glob().size > 1 )
        Assertions.assertTrue( "glob:./*.http".glob().size > 1 )
        Assertions.assertTrue( "glob:../../*.http".glob().size > 1 )
        Assertions.assertTrue( "*.http".glob()[0] == "*.http" )
    }

    @Test
    fun isDate() {
        Assertions.assertTrue( "2021-01-01".isDate() )
        Assertions.assertFalse( "2021-01-33".isDate() )
    }

}