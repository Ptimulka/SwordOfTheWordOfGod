package io.github.ptimulka.miecz.screens.riddles.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.QuizAnswerBuilder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizArgs(
    val verseText: String,
    val book: String,
    val chapter: Int,
    val number: String,
    val isEasy: Boolean,
    val sectionVerses: List<Verse>,
    val sectionId: Int,
    val verseIndex: Int,
    val hasHint: Boolean
)

class QuizViewModel(
    private val args: QuizArgs
) : ViewModel() {

    private val _state = MutableStateFlow(QuizUiState(verseText = args.verseText))
    val state = _state.asStateFlow()

    private val _effects = Channel<QuizEffect>()
    val effects = _effects.receiveAsFlow()

    private val correctAnswer = "${args.book} ${args.chapter},${args.number}"

    init {
        _state.update {
            it.copy(
                answers = QuizAnswerBuilder.build(
                    correctAnswer = correctAnswer,
                    isEasy = args.isEasy,
                    sectionVerses = args.sectionVerses
                )
            )
        }
    }

    fun onEvent(event: QuizEvent) {
        when (event) {
            is QuizEvent.Select -> _state.update { it.copy(selectedAnswer = event.answer) }
            QuizEvent.Check -> checkAnswer()
            QuizEvent.DismissResult -> {
                val s = _state.value
                val phase = s.phase
                if (phase is QuizUiState.Phase.Result && phase.correct) {
                    viewModelScope.launch { _effects.send(QuizEffect.Success) }
                }
                _state.update { it.copy(phase = QuizUiState.Phase.Answering) }
            }
            QuizEvent.DismissImage -> {
                _state.update { it.copy(phase = QuizUiState.Phase.Result(correct = true)) }
            }
        }
    }

    private fun checkAnswer() {
        val selected = _state.value.selectedAnswer ?: return
        val isCorrect = selected == correctAnswer

        if (isCorrect && args.hasHint) {
            _state.update { it.copy(phase = QuizUiState.Phase.ShowingImageReward) }
        } else {
            _state.update { it.copy(phase = QuizUiState.Phase.Result(correct = isCorrect)) }
        }
    }
}
