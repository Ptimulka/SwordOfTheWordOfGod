package io.github.ptimulka.miecz.screens.riddles.quiz

sealed interface QuizEvent {
    data class Select(val answer: String) : QuizEvent
    data object Check : QuizEvent
    data object DismissResult : QuizEvent
    data object DismissHint : QuizEvent
}
