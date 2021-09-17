package com.github.nayasis.simplelauncher.service

class TextMatcher {

    private val parser = KeywordParser()
    private var keyword: Keyword = Keyword()

    val hasKeyword: Boolean
        get() = keyword.isNotEmpty()

    fun setKeyword(search: String?) {
        keyword = parser.parse(search?.trim())
    }

    fun isMatch(words: Set<String>?): Boolean = keyword.match(words)

    fun isMatch(word: String?): Boolean = keyword.match(word)

}