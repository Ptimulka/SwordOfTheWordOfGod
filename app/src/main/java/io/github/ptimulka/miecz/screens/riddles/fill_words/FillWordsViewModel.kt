package io.github.ptimulka.miecz.screens.riddles.fill_words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.helpers.FillWordsPartBuilder
import io.github.ptimulka.miecz.helpers.foldPolishChars
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FillWordsArgs(
    val verseText: String,
    val book: String,
    val chapter: Int,
    val number: String,
    val isEasy: Boolean,
    val moreWords: Boolean,
    val hasHint: Boolean
)

class FillWordsViewModel(
    private val args: FillWordsArgs
) : ViewModel() {

    private val _state = MutableStateFlow(
        FillWordsUiState(
            book = args.book,
            chapter = args.chapter,
            number = args.number
        )
    )
    val state = _state.asStateFlow()

    private val _effects = Channel<FillWordsEffect>()
    val effects = _effects.receiveAsFlow()

    init {
        val parts = FillWordsPartBuilder.build(args.verseText, args.isEasy, args.moreWords)
        val fillableCount = parts.count { it is VersePart.WordToFill }
        _state.update {
            it.copy(
                verseParts = parts,
                userInputs = List(fillableCount) { "" }
            )
        }
    }

    fun onEvent(event: FillWordsEvent) {
        when (event) {
            is FillWordsEvent.UpdateInput -> updateInput(event.index, event.value)
            FillWordsEvent.Check -> checkAnswer()
            FillWordsEvent.DismissResult -> {
                val s = _state.value
                val phase = s.phase
                if (phase is FillWordsUiState.Phase.Result && phase.correct) {
                    viewModelScope.launch { _effects.send(FillWordsEffect.Success) }
                }
                _state.update { it.copy(phase = FillWordsUiState.Phase.Answering) }
            }
            FillWordsEvent.ShowHint -> _state.update { it.copy(phase = FillWordsUiState.Phase.ShowingHintImage) }
            FillWordsEvent.DismissHint -> _state.update { it.copy(phase = FillWordsUiState.Phase.Answering) }
        }
    }

    private fun updateInput(index: Int, value: String) {
        _state.update { 
            val newInputs = it.userInputs.toMutableList()
            if (index in newInputs.indices) {
                newInputs[index] = value
            }
            it.copy(
                userInputs = newInputs,
                wrongInputIndices = it.wrongInputIndices - index
            )
        }
    }

    private fun checkAnswer() {
        val s = _state.value
        val fillableParts = s.verseParts.filterIsInstance<VersePart.WordToFill>()
        val wrongIndices = mutableSetOf<Int>()
        var allCorrect = true

        fillableParts.forEachIndexed { i, part ->
            val userInput = s.userInputs.getOrElse(i) { "" }.trim()
            val isCorrect = foldPolishChars(userInput).equals(foldPolishChars(part.correctWord), ignoreCase = true)
            if (!isCorrect) {
                wrongIndices.add(i)
                allCorrect = false
            }
        }

        _state.update { 
            it.copy(
                wrongInputIndices = wrongIndices,
                phase = FillWordsUiState.Phase.Result(correct = allCorrect)
            )
        }
    }
}
