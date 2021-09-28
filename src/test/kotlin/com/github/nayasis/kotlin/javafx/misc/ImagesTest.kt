package com.github.nayasis.kotlin.javafx.misc

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class ImagesTest {

    @Test
    fun isAcceptable() {
        assertEquals(false, Images.isAcceptable(null) )
    }
}