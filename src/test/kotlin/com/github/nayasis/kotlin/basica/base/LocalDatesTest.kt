package com.github.nayasis.kotlin.basica.base

import com.github.nayasis.kotlin.basica.atEndOfMonth
import com.github.nayasis.kotlin.basica.atStartOfMonth
import com.github.nayasis.kotlin.basica.toLocalDateTime
import com.github.nayasis.kotlin.basica.toString
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private val log = KotlinLogging.logger {}

internal class LocalDatesTest {

    @Test
    fun atStartOfMonth() {

        val current = "2020-12-22".toLocalDateTime()

        assertEquals( "2020-12-01", current.atStartOfMonth().toString("YYYY-MM-DD") )
        assertEquals( "2020-12-31", current.atEndOfMonth().toString("YYYY-MM-DD") )

    }

}