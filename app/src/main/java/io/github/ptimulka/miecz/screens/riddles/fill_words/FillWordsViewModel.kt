package io.github.ptimulka.miecz.screens.riddles.fill_words

import io.github.ptimulka.miecz.helpers.FillWordsPartBuilder
import io.github.ptimulka.miecz.helpers.foldPolishChars
import io.github.ptimulka.miecz.screens.riddles.base.BaseRiddleViewModel
import io.github.ptimulka.miecz.screens.riddles.base.RiddleEvent
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase
import kotlinx.coroutines.flow.update

data class FillWordsArgs(
    val verseText: String,
    val book: String,
    val chapter: Int,
    val number: String,
    val isEasy: Boolean,
    val moreWords: Boolean,
    val sectionId: Int = 0,
    val verseIndex: Int = 0,
    val assetName: String? = null,
    val hasHint: Boolean = false
)

class FillWordsViewModel(
    private val args: FillWordsArgs,
    mnemonicRepo: io.github.ptimulka.miecz.repositories.MnemonicRepository? = null
) : BaseRiddleViewModel<FillWordsUiState>(
    initialState = FillWordsUiState(
        book = args.book,
        chapter = args.chapter,
        number = args.number
    ),
    mnemonicRepo = mnemonicRepo,
    sectionId = args.sectionId,
    verseIndex = args.verseIndex,
    assetName = args.assetName
) {

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
            FillWordsEvent.Check -> onBaseEvent(RiddleEvent.Check)
            FillWordsEvent.DismissResult -> onBaseEvent(RiddleEvent.DismissResult)
            FillWordsEvent.ShowHint -> onBaseEvent(RiddleEvent.ShowHint)
            FillWordsEvent.DismissHint -> onBaseEvent(RiddleEvent.DismissHint)
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

    override fun checkAnswer() {
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

        _state.update { it.copy(wrongInputIndices = wrongIndices) }
        setResult(allCorrect, args.hasHint)
    }

    override fun updatePhase(state: FillWordsUiState, newPhase: RiddlePhase): FillWordsUiState {
        return state.copy(phase = newPhase)
    }

    override fun updateHintBitmap(state: FillWordsUiState, bitmap: android.graphics.Bitmap?): FillWordsUiState {
        return state.copy(hintBitmap = bitmap)
    }
}
