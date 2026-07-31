package io.github.ptimulka.miecz.screens.riddles.quiz

data class QuizUiState(
    val verseText: String,
    val answers: List<String> = emptyList(),
    val selectedAnswer: String? = null,
    val phase: Phase = Phase.Answering
) {
    val checkEnabled: Boolean get() = selectedAnswer != null

    sealed interface Phase {
        data object Answering : Phase
        data object ShowingImageReward : Phase
        data class Result(val correct: Boolean) : Phase
    }
}
