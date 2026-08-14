package io.github.ptimulka.miecz.screens.riddles.quiz

import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.QuizAnswerBuilder
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

data class QuizArgs(
    val verseText: String,
    val book: String,
    val chapter: Int,
    val number: String,
    val isEasy: Boolean,
    val sectionVerses: List<Verse>,
    val sectionId: Int,
    val verseIndex: Int,
    val assetName: String?,
    val hasHint: Boolean
)

@HiltViewModel(assistedFactory = QuizViewModel.Factory::class)
class QuizViewModel @AssistedInject constructor(
    @Assisted private val args: QuizArgs,
    mnemonicRepo: MnemonicRepository,
    ioDispatcher: CoroutineDispatcher
) : BaseRiddleViewModel<QuizUiState>(
    initialState = QuizUiState(verseText = args.verseText),
    mnemonicRepo = mnemonicRepo,
    sectionId = args.sectionId,
    verseIndex = args.verseIndex,
    assetName = args.assetName,
    ioDispatcher = ioDispatcher
) {

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
            QuizEvent.Check -> onBaseEvent(RiddleEvent.Check)
            QuizEvent.DismissResult -> onBaseEvent(RiddleEvent.DismissResult)
            QuizEvent.DismissHint -> onBaseEvent(RiddleEvent.DismissHint)
        }
    }

    override fun checkAnswer() {
        val selected = _state.value.selectedAnswer ?: return
        val isCorrect = selected == correctAnswer
        setResult(isCorrect, args.hasHint)
    }

    override fun updatePhase(state: QuizUiState, newPhase: RiddlePhase): QuizUiState {
        return state.copy(phase = newPhase)
    }

    override fun updateHintBitmap(state: QuizUiState, bitmap: android.graphics.Bitmap?): QuizUiState {
        return state.copy(hintBitmap = bitmap)
    }

    @AssistedFactory
    interface Factory {
        fun create(args: QuizArgs): QuizViewModel
    }
}
