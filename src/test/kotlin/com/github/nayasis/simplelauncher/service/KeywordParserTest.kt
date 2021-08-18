package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.core.string.find
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class KeywordParserTest {

    private val matcher = KeywordParser()

    @Test
    fun match() {
        val list = listOf("merong", "nayasis", "emuloader", "gundam", "final fantasy")
        assertEquals("[emuloader]", filter(list, "m r -n"))
        assertEquals("[merong, emuloader]", filter(list, "(f sy), (m r -n)"))
    }

    private fun filter(list: List<String>, searchWord: String): List<String> {
        val keyword = matcher.toKeyword(searchWord)
            .also { if(it==null) return emptyList() }!!
        return list.filter { title ->
            keyword.match { pattern -> title.find(pattern) } }
    }

    @Test
    fun toPostfix() {
        assertEquals("[a, b, AND]", matcher.toPostfix("a b"))
        assertEquals("[a, b, AND]", matcher.toPostfix("a   b"))
        assertEquals("[a, b, OR]", matcher.toPostfix("a, b"))
        assertEquals("[a, b, OR]", matcher.toPostfix("a  , b"))
        assertEquals("[a, b, OR]", matcher.toPostfix("a,,, , b"))
        assertEquals("[a, NOT, b, NOT, AND]", matcher.toPostfix("-a -b"))
        assertEquals("[a, NOT, b, NOT, OR]", matcher.toPostfix("-a, -b"))
        assertEquals("[a, NOT, b, NOT, OR]", matcher.toPostfix("-a , -b "))
        assertEquals("[a, NOT, b, NOT, OR]", matcher.toPostfix("-a, -b, "))
        assertEquals("[a, NOT, b, NOT, AND]", matcher.toPostfix("(-a) -(b) "))
        assertEquals("[a, NOT, b, OR, c, d, AND, AND]", matcher.toPostfix("(-a, b) ( c d )"))
        assertEquals("[a, b, AND, c, d, OR, AND]", matcher.toPostfix("(a b) (c, d) "))
        assertEquals(
            "[a, NOT, b, OR, e, f, g, h, OR, OR, AND, AND, c, d, AND, AND]",
            matcher.toPostfix("((-a, b,) e (f, g, h ), ) ( c d )")
        )
        assertEquals("[except, NOT]", matcher.toPostfix("() ( ) -except"))
    }

    private fun assertEquals(string: String, list: List<*>) {
        assertEquals(string, list.toString())
    }

}