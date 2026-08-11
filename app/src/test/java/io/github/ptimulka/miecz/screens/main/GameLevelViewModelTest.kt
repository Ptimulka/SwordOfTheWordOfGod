package io.github.ptimulka.miecz.screens.main

import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.helpers.MainDispatcherRule
import io.github.ptimulka.miecz.repositories.ProgressRepository
import io.github.ptimulka.miecz.repositories.RiddlesOrderRepository
import io.github.ptimulka.miecz.repositories.SectionRepository
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GameLevelViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val progressRepo: ProgressRepository = mock()
    private val sectionRepo: SectionRepository = mock()
    private val groupsRepo: VersesGroupsRepository = mock()
    private val riddlesOrderRepo: RiddlesOrderRepository = mock()

    @Test
    fun `initial load fetches sections and progress`() {
        whenever(sectionRepo.loadInitialSections()).thenReturn(emptyList())
        whenever(progressRepo.getShieldsCount()).thenReturn(3)
        whenever(progressRepo.getCurrentSection()).thenReturn(2)
        
        val viewModel = GameLevelViewModel(progressRepo, sectionRepo, groupsRepo, riddlesOrderRepo)
        
        val state = viewModel.state.value
        assertEquals(3, state.progress.shieldsCount)
        assertEquals(2, state.progress.currentSectionId)
    }

    @Test
    fun `toggling shield info updates visibility`() {
        whenever(sectionRepo.loadInitialSections()).thenReturn(emptyList())
        val viewModel = GameLevelViewModel(progressRepo, sectionRepo, groupsRepo, riddlesOrderRepo)
        
        assertFalse(viewModel.state.value.isShieldInfoVisible)
        
        viewModel.onEvent(GameLevelEvent.ToggleShieldInfo)
        assertTrue(viewModel.state.value.isShieldInfoVisible)
        
        viewModel.onEvent(GameLevelEvent.ToggleShieldInfo)
        assertFalse(viewModel.state.value.isShieldInfoVisible)
    }

    @Test
    fun `onResume applies decay and refreshes data`() {
        whenever(sectionRepo.loadInitialSections()).thenReturn(emptyList())
        val viewModel = GameLevelViewModel(progressRepo, sectionRepo, groupsRepo, riddlesOrderRepo)
        
        viewModel.onEvent(GameLevelEvent.OnResume)
        
        verify(progressRepo).applyDailyRetentionDecay()
    }

    @Test
    fun `toggleGroupSelection limits selection to 2`() {
        whenever(sectionRepo.loadInitialSections()).thenReturn(emptyList())
        val viewModel = GameLevelViewModel(progressRepo, sectionRepo, groupsRepo, riddlesOrderRepo)
        
        viewModel.onEvent(GameLevelEvent.ToggleGroupSelection(1))
        viewModel.onEvent(GameLevelEvent.ToggleGroupSelection(2))
        viewModel.onEvent(GameLevelEvent.ToggleGroupSelection(3)) // Should be ignored
        
        val selection = viewModel.state.value.selectedGroupIds
        assertEquals(2, selection.size)
        assertTrue(selection.contains(1))
        assertTrue(selection.contains(2))
        assertFalse(selection.contains(3))
    }

    @Test
    fun `confirmGroupSelection saves and refreshes`() {
        whenever(sectionRepo.loadInitialSections()).thenReturn(emptyList())
        val viewModel = GameLevelViewModel(progressRepo, sectionRepo, groupsRepo, riddlesOrderRepo)
        
        viewModel.onEvent(GameLevelEvent.ToggleGroupSelection(10))
        viewModel.onEvent(GameLevelEvent.ToggleGroupSelection(20))
        viewModel.onEvent(GameLevelEvent.ConfirmGroupSelection)
        
        verify(progressRepo).saveCustomSection(org.mockito.kotlin.any(), org.mockito.kotlin.eq(10), org.mockito.kotlin.eq(20))
        assertFalse(viewModel.state.value.showChooseVerseGroups)
        assertTrue(viewModel.state.value.selectedGroupIds.isEmpty())
    }

    @Test
    fun `refreshProgress calculates level states correctly`() {
        val section = io.github.ptimulka.miecz.data.Section(1, "Test", emptyList())
        whenever(sectionRepo.loadInitialSections()).thenReturn(listOf(section))
        whenever(progressRepo.getCurrentSection()).thenReturn(1)
        
        // Mock riddles order: 1 level with 1 riddle
        whenever(riddlesOrderRepo.getRiddlesOrder()).thenReturn(listOf(listOf(io.github.ptimulka.miecz.data.RiddleType.QUIZ_NORMAL)))
        
        // Initial state: Level 1 not finished, 0% retention
        whenever(progressRepo.isLevelFinished(1, 1)).thenReturn(false)
        whenever(progressRepo.getRetention(1)).thenReturn(0)
        
        val viewModel = GameLevelViewModel(progressRepo, sectionRepo, groupsRepo, riddlesOrderRepo)
        
        val sectionState = viewModel.state.value.sectionStates[1]!!
        val level1 = sectionState.levels[0]
        
        // Previous levels (none) finished, but 0% retention -> HALF unlocked (progression yes, rays no)
        assertEquals(io.github.ptimulka.miecz.components.main.LevelButtonState.HALF, level1.state)
        assertEquals(R.string.level_locked_need_retention, level1.lockMessageRes)
    }
}
