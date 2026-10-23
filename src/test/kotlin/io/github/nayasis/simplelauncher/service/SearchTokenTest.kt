package io.github.nayasis.simplelauncher.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class SearchTokenTest {

    @Test
    fun termTokenMatchesWithFuzzySearch() {
        val tokens = listOf(SearchToken(SearchTokenKind.TERM, "ff"))

        matchesSearchTokens(listOf("final fantasy"), tokens) shouldBe true
    }

    @Test
    fun notExcludesBySubstringOnly() {
        val tokens = listOf(
            SearchToken(SearchTokenKind.NOT, "not"),
            SearchToken(SearchTokenKind.TERM, "exe"),
        )

        // "Excel Viewer" holds e-x-e as a subsequence, which must not exclude it
        matchesSearchTokens(listOf("Excel Viewer"), tokens) shouldBe true
        matchesSearchTokens(listOf("setup.exe"), tokens) shouldBe false
    }

    @Test
    fun notGroupExcludesBySubstringOnly() {
        val tokens = listOf(
            SearchToken(SearchTokenKind.TERM, "ev"),
            SearchToken(SearchTokenKind.NOT, "not"),
            SearchToken(SearchTokenKind.PAREN, "("),
            SearchToken(SearchTokenKind.TERM, "exe"),
            SearchToken(SearchTokenKind.OR, "or"),
            SearchToken(SearchTokenKind.TERM, "zip"),
            SearchToken(SearchTokenKind.PAREN, ")"),
        )

        // "ev" still matches fuzzily outside of the NOT
        matchesSearchTokens(listOf("Excel Viewer"), tokens) shouldBe true
        matchesSearchTokens(listOf("Excel Viewer.zip"), tokens) shouldBe false
    }

    @Test
    fun malformedExpressionStillExcludesWhatNotNames() {
        // "a OR" ends in an operator, so it falls back to the terms
        val tokens = listOf(
            SearchToken(SearchTokenKind.TERM, "alpha"),
            SearchToken(SearchTokenKind.NOT, "not"),
            SearchToken(SearchTokenKind.TERM, "beta"),
            SearchToken(SearchTokenKind.OR, "or"),
        )

        matchesSearchTokens(listOf("alpha"), tokens) shouldBe true
        matchesSearchTokens(listOf("alpha beta"), tokens) shouldBe false
        matchesSearchTokens(listOf("beta"), tokens) shouldBe false
    }

    @Test
    fun andOrCannotGoWhereAnOperandIsMissing() {
        val and = SearchToken(SearchTokenKind.AND, "and")
        val term = SearchToken(SearchTokenKind.TERM, "a")
        val open = SearchToken(SearchTokenKind.PAREN, "(")
        val close = SearchToken(SearchTokenKind.PAREN, ")")

        canInsertSearchToken(emptyList(), 0, and) shouldBe false
        canInsertSearchToken(listOf(open, term), 1, and) shouldBe false
        canInsertSearchToken(listOf(open, term, close), 2, and) shouldBe false
        canInsertSearchToken(listOf(term, close), 1, SearchToken(SearchTokenKind.NOT, "not")) shouldBe false
        canInsertSearchToken(listOf(term), 1, and) shouldBe true
    }

    @Test
    fun adjacentTermsUseImplicitAnd() {
        val tokens = listOf(
            SearchToken(SearchTokenKind.TERM, "final"),
            SearchToken(SearchTokenKind.TERM, "fantasy"),
        )

        matchesSearchTokens(listOf("final fantasy"), tokens) shouldBe true
        matchesSearchTokens(listOf("final round"), tokens) shouldBe false
    }

    @Test
    fun andHasHigherPrecedenceThanOr() {
        val tokens = listOf(
            SearchToken(SearchTokenKind.TERM, "alpha"),
            SearchToken(SearchTokenKind.OR, "or"),
            SearchToken(SearchTokenKind.TERM, "beta"),
            SearchToken(SearchTokenKind.AND, "and"),
            SearchToken(SearchTokenKind.TERM, "gamma"),
        )

        matchesSearchTokens(listOf("beta gamma"), tokens) shouldBe true
        matchesSearchTokens(listOf("beta"), tokens) shouldBe false
    }

    @Test
    fun parenthesisTokensMustMatchPair() {
        val tokens = listOf(
            SearchToken(SearchTokenKind.PAREN, "["),
            SearchToken(SearchTokenKind.TERM, "alpha"),
            SearchToken(SearchTokenKind.OR, "or"),
            SearchToken(SearchTokenKind.TERM, "beta"),
            SearchToken(SearchTokenKind.PAREN, "]"),
            SearchToken(SearchTokenKind.TERM, "gamma"),
        )

        matchesSearchTokens(listOf("beta gamma"), tokens) shouldBe true
        matchesSearchTokens(listOf("beta"), tokens) shouldBe false
    }

    @Test
    fun invalidExpressionFallsBackToAnyTerm() {
        val tokens = listOf(
            SearchToken(SearchTokenKind.OR, "or"),
            SearchToken(SearchTokenKind.TERM, "alpha"),
        )

        matchesSearchTokens(listOf("alpha"), tokens) shouldBe true
    }

    private val not   = SearchToken(SearchTokenKind.NOT, "not")
    private val and   = SearchToken(SearchTokenKind.AND, "and")
    private val or    = SearchToken(SearchTokenKind.OR, "or")
    private fun term(value: String) = SearchToken(SearchTokenKind.TERM, value)

    @Test
    fun notNegatesTheNextTerm() {
        val tokens = listOf(not, term("alpha"))

        matchesSearchTokens(listOf("beta"), tokens) shouldBe true
        matchesSearchTokens(listOf("alpha"), tokens) shouldBe false
    }

    @Test
    fun notAfterATermIsAnImplicitAnd() {
        // alpha NOT beta == alpha AND (NOT beta)
        val tokens = listOf(term("alpha"), not, term("beta"))

        matchesSearchTokens(listOf("alpha"), tokens) shouldBe true
        matchesSearchTokens(listOf("alpha", "beta"), tokens) shouldBe false
        matchesSearchTokens(listOf("gamma"), tokens) shouldBe false
    }

    @Test
    fun notBindsTighterThanAndOr() {
        // NOT alpha OR beta == (NOT alpha) OR beta
        val tokens = listOf(not, term("alpha"), or, term("beta"))

        matchesSearchTokens(listOf("alpha", "beta"), tokens) shouldBe true
        matchesSearchTokens(listOf("alpha"), tokens) shouldBe false
        matchesSearchTokens(listOf("gamma"), tokens) shouldBe true
    }

    @Test
    fun notNegatesAGroup() {
        // NOT (alpha OR beta)
        val tokens = listOf(not, SearchToken(SearchTokenKind.PAREN, "("), term("alpha"), or, term("beta"), SearchToken(SearchTokenKind.PAREN, ")"))

        matchesSearchTokens(listOf("gamma"), tokens) shouldBe true
        matchesSearchTokens(listOf("beta"), tokens) shouldBe false
    }

    @Test
    fun precedenceIsParenthesisNotAndOr() {
        val open  = SearchToken(SearchTokenKind.PAREN, "(")
        val close = SearchToken(SearchTokenKind.PAREN, ")")
        fun matches(words: List<String>, vararg tokens: SearchToken) = matchesSearchTokens(words, tokens.toList())

        // NOT alpha AND beta OR gamma == ((NOT alpha) AND beta) OR gamma
        val flat = arrayOf(not, term("alpha"), and, term("beta"), or, term("gamma"))
        matches(listOf("beta"), *flat) shouldBe true
        matches(listOf("alpha", "beta"), *flat) shouldBe false
        matches(listOf("alpha", "gamma"), *flat) shouldBe true
        matches(listOf("delta"), *flat) shouldBe false

        // parentheses first: NOT (alpha OR beta) AND gamma
        val grouped = arrayOf(not, open, term("alpha"), or, term("beta"), close, and, term("gamma"))
        matches(listOf("gamma"), *grouped) shouldBe true
        matches(listOf("beta", "gamma"), *grouped) shouldBe false
        matches(listOf("delta"), *grouped) shouldBe false

        // alpha OR beta AND gamma == alpha OR (beta AND gamma)
        val mixed = arrayOf(term("alpha"), or, term("beta"), and, term("gamma"))
        matches(listOf("alpha"), *mixed) shouldBe true
        matches(listOf("beta"), *mixed) shouldBe false
    }

    @Test
    fun termWithOrJoinsOnlyWhereItMakesSense() {
        val open  = SearchToken(SearchTokenKind.PAREN, "(")
        val close = SearchToken(SearchTokenKind.PAREN, ")")
        fun insert(vararg before: SearchToken): Pair<List<SearchToken>, Int> {
            val tokens = before.toMutableList()
            return tokens to insertTermWithOr(tokens, tokens.size, term("x"))
        }

        insert() shouldBe (listOf(term("x")) to 1)                                        // start
        insert(open) shouldBe (listOf(open, term("x")) to 2)                               // after (
        insert(term("a"), or) shouldBe (listOf(term("a"), or, term("x")) to 3)             // OR already there
        insert(not) shouldBe (listOf(not, term("x")) to 2)                                 // NOT needs its term
        insert(term("a"), and) shouldBe (listOf(term("a"), or, term("x")) to 3)            // AND becomes OR
        insert(term("a")) shouldBe (listOf(term("a"), or, term("x")) to 3)                 // after a term
        insert(open, term("a"), close) shouldBe (listOf(open, term("a"), close, or, term("x")) to 5) // after )

        // in the middle: only what precedes the cursor counts
        val tokens = mutableListOf(term("a"), term("b"))
        insertTermWithOr(tokens, 1, term("x")) shouldBe 3
        tokens shouldBe listOf(term("a"), or, term("x"), term("b"))
    }

    @Test
    fun doubleNotCancelsOut() {
        matchesSearchTokens(listOf("alpha"), listOf(not, not, term("alpha"))) shouldBe true
        matchesSearchTokens(listOf("beta"), listOf(not, not, term("alpha"))) shouldBe false
    }

    @Test
    fun danglingNotFallsBackToAnyTerm() {
        matchesSearchTokens(listOf("alpha"), listOf(term("alpha"), not)) shouldBe true
        matchesSearchTokens(listOf("alpha"), listOf(not, and, term("alpha"))) shouldBe true
    }

    @Test
    fun operatorAliasesAreNormalized() {
        createOperatorSearchToken("") shouldBe SearchToken(SearchTokenKind.AND, "and")
        createOperatorSearchToken("&&") shouldBe SearchToken(SearchTokenKind.AND, "and")
        createOperatorSearchToken("|") shouldBe SearchToken(SearchTokenKind.OR, "or")
        createOperatorSearchToken("(") shouldBe SearchToken(SearchTokenKind.PAREN, "(")
        createOperatorSearchToken("-") shouldBe not
        createOperatorSearchToken("!") shouldBe not
        createOperatorSearchToken("not") shouldBe not
        createCommandOrTermSearchToken("NOT") shouldBe not
        searchTokenLabel(not) shouldBe "NOT"
    }

    @Test
    fun operatorsCannotTouchAndOr() {
        val tokens = listOf(term("alpha"), not, term("beta"), and, term("gamma"))

        canInsertSearchToken(tokens, 2, and) shouldBe false      // NOT AND
        canInsertSearchToken(tokens, 3, not) shouldBe false      // NOT before AND
        canInsertSearchToken(tokens, 1, not) shouldBe true       // NOT NOT
        canInsertSearchToken(tokens, 1, or) shouldBe true        // alpha OR NOT beta
        canInsertSearchToken(tokens, 4, or) shouldBe false       // AND OR
    }

    @Test
    fun termTokenRejectsOperatorText() {
        createTermSearchToken("AND") shouldBe null
        createTermSearchToken("NOT") shouldBe null
        createTermSearchToken("final") shouldBe SearchToken(SearchTokenKind.TERM, "final")
    }

}
