package io.github.ptimulka.miecz.screens.mnemonic

import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.MainDispatcherRule
import io.github.ptimulka.miecz.repositories.ChosenPicture
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MnemonicViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: MnemonicRepository = mock()
    private val verse = Verse("Book", 1, "1", "Text")
    private val verses = listOf(verse)
    private val assets = listOf("asset.webp")

    @Test
    fun `initial load fetches verse data`() = runTest {
        whenever(repository.loadChoice(1, 0)).thenReturn(ChosenPicture.DEFAULT)
        
        val viewModel = MnemonicViewModel(1, "Section", verses, assets, repository, mainDispatcherRule.testDispatcher)
        
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertEquals(1, state.verseData.size)
        assertEquals(ChosenPicture.DEFAULT, state.verseData[0].chosen)
    }

    @Test
    fun `navigateToDrawing sets correct state`() = runTest {
        whenever(repository.loadChoice(1, 0)).thenReturn(ChosenPicture.NONE)
        val viewModel = MnemonicViewModel(1, "Section", verses, assets, repository, mainDispatcherRule.testDispatcher)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(MnemonicEvent.EnterDrawing(0))
        
        assertEquals(MnemonicScreen.DRAWING, viewModel.state.value.currentScreen)
        assertEquals(0, viewModel.state.value.drawingState.verseIndex)
        assertFalse(viewModel.state.value.drawingState.isDirty)
    }

    @Test
    fun `addStroke marks state as dirty`() = runTest {
        whenever(repository.loadChoice(1, 0)).thenReturn(ChosenPicture.NONE)
        val viewModel = MnemonicViewModel(1, "Section", verses, assets, repository, mainDispatcherRule.testDispatcher)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onEvent(MnemonicEvent.EnterDrawing(0))
        viewModel.onEvent(MnemonicEvent.AddStroke(emptyList()))
        
        assertTrue(viewModel.state.value.drawingState.isDirty)
        assertEquals(1, viewModel.state.value.drawingState.strokes.size)
    }

    @Test
    fun `selectChoice saves to repository and updates state`() = runTest {
        whenever(repository.loadChoice(1, 0)).thenReturn(ChosenPicture.NONE)
        val viewModel = MnemonicViewModel(1, "Section", verses, assets, repository, mainDispatcherRule.testDispatcher)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onEvent(MnemonicEvent.SelectChoice(0, ChosenPicture.USER))
        
        verify(repository).saveChoice(1, 0, ChosenPicture.USER)
        assertEquals(ChosenPicture.USER, viewModel.state.value.verseData[0].chosen)
    }
}
