package io.github.nayasis.simplelauncher.service

import java.util.Stack

enum class SearchTokenKind {
    TERM,
    AND,
    OR,
    /** prefix operator: negates the term or group right after it. */
    NOT,
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
        if (operator == null || operator.isOpenParenthesis()) return false
        val right = values.popOrNull() ?: return false
        if (operator.kind == SearchTokenKind.NOT) {
            values.push(!right)
            return true
        }
        val left = values.popOrNull() ?: return false
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
            SearchTokenKind.NOT -> {
                // "a NOT b" reads as "a AND NOT b"
                if (previous == PreviousSearchToken.VALUE || previous == PreviousSearchToken.CLOSE) {
                    pushSearchOperator(operators, SearchToken(SearchTokenKind.AND, "and"), ::applyTopOperator)
                }
                // a prefix operator has no left operand to reduce first
                operators.push(token)
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

fun createLiteralTermSearchToken(value: String): SearchToken? {
    val clean = value.trim()
    if (clean.isEmpty()) return null
    return SearchToken(SearchTokenKind.TERM, clean)
}

fun createCommandOrTermSearchToken(value: String): SearchToken? {
    val clean = value.trim()
    if (clean.isTextOperatorWord()) return createOperatorSearchToken(clean)
    return createLiteralTermSearchToken(clean)
}

fun createOperatorSearchToken(value: String): SearchToken? {
    val clean = value.trim()
    return when {
        clean.isAndOperatorText() -> SearchToken(SearchTokenKind.AND, "and")
        clean.isOrOperatorText() -> SearchToken(SearchTokenKind.OR, "or")
        clean.isNotOperatorText() -> SearchToken(SearchTokenKind.NOT, "not")
        clean.isSearchParenthesisText() -> SearchToken(SearchTokenKind.PAREN, clean)
        else -> null
    }
}

/**
 * insert [term] at [index] joined to what precedes it with OR, and return the index right after it.
 *
 * - nothing to join at the start, after "(", after OR, or after NOT (which needs the term itself)
 * - a preceding AND becomes the OR
 * - after a term or ")" an OR goes in between
 */
fun insertTermWithOr(tokens: MutableList<SearchToken>, index: Int, term: SearchToken): Int {
    val previous = tokens.getOrNull(index - 1)
    var at = index
    when {
        previous == null || previous.isOpenParenthesis() -> {}
        previous.kind == SearchTokenKind.OR || previous.kind == SearchTokenKind.NOT -> {}
        previous.kind == SearchTokenKind.AND -> tokens[index - 1] = SearchToken(SearchTokenKind.OR, "or")
        else -> tokens.add(at++, SearchToken(SearchTokenKind.OR, "or"))
    }
    tokens.add(at++, term)
    return at
}

/** whether [token] can go in at [index] without an operator ending up next to an AND / OR: "NOT AND" is as broken as "AND AND". */
fun canInsertSearchToken(tokens: List<SearchToken>, index: Int, token: SearchToken): Boolean {
    val previous = tokens.getOrNull(index - 1)
    val next     = tokens.getOrNull(index)
    return when (token.kind) {
        SearchTokenKind.AND,
        SearchTokenKind.OR  -> !previous.isSearchOperatorToken() && previous?.kind != SearchTokenKind.NOT && !next.isSearchOperatorToken()
        SearchTokenKind.NOT -> !next.isSearchOperatorToken()
        else -> true
    }
}

fun searchTokenLabel(token: SearchToken): String {
    return when(token.kind) {
        SearchTokenKind.AND -> "AND"
        SearchTokenKind.OR -> "OR"
        SearchTokenKind.NOT -> "NOT"
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
        SearchTokenKind.NOT -> 3
        SearchTokenKind.AND -> 2
        SearchTokenKind.OR -> 1
        else -> -1
    }

private fun String.isAndOperatorText(): Boolean =
    this == "" || this == "&" || this == "&&" || uppercase() == "AND"

private fun String.isOrOperatorText(): Boolean =
    this == "," || this == "|" || uppercase() == "OR"

private fun String.isNotOperatorText(): Boolean =
    this == "-" || this == "!" || uppercase() == "NOT"

private fun String.isSearchParenthesisText(): Boolean =
    this in listOf("(", ")", "[", "]", "{", "}")

private fun String.isTextOperatorWord(): Boolean =
    uppercase() in listOf("AND", "OR", "NOT")

private fun <T> Stack<T>.popOrNull(): T? =
    if (isEmpty()) null else pop()
