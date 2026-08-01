package io.github.ptimulka.miecz.screens.riddles

import io.github.ptimulka.miecz.screens.riddles.word_scramble.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordScrambleViewModelTest {

    private val defaultArgs = WordScrambleArgs(
        verseText = "Na początku Bóg",
        book = "Rdz",
        chapter = 1,
        number = "1",
        isEasy = false
    )

    @Test
    fun `initial state has all words in available list`() {
        val viewModel = WordScrambleViewModel(defaultArgs)
        val state = viewModel.state.value
        
        assertEquals(3, state.availableWords.size)
        assertTrue(state.placedWords.isEmpty())
        assertFalse(state.checkEnabled)
    }

    @Test
    fun `placing a word moves it from available to placed`() {
        val viewModel = WordScrambleViewModel(defaultArgs)
        val word = viewModel.state.value.availableWords.first()
        
        viewModel.onEvent(WordScrambleEvent.PlaceWord(word))
        
        val state = viewModel.state.value
        assertEquals(2, state.availableWords.size)
        assertEquals(1, state.placedWords.size)
        assertEquals(word, state.placedWords.first())
    }

    @Test
    fun `unplacing a word moves it back to available`() {
        val viewModel = WordScrambleViewModel(defaultArgs)
        val word = viewModel.state.value.availableWords.first()
        
        viewModel.onEvent(WordScrambleEvent.PlaceWord(word))
        viewModel.onEvent(WordScrambleEvent.UnplaceWord(word))
        
        val state = viewModel.state.value
        assertEquals(3, state.availableWords.size)
        assertTrue(state.placedWords.isEmpty())
    }

    @Test
    fun `moving a word swaps positions in placed list`() {
        val viewModel = WordScrambleViewModel(defaultArgs)
        val available = viewModel.state.value.availableWords
        
        viewModel.onEvent(WordScrambleEvent.PlaceWord(available[0]))
        viewModel.onEvent(WordScrambleEvent.PlaceWord(available[1]))
        
        val word0 = viewModel.state.value.placedWords[0]
        val word1 = viewModel.state.value.placedWords[1]
        
        viewModel.onEvent(WordScrambleEvent.MoveRight(0))
        
        assertEquals(word1, viewModel.state.value.placedWords[0])
        assertEquals(word0, viewModel.state.value.placedWords[1])
    }

    @Test
    fun `checking correct sequence updates phase to success`() {
        val viewModel = WordScrambleViewModel(defaultArgs)
        // Find words in correct order (ids are 0, 1, 2)
        val word0 = viewModel.state.value.availableWords.first { it.id == 0 }
        val word1 = viewModel.state.value.availableWords.first { it.id == 1 }
        val word2 = viewModel.state.value.availableWords.first { it.id == 2 }
        
        viewModel.onEvent(WordScrambleEvent.PlaceWord(word0))
        viewModel.onEvent(WordScrambleEvent.PlaceWord(word1))
        viewModel.onEvent(WordScrambleEvent.PlaceWord(word2))
        viewModel.onEvent(WordScrambleEvent.Check)
        
        val phase = viewModel.state.value.phase
        assertTrue(phase is WordScrambleUiState.Phase.Result)
        assertTrue((phase as WordScrambleUiState.Phase.Result).correct)
    }

    @Test
    fun `checking incorrect sequence highlights wrong words`() {
        val viewModel = WordScrambleViewModel(defaultArgs)
        
        // Find words
        val word0 = viewModel.state.value.availableWords.first { it.id == 0 }
        val word1 = viewModel.state.value.availableWords.first { it.id == 1 }
        val word2 = viewModel.state.value.availableWords.first { it.id == 2 }
        
        // Place in WRONG order (1, 0, 2 instead of 0, 1, 2)
        viewModel.onEvent(WordScrambleEvent.PlaceWord(word1))
        viewModel.onEvent(WordScrambleEvent.PlaceWord(word0))
        viewModel.onEvent(WordScrambleEvent.PlaceWord(word2))
        
        viewModel.onEvent(WordScrambleEvent.Check)
        
        val state = viewModel.state.value
        val phase = state.phase
        assertTrue(phase is WordScrambleUiState.Phase.Result)
        assertFalse((phase as WordScrambleUiState.Phase.Result).correct)
        assertTrue(state.wrongWords.contains(word1))
        assertTrue(state.wrongWords.contains(word0))
    }

    @Test
    fun `reset restores initial state`() {
        val viewModel = WordScrambleViewModel(defaultArgs)
        val word = viewModel.state.value.availableWords.first()
        
        viewModel.onEvent(WordScrambleEvent.PlaceWord(word))
        viewModel.onEvent(WordScrambleEvent.SelectForReorder(word))
        viewModel.onEvent(WordScrambleEvent.Check)
        
        viewModel.onEvent(WordScrambleEvent.Reset)
        
        val state = viewModel.state.value
        assertEquals(3, state.availableWords.size)
        assertTrue(state.placedWords.isEmpty())
        assertTrue(state.wrongWords.isEmpty())
        assertEquals(null, state.selectedWordForReorder)
        assertEquals(WordScrambleUiState.Phase.Answering, state.phase)
    }

    @Test
    fun `showing and dismissing hint updates phase`() {
        val viewModel = WordScrambleViewModel(defaultArgs)
        
        viewModel.onEvent(WordScrambleEvent.ShowHint)
        assertEquals(WordScrambleUiState.Phase.ShowingHintImage, viewModel.state.value.phase)
        
        viewModel.onEvent(WordScrambleEvent.DismissHint)
        assertEquals(WordScrambleUiState.Phase.Answering, viewModel.state.value.phase)
    }
}
