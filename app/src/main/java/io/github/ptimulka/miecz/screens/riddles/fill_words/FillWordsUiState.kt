package io.github.ptimulka.miecz.screens.riddles.fill_words

import io.github.ptimulka.miecz.screens.riddles.base.BaseRiddleUiState
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase

sealed class VersePart {
    data class StaticText(val text: String, val isGreyed: Boolean = false) : VersePart()
    data class WordToFill(val correctWord: String, val hint: String) : VersePart()
}

data class FillWordsUiState(
    val book: String,
    val chapter: Int,
    val number: String,
    val verseParts: List<VersePart> = emptyList(),
    val userInputs: List<String> = emptyList(),
    val wrongInputIndices: Set<Int> = emptySet(),
    override val phase: RiddlePhase = RiddlePhase.Answering,
    override val hintBitmap: android.graphics.Bitmap? = null
) : BaseRiddleUiState {
    val allFieldsFilled: Boolean get() = userInputs.all { it.isNotEmpty() }
}
