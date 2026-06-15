package io.github.nayasis.simplelauncher.view

data class SearchSubmitSnapshot(
    val keyword: String,
    val group: String,
) {
    fun isBlank(): Boolean = keyword.isBlank() && group.isBlank()
}

enum class SearchSubmitAction {
    SUBMIT,
    RUN_FIRST_RESULT,
}

class SearchSubmitTracker {

    private var lastSubmitted: SearchSubmitSnapshot? = null

    fun nextAction(snapshot: SearchSubmitSnapshot): SearchSubmitAction {
        if(lastSubmitted == snapshot) {
            return SearchSubmitAction.RUN_FIRST_RESULT
        }
        lastSubmitted = snapshot
        return SearchSubmitAction.SUBMIT
    }

}
