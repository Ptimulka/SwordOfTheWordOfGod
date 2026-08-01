package io.github.ptimulka.miecz.screens.riddles

import io.github.ptimulka.miecz.screens.riddles.fill_whole_sigla.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FillWholeSiglaViewModelTest {

    private val defaultArgs = FillWholeSiglaArgs(
        book = "Rdz",
        chapter = 1,
        number = "1-3",
        hasHint = false
    )

    @Test
    fun `checking correct combination updates phase to success`() {
        val viewModel = FillWholeSiglaViewModel(defaultArgs)
        
        viewModel.onEvent(FillWholeSiglaEvent.UpdateBook("Rodzaju"))
        viewModel.onEvent(FillWholeSiglaEvent.UpdateChapter("1"))
        viewModel.onEvent(FillWholeSiglaEvent.UpdateVerse("2"))
        viewModel.onEvent(FillWholeSiglaEvent.Check)
        
        val phase = viewModel.state.value.phase
        assertTrue(phase is FillWholeSiglaUiState.Phase.Result)
        assertTrue((phase as FillWholeSiglaUiState.Phase.Result).correct)
    }

    @Test
    fun `incorrect book name highlights error`() {
        val viewModel = FillWholeSiglaViewModel(defaultArgs)
        
        viewModel.onEvent(FillWholeSiglaEvent.UpdateBook("Wrong"))
        viewModel.onEvent(FillWholeSiglaEvent.UpdateChapter("1"))
        viewModel.onEvent(FillWholeSiglaEvent.UpdateVerse("1"))
        viewModel.onEvent(FillWholeSiglaEvent.Check)
        
        val state = viewModel.state.value
        assertFalse((state.phase as FillWholeSiglaUiState.Phase.Result).correct)
        assertTrue(state.wrongIndices.contains(0))
        assertFalse(state.wrongIndices.contains(1))
        assertFalse(state.wrongIndices.contains(2))
    }

    @Test
    fun `incorrect verse number highlights error`() {
        val viewModel = FillWholeSiglaViewModel(defaultArgs)
        
        viewModel.onEvent(FillWholeSiglaEvent.UpdateBook("Rdz"))
        viewModel.onEvent(FillWholeSiglaEvent.UpdateChapter("1"))
        viewModel.onEvent(FillWholeSiglaEvent.UpdateVerse("5")) // Not in 1-3
        viewModel.onEvent(FillWholeSiglaEvent.Check)
        
        val state = viewModel.state.value
        assertTrue(state.wrongIndices.contains(2))
    }

    @Test
    fun `correct answer with hint triggers image reward`() {
        val viewModel = FillWholeSiglaViewModel(defaultArgs.copy(hasHint = true))
        
        viewModel.onEvent(FillWholeSiglaEvent.UpdateBook("Rdz"))
        viewModel.onEvent(FillWholeSiglaEvent.UpdateChapter("1"))
        viewModel.onEvent(FillWholeSiglaEvent.UpdateVerse("1"))
        viewModel.onEvent(FillWholeSiglaEvent.Check)
        
        assertEquals(FillWholeSiglaUiState.Phase.ShowingImageReward, viewModel.state.value.phase)
    }
}
