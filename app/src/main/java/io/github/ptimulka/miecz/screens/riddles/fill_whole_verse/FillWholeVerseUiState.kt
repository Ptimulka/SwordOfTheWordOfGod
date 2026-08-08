package io.github.ptimulka.miecz.screens.riddles.fill_whole_verse

import io.github.ptimulka.miecz.helpers.DiffPart
import io.github.ptimulka.miecz.screens.riddles.base.BaseRiddleUiState
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase

data class FillWholeVerseUiState(
    val book: String,
    val chapter: Int,
    val number: String,
    val userInput: String = "",
    val similarityScore: Float = 0f,
    val diffs: List<DiffPart> = emptyList(),
    override val phase: RiddlePhase = RiddlePhase.Answering
) : BaseRiddleUiState
