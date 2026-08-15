package io.github.ptimulka.miecz.screens.riddles.fill_whole_verse

import android.graphics.Bitmap
import io.github.ptimulka.miecz.helpers.calculateWordSimilarity
import io.github.ptimulka.miecz.helpers.diffWords
import io.github.ptimulka.miecz.helpers.normalizeVerseText
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

data class FillWholeVerseArgs(
    val verseText: String,
    val book: String,
    val chapter: Int,
    val number: String,
    val sectionId: Int = 0,
    val verseIndex: Int = 0,
    val assetName: String? = null,
    val hasHint: Boolean = false
)

@HiltViewModel(assistedFactory = FillWholeVerseViewModel.Factory::class)
class FillWholeVerseViewModel @AssistedInject constructor(
    @Assisted private val args: FillWholeVerseArgs,
    mnemonicRepo: MnemonicRepository,
    ioDispatcher: CoroutineDispatcher
) : BaseRiddleViewModel<FillWholeVerseUiState>(
    initialState = FillWholeVerseUiState(
        book = args.book,
        chapter = args.chapter,
        number = args.number
    ),
    mnemonicRepo = mnemonicRepo,
    sectionId = args.sectionId,
    verseIndex = args.verseIndex,
    assetName = args.assetName,
    ioDispatcher = ioDispatcher
) {

    fun onEvent(event: FillWholeVerseEvent) {
        when (event) {
            is FillWholeVerseEvent.UpdateInput -> _state.update { it.copy(userInput = event.value) }
            FillWholeVerseEvent.Check -> onBaseEvent(RiddleEvent.Check)
            FillWholeVerseEvent.DismissResult -> onBaseEvent(RiddleEvent.DismissResult)
            FillWholeVerseEvent.ShowHint -> onBaseEvent(RiddleEvent.ShowHint)
            FillWholeVerseEvent.DismissHint -> onBaseEvent(RiddleEvent.DismissHint)
        }
    }

    override fun checkAnswer() {
        val userInput = _state.value.userInput
        val userWords = normalizeVerseText(userInput).split(' ').filter { it.isNotEmpty() }

        val verseWithSpaces = args.verseText.replace("_", " ")
        val wordsWithOptional = normalizeVerseText(verseWithSpaces.replace("*", "")).split(' ').filter { it.isNotEmpty() }
        val wordsWithoutOptional = normalizeVerseText(verseWithSpaces.replace(Regex("\\*.*?\\*"), "")).split(' ').filter { it.isNotEmpty() }

        val similarity1 = calculateWordSimilarity(userWords, wordsWithOptional)
        val similarity2 = calculateWordSimilarity(userWords, wordsWithoutOptional)

        val similarityScore: Float
        val bestCorrectWords: List<String>
        if (similarity1 > similarity2) {
            similarityScore = similarity1
            bestCorrectWords = wordsWithOptional
        } else {
            similarityScore = similarity2
            bestCorrectWords = wordsWithoutOptional
        }

        val isAnswerCorrect = similarityScore >= 90.0f
        val diffs = if (similarityScore < 100.0f) {
            diffWords(bestCorrectWords.joinToString(" "), userWords.joinToString(" "))
        } else {
            emptyList()
        }

        _state.update { 
            it.copy(
                similarityScore = similarityScore,
                diffs = diffs
            )
        }
        setResult(isAnswerCorrect, args.hasHint)
    }

    override fun updatePhase(state: FillWholeVerseUiState, newPhase: RiddlePhase): FillWholeVerseUiState {
        return state.copy(phase = newPhase)
    }

    override fun updateHintBitmap(state: FillWholeVerseUiState, bitmap: Bitmap?): FillWholeVerseUiState {
        return state.copy(hintBitmap = bitmap)
    }

    @AssistedFactory
    interface Factory {
        fun create(args: FillWholeVerseArgs): FillWholeVerseViewModel
    }
}
