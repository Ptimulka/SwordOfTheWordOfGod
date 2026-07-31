package io.github.ptimulka.miecz.screens.riddles.multi_quiz

data class MultiQuizUiState(
    val bookAnswers: List<String> = emptyList(),
    val chapterAnswers: List<String> = emptyList(),
    val verseAnswers: List<String> = emptyList(),
    val selectedBook: String? = null,
    val selectedChapter: String? = null,
    val selectedVerse: String? = null,
    val wrongBook: String? = null,
    val wrongChapter: String? = null,
    val wrongVerse: String? = null,
    val phase: Phase = Phase.Answering
) {
    val checkEnabled: Boolean get() = selectedBook != null && selectedChapter != null && selectedVerse != null

    sealed interface Phase {
        data object Answering : Phase
        data object ShowingImageReward : Phase
        data class Result(val correct: Boolean) : Phase
    }
}
