package io.github.ptimulka.miecz.screens.riddles.fill_sigla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.helpers.BookNameNormalizer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FillSiglaArgs(
    val book: String,
    val chapter: Int,
    val number: String,
    val fillType: FillSiglaType,
    val hasHint: Boolean
)

class FillSiglaViewModel(
    private val args: FillSiglaArgs
) : ViewModel() {

    private val _state = MutableStateFlow(FillSiglaUiState())
    val state = _state.asStateFlow()

    private val _effects = Channel<FillSiglaEffect>()
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: FillSiglaEvent) {
        when (event) {
            is FillSiglaEvent.UpdateInput -> _state.update { it.copy(userInput = event.value) }
            FillSiglaEvent.Check -> checkAnswer()
            FillSiglaEvent.DismissResult -> {
                val s = _state.value
                val phase = s.phase
                if (phase is FillSiglaUiState.Phase.Result && phase.correct) {
                    viewModelScope.launch { _effects.send(FillSiglaEffect.Success) }
                }
                _state.update { it.copy(phase = FillSiglaUiState.Phase.Answering) }
            }
            FillSiglaEvent.DismissImage -> {
                _state.update { it.copy(phase = FillSiglaUiState.Phase.Result(correct = true)) }
            }
        }
    }

    private fun checkAnswer() {
        val userInput = _state.value.userInput
        val isCorrect = when (args.fillType) {
            FillSiglaType.BOOK -> BookNameNormalizer.getCanonicalSigla(userInput) == args.book
            FillSiglaType.CHAPTER -> userInput == args.chapter.toString()
            FillSiglaType.VERSE -> checkVerseNumber(userInput, args.number)
        }

        if (isCorrect && args.hasHint) {
            _state.update { it.copy(phase = FillSiglaUiState.Phase.ShowingImageReward) }
        } else {
            _state.update { it.copy(phase = FillSiglaUiState.Phase.Result(correct = isCorrect)) }
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
