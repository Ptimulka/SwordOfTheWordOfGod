package io.github.ptimulka.miecz.screens.riddles.fill_whole_sigla

import android.graphics.Bitmap
import io.github.ptimulka.miecz.helpers.BookNameNormalizer
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

data class FillWholeSiglaArgs(
    val book: String,
    val chapter: Int,
    val number: String,
    val sectionId: Int = 0,
    val verseIndex: Int = 0,
    val assetName: String? = null,
    val hasHint: Boolean = false
)

@HiltViewModel(assistedFactory = FillWholeSiglaViewModel.Factory::class)
class FillWholeSiglaViewModel @AssistedInject constructor(
    @Assisted private val args: FillWholeSiglaArgs,
    mnemonicRepo: MnemonicRepository,
    ioDispatcher: CoroutineDispatcher
) : BaseRiddleViewModel<FillWholeSiglaUiState>(
    initialState = FillWholeSiglaUiState(),
    mnemonicRepo = mnemonicRepo,
    sectionId = args.sectionId,
    verseIndex = args.verseIndex,
    assetName = args.assetName,
    ioDispatcher = ioDispatcher
) {

    fun onEvent(event: FillWholeSiglaEvent) {
        when (event) {
            is FillWholeSiglaEvent.UpdateBook -> _state.update { it.copy(bookInput = event.value, wrongIndices = it.wrongIndices - 0) }
            is FillWholeSiglaEvent.UpdateChapter -> _state.update { it.copy(chapterInput = event.value, wrongIndices = it.wrongIndices - 1) }
            is FillWholeSiglaEvent.UpdateVerse -> _state.update { it.copy(verseInput = event.value, wrongIndices = it.wrongIndices - 2) }
            FillWholeSiglaEvent.Check -> onBaseEvent(RiddleEvent.Check)
            FillWholeSiglaEvent.DismissResult -> onBaseEvent(RiddleEvent.DismissResult)
            FillWholeSiglaEvent.DismissHint -> onBaseEvent(RiddleEvent.DismissHint)
        }
    }

    override fun checkAnswer() {
        val s = _state.value
        val isBookCorrect = BookNameNormalizer.getCanonicalSigla(s.bookInput) == args.book
        val isChapterCorrect = s.chapterInput == args.chapter.toString()
        val isVerseCorrect = checkVerseNumber(s.verseInput, args.number)

        val wrongIndices = mutableSetOf<Int>()
        if (!isBookCorrect) wrongIndices.add(0)
        if (!isChapterCorrect) wrongIndices.add(1)
        if (!isVerseCorrect) wrongIndices.add(2)

        val isCorrect = isBookCorrect && isChapterCorrect && isVerseCorrect

        _state.update { it.copy(wrongIndices = wrongIndices) }
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

    override fun updatePhase(state: FillWholeSiglaUiState, newPhase: RiddlePhase): FillWholeSiglaUiState {
        return state.copy(phase = newPhase)
    }

    override fun updateHintBitmap(state: FillWholeSiglaUiState, bitmap: Bitmap?): FillWholeSiglaUiState {
        return state.copy(hintBitmap = bitmap)
    }

    @AssistedFactory
    interface Factory {
        fun create(args: FillWholeSiglaArgs): FillWholeSiglaViewModel
    }
}
