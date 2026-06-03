package io.github.nayasis.simplelauncher.service

import java.util.Stack

enum class SearchTokenKind {
    TERM,
    AND,
    OR,
    PAREN,
}

data class SearchToken(
    val kind: SearchTokenKind,
    val value: String,
)

private enum class PreviousSearchToken {
    START,
    VALUE,
    OPEN,
    CLOSE,
    OPERATOR,
}

fun matchesSearchTokens(haystack: Iterable<String?>?, tokens: List<SearchToken>): Boolean {
    val terms = tokens.filter { it.kind == SearchTokenKind.TERM }
    if (terms.isEmpty()) return true

    fun matchTerm(value: String): Boolean =
        haystack?.firstOrNull { matchesSearchTerm(it, value) } != null

    fun fallbackMatch(): Boolean = terms.any { matchTerm(it.value) }

    val values = Stack<Boolean>()
    val operators = Stack<SearchToken>()

    fun applyTopOperator(): Boolean {
        val operator = operators.popOrNull()
        val right = values.popOrNull()
        val left = values.popOrNull()
        if (operator == null || operator.isOpenParenthesis() || left == null || right == null) return false
        values.push(if (operator.kind == SearchTokenKind.AND) left && right else left || right)
        return true
    }

    var hasValue = false
    var previous = PreviousSearchToken.START
    for (token in tokens) {
        when (token.kind) {
            SearchTokenKind.AND,
            SearchTokenKind.OR -> {
                if (previous == PreviousSearchToken.START || previous == PreviousSearchToken.OPEN || previous == PreviousSearchToken.OPERATOR) {
                    return fallbackMatch()
                }
                pushSearchOperator(operators, token, ::applyTopOperator)
                previous = PreviousSearchToken.OPERATOR
            }
            SearchTokenKind.PAREN -> {
                if (token.isOpenParenthesis()) {
                    if (previous == PreviousSearchToken.VALUE || previous == PreviousSearchToken.CLOSE) {
                        pushSearchOperator(operators, SearchToken(SearchTokenKind.AND, "and"), ::applyTopOperator)
                    }
                    operators.push(token)
                    previous = PreviousSearchToken.OPEN
                } else {
                    if (previous == PreviousSearchToken.START || previous == PreviousSearchToken.OPEN || previous == PreviousSearchToken.OPERATOR) {
                        return fallbackMatch()
                    }
                    while (operators.isNotEmpty() && !operators.peek().isOpenParenthesis()) {
                        if (!applyTopOperator()) return fallbackMatch()
                    }
                    if (operators.popOrNull()?.value != token.matchingOpenParenthesis()) return fallbackMatch()
                    previous = PreviousSearchToken.CLOSE
                }
            }
            SearchTokenKind.TERM -> {
                if (previous == PreviousSearchToken.VALUE || previous == PreviousSearchToken.CLOSE) {
                    pushSearchOperator(operators, SearchToken(SearchTokenKind.AND, "and"), ::applyTopOperator)
                }
                values.push(matchTerm(token.value))
                hasValue = true
                previous = PreviousSearchToken.VALUE
            }
        }
    }

    if (!hasValue || previous == PreviousSearchToken.OPEN || previous == PreviousSearchToken.OPERATOR) {
        return fallbackMatch()
    }
    while (operators.isNotEmpty()) {
        if (operators.peek().isOpenParenthesis()) return fallbackMatch()
        if (!applyTopOperator()) return fallbackMatch()
    }
    return if (values.size == 1) values.pop() else fallbackMatch()
}

fun createTermSearchToken(value: String): SearchToken? {
    val clean = value.trim()
    if (clean.isEmpty() || createOperatorSearchToken(clean) != null) return null
    return SearchToken(SearchTokenKind.TERM, clean)
}

fun createOperatorSearchToken(value: String): SearchToken? {
    val clean = value.trim()
    return when {
        clean.isAndOperatorText() -> SearchToken(SearchTokenKind.AND, "and")
        clean.isOrOperatorText() -> SearchToken(SearchTokenKind.OR, "or")
        clean.isSearchParenthesisText() -> SearchToken(SearchTokenKind.PAREN, clean)
        else -> null
    }
}

fun hasAdjacentSearchOperatorToken(tokens: List<SearchToken>, index: Int): Boolean {
    return tokens.getOrNull(index - 1).isSearchOperatorToken() || tokens.getOrNull(index).isSearchOperatorToken()
}

fun searchTokenLabel(token: SearchToken): String {
    return when(token.kind) {
        SearchTokenKind.AND -> "AND"
        SearchTokenKind.OR -> "OR"
        else -> token.value
    }
}

private fun pushSearchOperator(
    operators: Stack<SearchToken>,
    operator: SearchToken,
    applyTopOperator: () -> Boolean,
) {
    while (operators.isNotEmpty()) {
        val top = operators.peek()
        if (top.isOpenParenthesis() || top.precedence() < operator.precedence()) break
        if (!applyTopOperator()) return
    }
    operators.push(operator)
}

private fun SearchToken?.isSearchOperatorToken(): Boolean =
    this?.kind == SearchTokenKind.AND || this?.kind == SearchTokenKind.OR

private fun SearchToken.isOpenParenthesis(): Boolean =
    kind == SearchTokenKind.PAREN && value in listOf("(", "[", "{")

private fun SearchToken.matchingOpenParenthesis(): String? =
    when(value) {
        ")" -> "("
        "]" -> "["
        "}" -> "{"
        else -> null
    }

private fun SearchToken.precedence(): Int =
    when(kind) {
        SearchTokenKind.AND -> 2
        SearchTokenKind.OR -> 1
        else -> -1
    }

private fun String.isAndOperatorText(): Boolean =
    this == "" || this == "&" || this == "&&" || uppercase() == "AND"

private fun String.isOrOperatorText(): Boolean =
    this == "," || this == "|"

private fun String.isSearchParenthesisText(): Boolean =
    this in listOf("(", ")", "[", "]", "{", "}")

private fun <T> Stack<T>.popOrNull(): T? =
    if (isEmpty()) null else pop()
