package io.github.ptimulka.miecz.screens.riddles.word_scramble

import io.github.ptimulka.miecz.data.WordItem

data class WordScrambleUiState(
    val book: String,
    val chapter: Int,
    val number: String,
    val placedWords: List<WordItem> = emptyList(),
    val availableWords: List<WordItem> = emptyList(),
    val wrongWords: Set<WordItem> = emptySet(),
    val selectedWordForReorder: WordItem? = null,
    val phase: Phase = Phase.Answering
) {
    val checkEnabled: Boolean get() = availableWords.isEmpty()

    sealed interface Phase {
        data object Answering : Phase
        data object ShowingHintImage : Phase
        data class Result(val correct: Boolean) : Phase
    }
}
