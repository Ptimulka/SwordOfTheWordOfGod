package io.github.ptimulka.miecz.screens.riddles.fill_whole_verse

import io.github.ptimulka.miecz.helpers.DiffPart

data class FillWholeVerseUiState(
    val book: String,
    val chapter: Int,
    val number: String,
    val userInput: String = "",
    val similarityScore: Float = 0f,
    val diffs: List<DiffPart> = emptyList(),
    val phase: Phase = Phase.Answering
) {
    sealed interface Phase {
        data object Answering : Phase
        data object ShowingHintImage : Phase
        data class Result(val correct: Boolean) : Phase
    }
}
