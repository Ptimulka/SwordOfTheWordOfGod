package io.github.ptimulka.miecz.screens.riddles.quiz

import io.github.ptimulka.miecz.screens.riddles.base.BaseRiddleUiState
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase

data class QuizUiState(
    val verseText: String,
    val answers: List<String> = emptyList(),
    val selectedAnswer: String? = null,
    override val phase: RiddlePhase = RiddlePhase.Answering
) : BaseRiddleUiState {
    val checkEnabled: Boolean get() = selectedAnswer != null
}
