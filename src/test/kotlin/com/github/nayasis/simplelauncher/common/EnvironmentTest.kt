package com.github.nayasis.simplelauncher.common

import org.junit.jupiter.api.Test

class EnvironmentTest {
    @Test
    fun basic() {
        val env = Environment()
        println(env.all)
    }
}