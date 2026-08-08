package io.github.ptimulka.miecz.screens.riddles.multi_quiz

import io.github.ptimulka.miecz.helpers.MultiQuizAnswerBuilder
import io.github.ptimulka.miecz.screens.riddles.base.BaseRiddleViewModel
import io.github.ptimulka.miecz.screens.riddles.base.RiddleEvent
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase
import kotlinx.coroutines.flow.update

data class MultiQuizArgs(
    val book: String,
    val chapter: Int,
    val number: String,
    val hasHint: Boolean
)

class MultiQuizViewModel(
    private val args: MultiQuizArgs
) : BaseRiddleViewModel<MultiQuizUiState>(MultiQuizUiState()) {

    private val correctBook = args.book
    private val correctChapter = args.chapter.toString()
    private val correctVerse = args.number

    init {
        val answers = MultiQuizAnswerBuilder.build(
            correctBook = correctBook,
            correctChapter = correctChapter,
            correctVerse = correctVerse
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
            MultiQuizEvent.Check -> onBaseEvent(RiddleEvent.Check)
            MultiQuizEvent.DismissResult -> onBaseEvent(RiddleEvent.DismissResult)
            MultiQuizEvent.DismissHint -> onBaseEvent(RiddleEvent.DismissHint)
        }
    }

    override fun checkAnswer() {
        val s = _state.value
        val wrongBook = if (s.selectedBook != correctBook) s.selectedBook else null
        val wrongChapter = if (s.selectedChapter != correctChapter) s.selectedChapter else null
        val wrongVerse = if (s.selectedVerse != correctVerse) s.selectedVerse else null

        val isCorrect = wrongBook == null && wrongChapter == null && wrongVerse == null

        _state.update { 
            it.copy(
                wrongBook = wrongBook,
                wrongChapter = wrongChapter,
                wrongVerse = wrongVerse
            )
        }
        setResult(isCorrect, args.hasHint)
    }

    override fun updatePhase(state: MultiQuizUiState, newPhase: RiddlePhase): MultiQuizUiState {
        return state.copy(phase = newPhase)
    }
}
