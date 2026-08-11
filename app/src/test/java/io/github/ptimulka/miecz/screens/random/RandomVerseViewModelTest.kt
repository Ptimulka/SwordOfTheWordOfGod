package io.github.ptimulka.miecz.screens.random

import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.data.VerseGroup
import io.github.ptimulka.miecz.helpers.MainDispatcherRule
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class RandomVerseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: VersesGroupsRepository = mock()
    private val verse = Verse("Book", 1, "1", "Text")
    private val group = VerseGroup(1, "Name", listOf(verse))

    @Test
    fun `initialization draws a random verse but does not start countdown immediately`() = runTest {
        whenever(repository.loadVerseGroups()).thenReturn(listOf(group))
        
        val viewModel = RandomVerseViewModel(repository, "Random")
        
        assertEquals(verse, viewModel.state.value.randomVerse)
        assertFalse(viewModel.state.value.isCountingDown)
    }

    @Test
    fun `enterScreen with newTabEntry resets and starts countdown`() = runTest {
        whenever(repository.loadVerseGroups()).thenReturn(listOf(group))
        val viewModel = RandomVerseViewModel(repository, "Random")
        
        viewModel.onEvent(RandomVerseEvent.EnterScreen(isNewTabEntry = true))
        
        assertTrue(viewModel.state.value.isCountingDown)
        assertEquals(10, viewModel.state.value.countdown)
    }

    @Test
    fun `countdown reaches zero and triggers effect`() = runTest {
        whenever(repository.loadVerseGroups()).thenReturn(listOf(group))
        val viewModel = RandomVerseViewModel(repository, "Random")
        viewModel.onEvent(RandomVerseEvent.EnterScreen(isNewTabEntry = true))
        
        advanceTimeBy(11000)
        
        assertFalse(viewModel.state.value.isCountingDown)
        assertEquals(0, viewModel.state.value.countdown)
    }

    @Test
    fun `startNow immediately finishes countdown`() = runTest {
        whenever(repository.loadVerseGroups()).thenReturn(listOf(group))
        val viewModel = RandomVerseViewModel(repository, "Random")
        viewModel.onEvent(RandomVerseEvent.EnterScreen(isNewTabEntry = true))
        
        viewModel.onEvent(RandomVerseEvent.StartNow)
        
        assertFalse(viewModel.state.value.isCountingDown)
        assertEquals(0, viewModel.state.value.countdown)
    }

    @Test
    fun `drawAnother starts countdown even if not triggered by tab entry`() = runTest {
        whenever(repository.loadVerseGroups()).thenReturn(listOf(group))
        val viewModel = RandomVerseViewModel(repository, "Random")
        
        viewModel.onEvent(RandomVerseEvent.DrawAnother)
        
        assertEquals(10, viewModel.state.value.countdown)
        assertTrue(viewModel.state.value.isCountingDown)
    }

    @Test
    fun `leaveScreen pauses countdown`() = runTest {
        whenever(repository.loadVerseGroups()).thenReturn(listOf(group))
        val viewModel = RandomVerseViewModel(repository, "Random")
        viewModel.onEvent(RandomVerseEvent.EnterScreen(isNewTabEntry = true))
        
        advanceTimeBy(3100)
        assertEquals(7, viewModel.state.value.countdown)
        
        viewModel.onEvent(RandomVerseEvent.LeaveScreen)
        
        advanceTimeBy(5000)
        // Should still be 7 because job was cancelled
        assertEquals(7, viewModel.state.value.countdown)
    }
}
