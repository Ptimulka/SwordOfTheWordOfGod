package io.github.ptimulka.miecz.screens.riddles.multi_quiz

import io.github.ptimulka.miecz.screens.riddles.base.BaseRiddleUiState
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase

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
    override val phase: RiddlePhase = RiddlePhase.Answering,
    override val hintBitmap: android.graphics.Bitmap? = null
) : BaseRiddleUiState {
    val checkEnabled: Boolean get() = selectedBook != null && selectedChapter != null && selectedVerse != null
}
