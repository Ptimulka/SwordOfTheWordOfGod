package io.github.ptimulka.miecz.screens.riddles.word_scramble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.data.WordItem
import io.github.ptimulka.miecz.helpers.WordScramblePartBuilder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections

data class WordScrambleArgs(
    val verseText: String,
    val book: String,
    val chapter: Int,
    val number: String,
    val isEasy: Boolean
)

class WordScrambleViewModel(
    private val args: WordScrambleArgs
) : ViewModel() {

    private val _state = MutableStateFlow(
        WordScrambleUiState(
            book = args.book,
            chapter = args.chapter,
            number = args.number
        )
    )
    val state = _state.asStateFlow()

    private val _effects = Channel<WordScrambleEffect>()
    val effects = _effects.receiveAsFlow()

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
            WordScrambleEvent.Check -> checkAnswer()
            WordScrambleEvent.DismissResult -> {
                val s = _state.value
                val phase = s.phase
                if (phase is WordScrambleUiState.Phase.Result && phase.correct) {
                    viewModelScope.launch { _effects.send(WordScrambleEffect.Success) }
                }
                _state.update { it.copy(phase = WordScrambleUiState.Phase.Answering) }
            }
            WordScrambleEvent.ShowHint -> _state.update { it.copy(phase = WordScrambleUiState.Phase.ShowingHintImage) }
            WordScrambleEvent.DismissHint -> _state.update { it.copy(phase = WordScrambleUiState.Phase.Answering) }
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
                phase = WordScrambleUiState.Phase.Answering
            )
        }
    }

    private fun checkAnswer() {
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

        _state.update { 
            it.copy(
                wrongWords = wrongWords,
                phase = WordScrambleUiState.Phase.Result(correct = allCorrect)
            )
        }
    }
}
