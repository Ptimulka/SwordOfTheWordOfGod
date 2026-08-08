package io.github.ptimulka.miecz.screens.riddles.fill_sigla

import io.github.ptimulka.miecz.helpers.BookNameNormalizer
import io.github.ptimulka.miecz.screens.riddles.base.BaseRiddleViewModel
import io.github.ptimulka.miecz.screens.riddles.base.RiddleEvent
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase
import kotlinx.coroutines.flow.update

data class FillSiglaArgs(
    val book: String,
    val chapter: Int,
    val number: String,
    val fillType: FillSiglaType,
    val hasHint: Boolean
)

class FillSiglaViewModel(
    private val args: FillSiglaArgs
) : BaseRiddleViewModel<FillSiglaUiState>(FillSiglaUiState()) {

    fun onEvent(event: FillSiglaEvent) {
        when (event) {
            is FillSiglaEvent.UpdateInput -> _state.update { it.copy(userInput = event.value) }
            FillSiglaEvent.Check -> onBaseEvent(RiddleEvent.Check)
            FillSiglaEvent.DismissResult -> onBaseEvent(RiddleEvent.DismissResult)
            FillSiglaEvent.DismissHint -> onBaseEvent(RiddleEvent.DismissHint)
        }
    }

    override fun checkAnswer() {
        val userInput = _state.value.userInput
        val isCorrect = when (args.fillType) {
            FillSiglaType.BOOK -> BookNameNormalizer.getCanonicalSigla(userInput) == args.book
            FillSiglaType.CHAPTER -> userInput == args.chapter.toString()
            FillSiglaType.VERSE -> checkVerseNumber(userInput, args.number)
        }

        setResult(isCorrect, args.hasHint)
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

    override fun updatePhase(state: FillSiglaUiState, newPhase: RiddlePhase): FillSiglaUiState {
        return state.copy(phase = newPhase)
    }
}
