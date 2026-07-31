package io.github.ptimulka.miecz.screens.riddles

import io.github.ptimulka.miecz.screens.riddles.multi_quiz.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiQuizViewModelTest {

    private val defaultArgs = MultiQuizArgs(
        book = "Rdz",
        chapter = 1,
        number = "1",
        hasHint = false
    )

    @Test
    fun `initial state has answers and empty selections`() {
        val viewModel = MultiQuizViewModel(defaultArgs)
        val state = viewModel.state.value
        
        assertEquals(4, state.bookAnswers.size)
        assertNull(state.selectedBook)
        assertFalse(state.checkEnabled)
    }

    @Test
    fun `selecting options updates state and enables check`() {
        val viewModel = MultiQuizViewModel(defaultArgs)
        viewModel.onEvent(MultiQuizEvent.SelectBook("Rdz"))
        viewModel.onEvent(MultiQuizEvent.SelectChapter("1"))
        viewModel.onEvent(MultiQuizEvent.SelectVerse("1"))
        
        val state = viewModel.state.value
        assertEquals("Rdz", state.selectedBook)
        assertEquals("1", state.selectedChapter)
        assertEquals("1", state.selectedVerse)
        assertTrue(state.checkEnabled)
    }

    @Test
    fun `checking correct answers updates phase to result success`() {
        val viewModel = MultiQuizViewModel(defaultArgs)
        viewModel.onEvent(MultiQuizEvent.SelectBook("Rdz"))
        viewModel.onEvent(MultiQuizEvent.SelectChapter("1"))
        viewModel.onEvent(MultiQuizEvent.SelectVerse("1"))
        viewModel.onEvent(MultiQuizEvent.Check)
        
        val phase = viewModel.state.value.phase
        assertTrue(phase is MultiQuizUiState.Phase.Result)
        assertTrue((phase as MultiQuizUiState.Phase.Result).correct)
    }

    @Test
    fun `checking wrong answers updates phase to result failure and highlights errors`() {
        val viewModel = MultiQuizViewModel(defaultArgs)
        viewModel.onEvent(MultiQuizEvent.SelectBook("Wrong"))
        viewModel.onEvent(MultiQuizEvent.SelectChapter("1"))
        viewModel.onEvent(MultiQuizEvent.SelectVerse("1"))
        viewModel.onEvent(MultiQuizEvent.Check)
        
        val state = viewModel.state.value
        assertTrue(state.phase is MultiQuizUiState.Phase.Result)
        assertFalse((state.phase as MultiQuizUiState.Phase.Result).correct)
        assertEquals("Wrong", state.wrongBook)
        assertNull(state.wrongChapter)
        assertNull(state.wrongVerse)
    }
}
