package io.github.ptimulka.miecz.screens.riddles.word_scramble

import io.github.ptimulka.miecz.data.WordItem
import io.github.ptimulka.miecz.screens.riddles.base.BaseRiddleUiState
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase

data class WordScrambleUiState(
    val book: String,
    val chapter: Int,
    val number: String,
    val placedWords: List<WordItem> = emptyList(),
    val availableWords: List<WordItem> = emptyList(),
    val wrongWords: Set<WordItem> = emptySet(),
    val selectedWordForReorder: WordItem? = null,
    override val phase: RiddlePhase = RiddlePhase.Answering,
    override val hintBitmap: android.graphics.Bitmap? = null
) : BaseRiddleUiState {
    val checkEnabled: Boolean get() = availableWords.isEmpty()
}
