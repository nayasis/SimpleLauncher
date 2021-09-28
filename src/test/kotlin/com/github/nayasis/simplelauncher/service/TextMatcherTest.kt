package com.github.nayasis.simplelauncher.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class TextMatcherTest {

    @Test
    fun test() {

        val keyword = setOf("autohotkey", "dev", "spy", "au3")
        val matcher = TextMatcher()
        matcher.setKeyword("a d")

        assertTrue( matcher.isMatch(keyword) )

    }

}