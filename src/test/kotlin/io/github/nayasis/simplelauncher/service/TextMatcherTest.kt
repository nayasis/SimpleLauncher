package io.github.nayasis.simplelauncher.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class TextMatcherTest {

    @Test
    fun test() {

        val keyword = listOf("autohotkey", "dev", "spy", "au3")
        val matcher = TextMatcher()
        matcher.setKeyword("a d")

        assertTrue( matcher.isMatch(keyword) )

    }

    @Test
    fun fuzzyMatchTitle() {
        val matcher = TextMatcher()
        matcher.setKeyword("ff")

        assertTrue(matcher.isMatch("final fantasy"))
    }

    @Test
    fun fuzzyMatchGroup() {
        val matcher = TextMatcher()
        matcher.setKeyword("dtl")

        assertTrue(matcher.isMatch("developer tool"))
    }

    @Test
    fun fuzzyMatchHashtagTokens() {
        val matcher = TextMatcher()
        matcher.setKeyword("ahk")

        assertTrue(matcher.isMatch(listOf("auto hotkey", "script")))
    }

}
