package io.github.ptimulka.miecz.screens.riddles.multi_quiz

import io.github.ptimulka.miecz.helpers.MultiQuizAnswerBuilder
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import io.github.ptimulka.miecz.screens.riddles.base.BaseRiddleViewModel
import io.github.ptimulka.miecz.screens.riddles.base.RiddleEvent
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase
import kotlinx.coroutines.flow.update
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher

data class MultiQuizArgs(
    val book: String,
    val chapter: Int,
    val number: String,
    val sectionId: Int = 0,
    val verseIndex: Int = 0,
    val assetName: String? = null,
    val hasHint: Boolean = false
)

@HiltViewModel(assistedFactory = MultiQuizViewModel.Factory::class)
class MultiQuizViewModel @AssistedInject constructor(
    @Assisted private val args: MultiQuizArgs,
    mnemonicRepo: MnemonicRepository,
    ioDispatcher: CoroutineDispatcher
) : BaseRiddleViewModel<MultiQuizUiState>(
    initialState = MultiQuizUiState(),
    mnemonicRepo = mnemonicRepo,
    sectionId = args.sectionId,
    verseIndex = args.verseIndex,
    assetName = args.assetName,
    ioDispatcher = ioDispatcher
) {

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

    override fun updateHintBitmap(state: MultiQuizUiState, bitmap: android.graphics.Bitmap?): MultiQuizUiState {
        return state.copy(hintBitmap = bitmap)
    }

    @AssistedFactory
    interface Factory {
        fun create(args: MultiQuizArgs): MultiQuizViewModel
    }
}
