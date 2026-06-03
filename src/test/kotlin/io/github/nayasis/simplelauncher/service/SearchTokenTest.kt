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

    @Test
    fun operatorAliasesAreNormalized() {
        createOperatorSearchToken("") shouldBe SearchToken(SearchTokenKind.AND, "and")
        createOperatorSearchToken("&&") shouldBe SearchToken(SearchTokenKind.AND, "and")
        createOperatorSearchToken("|") shouldBe SearchToken(SearchTokenKind.OR, "or")
        createOperatorSearchToken("(") shouldBe SearchToken(SearchTokenKind.PAREN, "(")
    }

    @Test
    fun termTokenRejectsOperatorText() {
        createTermSearchToken("AND") shouldBe null
        createTermSearchToken("final") shouldBe SearchToken(SearchTokenKind.TERM, "final")
    }

}
