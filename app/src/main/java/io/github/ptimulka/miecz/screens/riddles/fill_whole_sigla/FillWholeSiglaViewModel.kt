package io.github.ptimulka.miecz.screens.riddles.fill_whole_sigla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.helpers.BookNameNormalizer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FillWholeSiglaArgs(
    val book: String,
    val chapter: Int,
    val number: String,
    val hasHint: Boolean
)

class FillWholeSiglaViewModel(
    private val args: FillWholeSiglaArgs
) : ViewModel() {

    private val _state = MutableStateFlow(FillWholeSiglaUiState())
    val state = _state.asStateFlow()

    private val _effects = Channel<FillWholeSiglaEffect>()
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: FillWholeSiglaEvent) {
        when (event) {
            is FillWholeSiglaEvent.UpdateBook -> _state.update { it.copy(bookInput = event.value, wrongIndices = it.wrongIndices - 0) }
            is FillWholeSiglaEvent.UpdateChapter -> _state.update { it.copy(chapterInput = event.value, wrongIndices = it.wrongIndices - 1) }
            is FillWholeSiglaEvent.UpdateVerse -> _state.update { it.copy(verseInput = event.value, wrongIndices = it.wrongIndices - 2) }
            FillWholeSiglaEvent.Check -> checkAnswer()
            FillWholeSiglaEvent.DismissResult -> {
                val s = _state.value
                val phase = s.phase
                if (phase is FillWholeSiglaUiState.Phase.Result && phase.correct) {
                    viewModelScope.launch { _effects.send(FillWholeSiglaEffect.Success) }
                }
                _state.update { it.copy(phase = FillWholeSiglaUiState.Phase.Answering) }
            }
            FillWholeSiglaEvent.DismissImage -> {
                _state.update { it.copy(phase = FillWholeSiglaUiState.Phase.Result(correct = true)) }
            }
        }
    }

    private fun checkAnswer() {
        val s = _state.value
        val isBookCorrect = BookNameNormalizer.getCanonicalSigla(s.bookInput) == args.book
        val isChapterCorrect = s.chapterInput == args.chapter.toString()
        val isVerseCorrect = checkVerseNumber(s.verseInput, args.number)

        val wrongIndices = mutableSetOf<Int>()
        if (!isBookCorrect) wrongIndices.add(0)
        if (!isChapterCorrect) wrongIndices.add(1)
        if (!isVerseCorrect) wrongIndices.add(2)

        val isCorrect = isBookCorrect && isChapterCorrect && isVerseCorrect

        if (isCorrect && args.hasHint) {
            _state.update { it.copy(phase = FillWholeSiglaUiState.Phase.ShowingImageReward) }
        } else {
            _state.update { 
                it.copy(
                    wrongIndices = wrongIndices,
                    phase = FillWholeSiglaUiState.Phase.Result(correct = isCorrect)
                )
            }
        }
    }

    private fun checkVerseNumber(userInput: String, correctNumber: String): Boolean {
        return if (correctNumber.contains("-")) {
            val parts = correctNumber.split("-").mapNotNull { it.toIntOrNull() }
            if (parts.size == 2) {
                val userNum = userInput.toIntOrNull()
                userNum != null && userNum >= parts[0] && userNum <= parts[1]
            } else {
                userInput == correctNumber
            }
        } else {
            userInput == correctNumber
        }
    }
}
