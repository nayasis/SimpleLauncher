package io.github.nayasis.simplelauncher.view

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SearchSubmitTrackerTest {

    @Test
    fun enterOnTheShownResultRunsFirstResult() {
        val tracker = SearchSubmitTracker()
        val search = SearchSubmitSnapshot(keyword = "final fantasy", group = "game")

        tracker.nextAction(search) shouldBe SearchSubmitAction.SUBMIT
        tracker.applied(search)
        tracker.nextAction(search) shouldBe SearchSubmitAction.RUN_FIRST_RESULT
    }

    @Test
    fun changedSearchSubmitsAgainBeforeRunningFirstResult() {
        val tracker = SearchSubmitTracker()
        tracker.applied(SearchSubmitSnapshot(keyword = "final", group = ""))

        tracker.nextAction(SearchSubmitSnapshot(keyword = "final fantasy", group = "")) shouldBe SearchSubmitAction.SUBMIT
    }

    @Test
    fun searchAppliedWithoutEnterCounts() {
        val tracker = SearchSubmitTracker()
        val foo = SearchSubmitSnapshot(keyword = "foo", group = "")

        tracker.applied(foo)
        // the chip was removed: the table shows every link again
        tracker.applied(SearchSubmitSnapshot(keyword = "", group = ""))

        tracker.nextAction(foo) shouldBe SearchSubmitAction.SUBMIT
    }

}
