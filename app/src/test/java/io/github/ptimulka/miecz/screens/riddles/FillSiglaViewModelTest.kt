package io.github.ptimulka.miecz.screens.riddles

import io.github.ptimulka.miecz.screens.riddles.fill_sigla.FillSiglaArgs
import io.github.ptimulka.miecz.screens.riddles.fill_sigla.FillSiglaEvent
import io.github.ptimulka.miecz.screens.riddles.fill_sigla.FillSiglaType
import io.github.ptimulka.miecz.screens.riddles.fill_sigla.FillSiglaUiState
import io.github.ptimulka.miecz.screens.riddles.fill_sigla.FillSiglaViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FillSiglaViewModelTest {

    @Test
    fun `checking correct book name updates phase to result success`() {
        val args = FillSiglaArgs(book = "Rdz", chapter = 1, number = "1", fillType = FillSiglaType.BOOK, hasHint = false)
        val viewModel = FillSiglaViewModel(args)
        
        viewModel.onEvent(FillSiglaEvent.UpdateInput("Rodzaju")) // Should normalize to Rdz
        viewModel.onEvent(FillSiglaEvent.Check)
        
        val phase = viewModel.state.value.phase
        assertTrue(phase is FillSiglaUiState.Phase.Result)
        assertTrue((phase as FillSiglaUiState.Phase.Result).correct)
    }

    @Test
    fun `checking correct chapter updates phase to result success`() {
        val args = FillSiglaArgs(book = "Rdz", chapter = 5, number = "1", fillType = FillSiglaType.CHAPTER, hasHint = false)
        val viewModel = FillSiglaViewModel(args)
        
        viewModel.onEvent(FillSiglaEvent.UpdateInput("5"))
        viewModel.onEvent(FillSiglaEvent.Check)
        
        assertTrue((viewModel.state.value.phase as FillSiglaUiState.Phase.Result).correct)
    }

    @Test
    fun `checking correct verse range updates phase to result success`() {
        val args = FillSiglaArgs(book = "Rdz", chapter = 1, number = "1-3", fillType = FillSiglaType.VERSE, hasHint = false)
        val viewModel = FillSiglaViewModel(args)
        
        viewModel.onEvent(FillSiglaEvent.UpdateInput("2")) // Within 1-3
        viewModel.onEvent(FillSiglaEvent.Check)
        
        assertTrue((viewModel.state.value.phase as FillSiglaUiState.Phase.Result).correct)
    }

    @Test
    fun `checking wrong input updates phase to result failure`() {
        val args = FillSiglaArgs(book = "Rdz", chapter = 1, number = "1", fillType = FillSiglaType.CHAPTER, hasHint = false)
        val viewModel = FillSiglaViewModel(args)
        
        viewModel.onEvent(FillSiglaEvent.UpdateInput("2"))
        viewModel.onEvent(FillSiglaEvent.Check)
        
        val phase = viewModel.state.value.phase
        assertTrue(phase is FillSiglaUiState.Phase.Result)
        assertFalse((phase as FillSiglaUiState.Phase.Result).correct)
    }
}
