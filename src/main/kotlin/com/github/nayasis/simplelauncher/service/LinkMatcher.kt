package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.cache.implement.LruCache
import java.util.regex.Pattern

private val REGEXP_ASTERISK = Pattern.compile("(?<!\\\\)(\\\\[\\*\\?])")

class LinkMatcher {

    private val cache: LruCache<String,List<*>> = LruCache(20)

    private fun toSearchPattern(keyword: String): Pattern? {
        if (keyword.isEmpty()) return null
        val matcher = REGEXP_ASTERISK.matcher(Strings.escapeRegexp(keyword.toLowerCase()))
        val sb = StringBuffer()
        while (matcher.find()) {
            var group = matcher.group(1)
            group = if (group == "\\*") {
                ".*"
            } else {
                "."
            }
            matcher.appendReplacement(sb, group)
        }
        matcher.appendTail(sb)
        return Pattern.compile(sb.toString())
    }


    private enum class Operator {
        AND, OR
    }

}