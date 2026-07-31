package io.github.ptimulka.miecz.screens.riddles.fill_words

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
    val phase: Phase = Phase.Answering
) {
    val allFieldsFilled: Boolean get() = userInputs.all { it.isNotEmpty() }

    sealed interface Phase {
        data object Answering : Phase
        data object ShowingHintImage : Phase
        data class Result(val correct: Boolean) : Phase
    }
}
