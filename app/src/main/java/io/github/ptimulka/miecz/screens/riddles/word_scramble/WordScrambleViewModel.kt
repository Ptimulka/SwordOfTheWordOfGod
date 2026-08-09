package io.github.ptimulka.miecz.screens.riddles.word_scramble

import io.github.ptimulka.miecz.data.WordItem
import io.github.ptimulka.miecz.helpers.WordScramblePartBuilder
import io.github.ptimulka.miecz.screens.riddles.base.BaseRiddleViewModel
import io.github.ptimulka.miecz.screens.riddles.base.RiddleEvent
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase
import kotlinx.coroutines.flow.update
import java.util.Collections

data class WordScrambleArgs(
    val verseText: String,
    val book: String,
    val chapter: Int,
    val number: String,
    val isEasy: Boolean,
    val sectionId: Int = 0,
    val verseIndex: Int = 0,
    val assetName: String? = null,
    val hasHint: Boolean = false
)

class WordScrambleViewModel(
    private val args: WordScrambleArgs,
    mnemonicRepo: io.github.ptimulka.miecz.repositories.MnemonicRepository? = null
) : BaseRiddleViewModel<WordScrambleUiState>(
    initialState = WordScrambleUiState(
        book = args.book,
        chapter = args.chapter,
        number = args.number
    ),
    mnemonicRepo = mnemonicRepo,
    sectionId = args.sectionId,
    verseIndex = args.verseIndex,
    assetName = args.assetName
) {

    private val correctWordsStrings = WordScramblePartBuilder.build(args.verseText, args.isEasy)
    private val initialShuffledWords = correctWordsStrings.mapIndexed { index, word ->
        WordItem(index, word)
    }.shuffled()

    init {
        _state.update { it.copy(availableWords = initialShuffledWords) }
    }

    fun onEvent(event: WordScrambleEvent) {
        when (event) {
            is WordScrambleEvent.PlaceWord -> placeWord(event.word)
            is WordScrambleEvent.UnplaceWord -> unplaceWord(event.word)
            is WordScrambleEvent.SelectForReorder -> _state.update { it.copy(selectedWordForReorder = event.word, wrongWords = emptySet()) }
            is WordScrambleEvent.MoveLeft -> moveWord(event.index, -1)
            is WordScrambleEvent.MoveRight -> moveWord(event.index, 1)
            WordScrambleEvent.Reset -> reset()
            WordScrambleEvent.Check -> onBaseEvent(RiddleEvent.Check)
            WordScrambleEvent.DismissResult -> onBaseEvent(RiddleEvent.DismissResult)
            WordScrambleEvent.ShowHint -> onBaseEvent(RiddleEvent.ShowHint)
            WordScrambleEvent.DismissHint -> onBaseEvent(RiddleEvent.DismissHint)
        }
    }

    private fun placeWord(word: WordItem) {
        _state.update { 
            it.copy(
                availableWords = it.availableWords - word,
                placedWords = it.placedWords + word,
                wrongWords = emptySet()
            )
        }
    }

    private fun unplaceWord(word: WordItem) {
        _state.update { 
            it.copy(
                placedWords = it.placedWords - word,
                availableWords = it.availableWords + word,
                wrongWords = emptySet(),
                selectedWordForReorder = if (it.selectedWordForReorder == word) null else it.selectedWordForReorder
            )
        }
    }

    private fun moveWord(index: Int, delta: Int) {
        _state.update { 
            val newPlaced = it.placedWords.toMutableList()
            val targetIndex = index + delta
            if (targetIndex in newPlaced.indices) {
                Collections.swap(newPlaced, index, targetIndex)
            }
            it.copy(placedWords = newPlaced, wrongWords = emptySet())
        }
    }

    private fun reset() {
        _state.update { 
            it.copy(
                placedWords = emptyList(),
                availableWords = initialShuffledWords,
                wrongWords = emptySet(),
                selectedWordForReorder = null,
                phase = RiddlePhase.Answering
            )
        }
    }

    override fun checkAnswer() {
        val s = _state.value
        val wrongWords = mutableSetOf<WordItem>()
        var allCorrect = true

        if (s.placedWords.size != correctWordsStrings.size) {
            allCorrect = false
        } else {
            s.placedWords.forEachIndexed { index, wordItem ->
                if (wordItem.text != correctWordsStrings[index]) {
                    wrongWords.add(wordItem)
                    allCorrect = false
                }
            }
        }

        _state.update { it.copy(wrongWords = wrongWords) }
        setResult(allCorrect, args.hasHint)
    }

    override fun updatePhase(state: WordScrambleUiState, newPhase: RiddlePhase): WordScrambleUiState {
        return state.copy(phase = newPhase)
    }

    override fun updateHintBitmap(state: WordScrambleUiState, bitmap: android.graphics.Bitmap?): WordScrambleUiState {
        return state.copy(hintBitmap = bitmap)
    }
}
