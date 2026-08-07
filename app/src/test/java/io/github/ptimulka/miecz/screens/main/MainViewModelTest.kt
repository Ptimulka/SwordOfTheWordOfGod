package io.github.ptimulka.miecz.screens.main

import io.github.ptimulka.miecz.repositories.ProgressRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MainViewModelTest {

    private val progressRepo: ProgressRepository = mock()

    @Test
    fun `initialization sets review tab visibility based on progress`() {
        whenever(progressRepo.getCurrentSection()).thenReturn(3)
        val viewModel = MainViewModel(progressRepo)
        assertTrue(viewModel.state.value.isReviewTabVisible)
    }

    @Test
    fun `review tab is hidden if progress is less than section 3`() {
        whenever(progressRepo.getCurrentSection()).thenReturn(2)
        val viewModel = MainViewModel(progressRepo)
        assertFalse(viewModel.state.value.isReviewTabVisible)
    }

    @Test
    fun `selectScreen updates selected tab`() {
        val viewModel = MainViewModel(progressRepo)
        assertEquals(Screen.Levels, viewModel.state.value.selectedScreen)
        
        viewModel.onEvent(MainEvent.SelectScreen(Screen.Settings))
        assertEquals(Screen.Settings, viewModel.state.value.selectedScreen)
    }

    @Test
    fun `refreshTabVisibility re-evaluates progress`() {
        whenever(progressRepo.getCurrentSection()).thenReturn(2)
        val viewModel = MainViewModel(progressRepo)
        assertFalse(viewModel.state.value.isReviewTabVisible)
        
        whenever(progressRepo.getCurrentSection()).thenReturn(3)
        viewModel.onEvent(MainEvent.RefreshTabVisibility)
        assertTrue(viewModel.state.value.isReviewTabVisible)
    }
}
