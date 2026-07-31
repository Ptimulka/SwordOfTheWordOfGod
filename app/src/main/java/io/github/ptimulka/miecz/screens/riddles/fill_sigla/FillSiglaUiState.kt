package io.github.ptimulka.miecz.screens.riddles.fill_sigla

enum class FillSiglaType {
    BOOK,
    CHAPTER,
    VERSE
}

data class FillSiglaUiState(
    val userInput: String = "",
    val phase: Phase = Phase.Answering
) {
    sealed interface Phase {
        data object Answering : Phase
        data object ShowingImageReward : Phase
        data class Result(val correct: Boolean) : Phase
    }
}
