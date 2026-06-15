package io.github.nayasis.simplelauncher.view

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SearchSubmitTrackerTest {

    @Test
    fun repeatedEnterWithSameSearchRunsFirstResult() {
        val tracker = SearchSubmitTracker()
        val search = SearchSubmitSnapshot(keyword = "final fantasy", group = "game")

        tracker.nextAction(search) shouldBe SearchSubmitAction.SUBMIT
        tracker.nextAction(search) shouldBe SearchSubmitAction.RUN_FIRST_RESULT
    }

    @Test
    fun changedSearchSubmitsAgainBeforeRunningFirstResult() {
        val tracker = SearchSubmitTracker()

        tracker.nextAction(SearchSubmitSnapshot(keyword = "final", group = "")) shouldBe SearchSubmitAction.SUBMIT
        tracker.nextAction(SearchSubmitSnapshot(keyword = "final fantasy", group = "")) shouldBe SearchSubmitAction.SUBMIT
        tracker.nextAction(SearchSubmitSnapshot(keyword = "final fantasy", group = "")) shouldBe SearchSubmitAction.RUN_FIRST_RESULT
    }

}
