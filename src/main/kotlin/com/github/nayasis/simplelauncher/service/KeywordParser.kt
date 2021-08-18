package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.cache.implement.LruCache
import com.github.nayasis.kotlin.basica.core.string.find
import com.github.nayasis.kotlin.basica.core.string.tokenize
import com.github.nayasis.simplelauncher.service.Operator.*
import java.util.*
import java.util.regex.Pattern

class KeywordParser(capacity: Int = 20) {

    private val cache = LruCache<String,Keyword>(capacity)

    fun parse(word: String?): Keyword? {
        if(word.isNullOrEmpty()) return null
        val key = word.trim()
        return cache.getOrPut(key) {toPostfix(key)}
    }

    internal fun toPostfix(text: String): Keyword {

        val queue = Keyword()
        val stack = Stack<Any?>()

        for (token in tokenize(text)) {
            if (token is String) {
                queue.add(toPattern(token)!!)
            } else if (token === BRACE_OPEN) {
                stack.push(token)
            } else if (token === BRACE_CLOSE) {
                while (stack.peek() !== BRACE_OPEN) {
                    queue.add(stack.pop()!!)
                }
                stack.pop()
            } else {

                /*  우선순위가 높은 연산자를 stack에서 꺼내 queue에 넣는다.
                     -> 우선순위가 높은 연산자를 먼저 계산해주기 위함

                   ex. 1 + 2 / 3 + 5

                    queue          stack
                    1
                    1              +
                    1,2            +
                    1,2            +,/
                    1,2,3
                    1,2,3,/        +,+
                    1,2,3,/,+,+

                 */
                while (!stack.isEmpty()) {
                    val top = stack.peek()
                    // 괄호 주의
                    if (top !== BRACE_OPEN && priority(top) > priority(token)) {
                        queue.add(stack.pop()!!)
                    } else {
                        break
                    }
                }

                stack.push(token)

            }
        }

        while (!stack.isEmpty()) queue.add(stack.pop()!!)
        return queue

    }

    private fun priority(token: Any?): Int {
        return if (token !is Operator) -1 else token.priority
    }

    private fun tokenize(text: String?): List<*> {

        val buffer = LinkedList<Any>()
        var braces = 0

        for (token in text.tokenize(" ,-()", true)) {
            val prev = buffer.peek()
            when (token) {
                " " -> if (prev is String || prev === BRACE_CLOSE) {
                    buffer.push(AND)
                }
                "," -> if (prev is String || prev === BRACE_CLOSE) {
                    buffer.push(OR)
                } else if (prev === AND) {
                    buffer.pop()
                    buffer.push(OR)
                }
                "-" -> {
                    if (prev === NOT) continue
                    if (prev is String || prev === BRACE_CLOSE) {
                        buffer.push(AND)
                    }
                    buffer.push(NOT)
                }
                "(" -> {
                    if (prev is String || prev === BRACE_CLOSE) {
                        buffer.push(AND)
                    }
                    buffer.push(BRACE_OPEN)
                    braces++
                }
                ")" -> {
                    if (prev === BRACE_OPEN) {
                        buffer.pop()
                    } else {
                        buffer.push(BRACE_CLOSE)
                    }
                    braces--
                }
                else -> buffer.push(token.trim())
            }
        }

        // 괄호 쌍이 안맞으면 작업 중지
        if (braces != 0)
            return ArrayList<Any>()

        // 표현식 마지막이 연산자일 경우 삭제
        val last = buffer.peek()

        if( last in ARITH_OPERATOR)
            buffer.pop()
        val result = ArrayList<Any>()
        while (!buffer.isEmpty()) {
            val curr = buffer.pop()
            val prev = buffer.peek()
            if (curr is Operator && curr !== NOT && curr !== BRACE_OPEN) {
                if( prev in ARITH_OPERATOR) {
                    buffer.pop()
                }
            }
            result.add(curr)
        }
        return result.reversed()
    }

    private fun toPattern(keyword: String?): Pattern? {
        if (keyword.isNullOrEmpty()) return null
        return try {
            Pattern.compile(keyword.toLowerCase())
        } catch (e: Exception) {

            Pattern.compile(
                keyword.toLowerCase().replace(PATTERN_REGEX, "")
            )
        }
    }

}

private enum class Operator(val priority: Int) {

    AND(1), OR(1), NOT(5),

    // operator가 아니라서, -1로 제외시킴
    BRACE_OPEN(-1), BRACE_CLOSE(-1);

}

private val PATTERN_REGEX  = "[\\[\\]\\(\\)\\{\\}\\.\\*\\+\\?\\$\\^\\|\\#\\\\]".toRegex()
private val ARITH_OPERATOR = listOf(AND, OR, NOT)

class Keyword: ArrayList<Any>() {

    fun match(fn: (pattern: Pattern) -> Boolean): Boolean {
        val stack = Stack<Boolean>()
        for (token in this) {
            when {
                token is Pattern -> stack.push(fn(token))
                token === NOT -> stack.push(!stack.pop()!!)
                token === AND -> stack.push(stack.pop()!! && stack.pop()!!)
                token === OR  -> stack.push(stack.pop()!! || stack.pop()!!)
            }
        }
        return stack.pop()!!
    }

    fun match(text: String?): Boolean {
        return match { pattern -> text.find(pattern) }
    }

}

