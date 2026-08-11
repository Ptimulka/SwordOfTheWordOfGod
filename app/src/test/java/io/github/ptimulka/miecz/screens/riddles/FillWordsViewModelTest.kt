package io.github.ptimulka.miecz.screens.riddles

import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase
import io.github.ptimulka.miecz.screens.riddles.fill_words.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FillWordsViewModelTest {

    private val defaultArgs = FillWordsArgs(
        verseText = "Na początku Bóg stworzył niebo i ziemię.",
        book = "Rdz",
        chapter = 1,
        number = "1",
        isEasy = true,
        moreWords = false
    )

    @Test
    fun `initial state has correct parts and empty inputs`() {
        val viewModel = FillWordsViewModel(defaultArgs)
        val state = viewModel.state.value
        
        val fillableCount = state.verseParts.count { it is VersePart.WordToFill }
        assertEquals(fillableCount, state.userInputs.size)
        assertTrue(state.userInputs.all { it == "" })
    }

    @Test
    fun `updating input updates state and clears error`() {
        val viewModel = FillWordsViewModel(defaultArgs)
        viewModel.onEvent(FillWordsEvent.UpdateInput(0, "Test"))
        
        assertEquals("Test", viewModel.state.value.userInputs[0])
        assertFalse(viewModel.state.value.wrongInputIndices.contains(0))
    }

    @Test
    fun `checking correct answers updates phase to result success`() {
        val viewModel = FillWordsViewModel(defaultArgs)
        val state = viewModel.state.value
        val fillableParts = state.verseParts.filterIsInstance<VersePart.WordToFill>()
        
        fillableParts.forEachIndexed { i, part ->
            viewModel.onEvent(FillWordsEvent.UpdateInput(i, part.correctWord))
        }
        
        viewModel.onEvent(FillWordsEvent.Check)
        
        val phase = viewModel.state.value.phase
        assertTrue(phase is RiddlePhase.Result)
        assertTrue((phase as RiddlePhase.Result).correct)
    }

    @Test
    fun `checking wrong answers updates phase to result failure and marks indices`() {
        val viewModel = FillWordsViewModel(defaultArgs)
        viewModel.onEvent(FillWordsEvent.UpdateInput(0, "Wrong"))
        viewModel.onEvent(FillWordsEvent.Check)
        
        val phase = viewModel.state.value.phase
        assertTrue(phase is RiddlePhase.Result)
        assertFalse((phase as RiddlePhase.Result).correct)
        assertTrue(viewModel.state.value.wrongInputIndices.contains(0))
    }
}
