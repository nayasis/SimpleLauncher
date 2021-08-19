package com.github.nayasis.simplelauncher.service

import com.github.nayasis.simplelauncher.jpa.entity.Link
import org.springframework.stereotype.Service

@Service
class LinkMatcher {

    private val parser = KeywordParser()
    private var keyword: Keyword = Keyword()

    val hasKeyword: Boolean
        get() = keyword.isNotEmpty()

    fun setKeyword(search: String?) {
        keyword = parser.parse(search)
    }

    fun isMatch(link: Link): Boolean {
        if(keyword == null) return false
        for(word in link.keyword ?: emptySet())
            if(keyword!!.match(word)) return true
        return false
    }

}