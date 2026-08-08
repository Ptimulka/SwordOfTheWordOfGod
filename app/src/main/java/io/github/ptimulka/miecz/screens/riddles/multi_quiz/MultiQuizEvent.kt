package io.github.ptimulka.miecz.screens.riddles.multi_quiz

sealed interface MultiQuizEvent {
    data class SelectBook(val book: String) : MultiQuizEvent
    data class SelectChapter(val chapter: String) : MultiQuizEvent
    data class SelectVerse(val verse: String) : MultiQuizEvent
    data object Check : MultiQuizEvent
    data object DismissResult : MultiQuizEvent
    data object DismissHint : MultiQuizEvent
}
