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

/**
 * decides what Enter in the search does: search, or run the first result when the table already shows
 * the result of exactly these inputs.
 *
 * every applied search counts, not only the ones submitted with Enter: removing a chip searches too.
 */
class SearchSubmitTracker {

    private var lastApplied: SearchSubmitSnapshot? = null

    /** the table now shows the result of [snapshot]. */
    fun applied(snapshot: SearchSubmitSnapshot) {
        lastApplied = snapshot
    }

    fun nextAction(snapshot: SearchSubmitSnapshot): SearchSubmitAction {
        return if(lastApplied == snapshot) SearchSubmitAction.RUN_FIRST_RESULT else SearchSubmitAction.SUBMIT
    }

}
