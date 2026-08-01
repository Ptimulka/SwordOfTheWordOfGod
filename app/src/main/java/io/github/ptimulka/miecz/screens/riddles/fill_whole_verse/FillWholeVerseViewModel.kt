package io.github.ptimulka.miecz.screens.riddles.fill_whole_verse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.helpers.calculateWordSimilarity
import io.github.ptimulka.miecz.helpers.diffWords
import io.github.ptimulka.miecz.helpers.normalizeVerseText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FillWholeVerseArgs(
    val verseText: String,
    val book: String,
    val chapter: Int,
    val number: String
)

class FillWholeVerseViewModel(
    private val args: FillWholeVerseArgs
) : ViewModel() {

    private val _state = MutableStateFlow(
        FillWholeVerseUiState(
            book = args.book,
            chapter = args.chapter,
            number = args.number
        )
    )
    val state = _state.asStateFlow()

    private val _effects = Channel<FillWholeVerseEffect>()
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: FillWholeVerseEvent) {
        when (event) {
            is FillWholeVerseEvent.UpdateInput -> _state.update { it.copy(userInput = event.value) }
            FillWholeVerseEvent.Check -> checkAnswer()
            FillWholeVerseEvent.DismissResult -> {
                val s = _state.value
                val phase = s.phase
                if (phase is FillWholeVerseUiState.Phase.Result && phase.correct) {
                    viewModelScope.launch { _effects.send(FillWholeVerseEffect.Success) }
                }
                _state.update { it.copy(phase = FillWholeVerseUiState.Phase.Answering) }
            }
            FillWholeVerseEvent.ShowHint -> _state.update { it.copy(phase = FillWholeVerseUiState.Phase.ShowingHintImage) }
            FillWholeVerseEvent.DismissHint -> _state.update { it.copy(phase = FillWholeVerseUiState.Phase.Answering) }
        }
    }

    private fun checkAnswer() {
        val userInput = _state.value.userInput
        val userWords = normalizeVerseText(userInput).split(' ').filter { it.isNotEmpty() }

        val verseWithSpaces = args.verseText.replace("_", " ")

        // Version 1: With text in asterisks (the optional parts)
        val wordsWithOptional = normalizeVerseText(verseWithSpaces.replace("*", "")).split(' ').filter { it.isNotEmpty() }

        // Version 2: Without text in asterisks
        val wordsWithoutOptional = normalizeVerseText(verseWithSpaces.replace(Regex("\\*.*?\\*"), "")).split(' ').filter { it.isNotEmpty() }

        // Calculate similarity for both versions
        val similarity1 = calculateWordSimilarity(userWords, wordsWithOptional)
        val similarity2 = calculateWordSimilarity(userWords, wordsWithoutOptional)

        // Choose the best match
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
                diffs = diffs,
                phase = FillWholeVerseUiState.Phase.Result(correct = isAnswerCorrect)
            )
        }
    }
}
