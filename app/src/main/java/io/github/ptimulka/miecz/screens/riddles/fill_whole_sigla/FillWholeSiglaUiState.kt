package io.github.ptimulka.miecz.screens.riddles.fill_whole_sigla

import io.github.ptimulka.miecz.screens.riddles.base.BaseRiddleUiState
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase

data class FillWholeSiglaUiState(
    val bookInput: String = "",
    val chapterInput: String = "",
    val verseInput: String = "",
    val wrongIndices: Set<Int> = emptySet(),
    override val phase: RiddlePhase = RiddlePhase.Answering
) : BaseRiddleUiState {
    val allFieldsFilled: Boolean get() = bookInput.isNotBlank() && chapterInput.isNotBlank() && verseInput.isNotBlank()
}
