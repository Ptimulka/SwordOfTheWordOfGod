package io.github.ptimulka.miecz.screens.riddles.multi_quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.helpers.MultiQuizAnswerBuilder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MultiQuizArgs(
    val book: String,
    val chapter: Int,
    val number: String,
    val hasHint: Boolean
)

class MultiQuizViewModel(
    private val args: MultiQuizArgs
) : ViewModel() {

    private val _state = MutableStateFlow(MultiQuizUiState())
    val state = _state.asStateFlow()

    private val _effects = Channel<MultiQuizEffect>()
    val effects = _effects.receiveAsFlow()

    init {
        val answers = MultiQuizAnswerBuilder.build(
            correctBook = args.book,
            correctChapter = args.chapter.toString(),
            correctVerse = args.number
        )
        _state.update {
            it.copy(
                bookAnswers = answers.books,
                chapterAnswers = answers.chapters,
                verseAnswers = answers.verses
            )
        }
    }

    fun onEvent(event: MultiQuizEvent) {
        when (event) {
            is MultiQuizEvent.SelectBook -> _state.update { it.copy(selectedBook = event.book, wrongBook = null, wrongChapter = null, wrongVerse = null) }
            is MultiQuizEvent.SelectChapter -> _state.update { it.copy(selectedChapter = event.chapter, wrongBook = null, wrongChapter = null, wrongVerse = null) }
            is MultiQuizEvent.SelectVerse -> _state.update { it.copy(selectedVerse = event.verse, wrongBook = null, wrongChapter = null, wrongVerse = null) }
            MultiQuizEvent.Check -> checkAnswer()
            MultiQuizEvent.DismissResult -> {
                val s = _state.value
                val phase = s.phase
                if (phase is MultiQuizUiState.Phase.Result && phase.correct) {
                    viewModelScope.launch { _effects.send(MultiQuizEffect.Success) }
                }
                _state.update { it.copy(phase = MultiQuizUiState.Phase.Answering) }
            }
            MultiQuizEvent.DismissImage -> {
                _state.update { it.copy(phase = MultiQuizUiState.Phase.Result(correct = true)) }
            }
        }
    }

    private fun checkAnswer() {
        val s = _state.value
        val correctBook = args.book
        val correctChapter = args.chapter.toString()
        val correctVerse = args.number

        val wrongBook = if (s.selectedBook != correctBook) s.selectedBook else null
        val wrongChapter = if (s.selectedChapter != correctChapter) s.selectedChapter else null
        val wrongVerse = if (s.selectedVerse != correctVerse) s.selectedVerse else null

        val isCorrect = wrongBook == null && wrongChapter == null && wrongVerse == null

        if (isCorrect && args.hasHint) {
            _state.update { it.copy(phase = MultiQuizUiState.Phase.ShowingImageReward) }
        } else {
            _state.update { 
                it.copy(
                    wrongBook = wrongBook,
                    wrongChapter = wrongChapter,
                    wrongVerse = wrongVerse,
                    phase = MultiQuizUiState.Phase.Result(correct = isCorrect)
                )
            }
        }
    }
}
