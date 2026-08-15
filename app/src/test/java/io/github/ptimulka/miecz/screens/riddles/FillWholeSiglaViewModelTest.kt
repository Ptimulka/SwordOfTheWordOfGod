package io.github.ptimulka.miecz.screens.riddles

import io.github.ptimulka.miecz.helpers.MainDispatcherRule
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase
import io.github.ptimulka.miecz.screens.riddles.fill_whole_sigla.FillWholeSiglaArgs
import io.github.ptimulka.miecz.screens.riddles.fill_whole_sigla.FillWholeSiglaEvent
import io.github.ptimulka.miecz.screens.riddles.fill_whole_sigla.FillWholeSiglaViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

class FillWholeSiglaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mnemonicRepo: MnemonicRepository = mock()
    private val args = FillWholeSiglaArgs(book = "Rdz", chapter = 1, number = "1", hasHint = false)

    @Test
    fun `checking correct whole sigla updates phase to result success`() {
        val viewModel = FillWholeSiglaViewModel(args, mnemonicRepo, mainDispatcherRule.testDispatcher)
        
        viewModel.onEvent(FillWholeSiglaEvent.UpdateBook("Rodzaju"))
        viewModel.onEvent(FillWholeSiglaEvent.UpdateChapter("1"))
        viewModel.onEvent(FillWholeSiglaEvent.UpdateVerse("1"))
        viewModel.onEvent(FillWholeSiglaEvent.Check)
        
        val phase = viewModel.state.value.phase
        assertTrue(phase is RiddlePhase.Result)
        assertTrue((phase as RiddlePhase.Result).correct)
    }

    @Test
    fun `checking partially wrong sigla highlights errors`() {
        val viewModel = FillWholeSiglaViewModel(args, mnemonicRepo, mainDispatcherRule.testDispatcher)
        
        viewModel.onEvent(FillWholeSiglaEvent.UpdateBook("Rodzaju"))
        viewModel.onEvent(FillWholeSiglaEvent.UpdateChapter("2")) // WRONG
        viewModel.onEvent(FillWholeSiglaEvent.UpdateVerse("1"))
        viewModel.onEvent(FillWholeSiglaEvent.Check)
        
        val state = viewModel.state.value
        assertTrue(!(state.phase as RiddlePhase.Result).correct)
        assertEquals(setOf(1), state.wrongIndices)
    }

    @Test
    fun `dismissResult resets to answering phase`() {
        val viewModel = FillWholeSiglaViewModel(args, mnemonicRepo, mainDispatcherRule.testDispatcher)
        viewModel.onEvent(FillWholeSiglaEvent.UpdateBook("X"))
        viewModel.onEvent(FillWholeSiglaEvent.Check)
        viewModel.onEvent(FillWholeSiglaEvent.DismissResult)
        
        assertEquals(RiddlePhase.Answering, viewModel.state.value.phase)
    }
}
