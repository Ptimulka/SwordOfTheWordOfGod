package io.github.ptimulka.miecz.screens.riddles.fill_whole_sigla

data class FillWholeSiglaUiState(
    val bookInput: String = "",
    val chapterInput: String = "",
    val verseInput: String = "",
    val wrongIndices: Set<Int> = emptySet(),
    val phase: Phase = Phase.Answering
) {
    val allFieldsFilled: Boolean get() = bookInput.isNotEmpty() && chapterInput.isNotEmpty() && verseInput.isNotEmpty()

    sealed interface Phase {
        data object Answering : Phase
        data object ShowingImageReward : Phase
        data class Result(val correct: Boolean) : Phase
    }
}
