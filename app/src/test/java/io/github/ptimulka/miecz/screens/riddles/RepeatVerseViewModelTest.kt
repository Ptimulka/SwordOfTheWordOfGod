package io.github.ptimulka.miecz.screens.riddles

import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.MainDispatcherRule
import io.github.ptimulka.miecz.repositories.ProgressRepository
import io.github.ptimulka.miecz.screens.riddles.repeat_verse.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class RepeatVerseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val progressRepo: ProgressRepository = mock()
    private val verses = listOf(
        Verse("Rdz", 1, "1", "Na początku Bóg stworzył niebo i ziemię.")
    )
    private val defaultArgs = RepeatVerseArgs(1, verses)

    @Test
    fun `selecting a verse updates state with current repeat count`() {
        whenever(progressRepo.getVerseRepeatCountToday(1, 0)).thenReturn(3)
        val viewModel = RepeatVerseViewModel(defaultArgs, progressRepo)
        
        viewModel.onEvent(RepeatVerseEvent.SelectVerse(0))
        
        val state = viewModel.state.value
        assertEquals(0, state.selectedIndex)
        assertEquals(3, state.repeatCount)
    }

    @Test
    fun `processing valid speech result increments repeat count`() {
        whenever(progressRepo.getVerseRepeatCountToday(1, 0)).thenReturn(0)
        whenever(progressRepo.incrementVerseRepeatToday(1, 0)).thenReturn(1)
        val viewModel = RepeatVerseViewModel(defaultArgs, progressRepo)
        
        viewModel.onEvent(RepeatVerseEvent.SelectVerse(0))
        viewModel.onEvent(RepeatVerseEvent.ProcessResult("Na poczatku Bog stworzyl niebo i ziemie"))
        
        val state = viewModel.state.value
        assertEquals(1, state.repeatCount)
        assertTrue(state.lastSimilarity >= 50f)
    }

    @Test
    fun `processing invalid speech result does not increment count`() {
        whenever(progressRepo.getVerseRepeatCountToday(1, 0)).thenReturn(0)
        val viewModel = RepeatVerseViewModel(defaultArgs, progressRepo)
        
        viewModel.onEvent(RepeatVerseEvent.SelectVerse(0))
        viewModel.onEvent(RepeatVerseEvent.ProcessResult("Zupelnie inny tekst"))
        
        val state = viewModel.state.value
        assertEquals(0, state.repeatCount)
        assertTrue(state.lastSimilarity < 50f)
    }
}
