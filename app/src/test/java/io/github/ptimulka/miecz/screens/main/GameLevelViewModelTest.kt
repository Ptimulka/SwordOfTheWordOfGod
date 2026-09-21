package io.github.ptimulka.miecz.screens.main

import io.github.ptimulka.miecz.helpers.MainDispatcherRule
import io.github.ptimulka.miecz.repositories.ProgressRepository
import io.github.ptimulka.miecz.repositories.RiddlesOrderRepository
import io.github.ptimulka.miecz.repositories.SectionRepository
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.main.LevelButtonState
import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.data.Section
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GameLevelViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val progressRepo: ProgressRepository = mock()
    private val sectionRepo: SectionRepository = mock()
    private val groupsRepo: VersesGroupsRepository = mock()
    private val riddlesOrderRepo: RiddlesOrderRepository = mock()

    @Before
    fun setup() = runTest {
        whenever(sectionRepo.loadInitialSections()).thenReturn(emptyList())
        whenever(groupsRepo.loadVerseGroups()).thenReturn(emptyList())
        whenever(riddlesOrderRepo.getRiddlesOrder()).thenReturn(emptyList())
        
        // Default mocks for refreshProgress
        whenever(progressRepo.getAllUsedGroupIds()).thenReturn(emptySet())
        whenever(progressRepo.getCurrentSection()).thenReturn(1)
        whenever(progressRepo.getShieldsCount()).thenReturn(5)
        whenever(progressRepo.getCurrentDayStreak()).thenReturn(0)
        whenever(progressRepo.hasPlayedToday()).thenReturn(false)
        whenever(progressRepo.getTimeToNextShield()).thenReturn(0L)
        whenever(progressRepo.retentionContributionForRepeats(any(), any())).thenReturn(0)
    }

    @Test
    fun `onResume triggers daily decay and refreshes data`() = runTest {
        val viewModel = GameLevelViewModel(progressRepo, sectionRepo, groupsRepo, riddlesOrderRepo, false)
        
        viewModel.onEvent(GameLevelEvent.OnResume)
        
        verify(progressRepo).applyDailyRetentionDecay()
    }

    @Test
    fun `toggleShieldInfo toggles state`() = runTest {
        val viewModel = GameLevelViewModel(progressRepo, sectionRepo, groupsRepo, riddlesOrderRepo, false)
        
        assertFalse(viewModel.state.value.isShieldInfoVisible)
        viewModel.onEvent(GameLevelEvent.ToggleShieldInfo)
        assertTrue(viewModel.state.value.isShieldInfoVisible)
    }

    @Test
    fun `dismissUnlockedDialog clears unlocked section id`() = runTest {
        val viewModel = GameLevelViewModel(progressRepo, sectionRepo, groupsRepo, riddlesOrderRepo, false)
        
        viewModel.onEvent(GameLevelEvent.DismissUnlockedDialog)
        assertEquals(-1, viewModel.state.value.progress.unlockedSectionId)
    }

    @Test
    fun `navigation to choose groups updates state`() = runTest {
        val viewModel = GameLevelViewModel(progressRepo, sectionRepo, groupsRepo, riddlesOrderRepo, false)
        
        viewModel.onEvent(GameLevelEvent.NavigateToChooseGroups)
        assertTrue(viewModel.state.value.showChooseVerseGroups)
    }

    @Test
    fun `confirmGroupSelection saves section and refreshes`() = runTest {
        val viewModel = GameLevelViewModel(progressRepo, sectionRepo, groupsRepo, riddlesOrderRepo, false)
        
        // Mock selection of 2 groups
        viewModel.onEvent(GameLevelEvent.ToggleGroupSelection(1))
        viewModel.onEvent(GameLevelEvent.ToggleGroupSelection(2))
        
        viewModel.onEvent(GameLevelEvent.ConfirmGroupSelection)
        
        verify(progressRepo).saveCustomSection(any(), any(), any())
        assertFalse(viewModel.state.value.showChooseVerseGroups)
    }

    @Test
    fun `showSectionVerses updates state`() = runTest {
        val viewModel = GameLevelViewModel(progressRepo, sectionRepo, groupsRepo, riddlesOrderRepo, false)
        val section = mock<Section>()
        
        viewModel.onEvent(GameLevelEvent.ShowSectionVerses(section))
        assertEquals(section, viewModel.state.value.selectedSectionForDialog)
    }

    @Test
    fun `toggleGroupSelection limits selection to 2`() = runTest {
        val viewModel = GameLevelViewModel(progressRepo, sectionRepo, groupsRepo, riddlesOrderRepo, false)
        
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
    fun `refreshProgress calculates level states correctly`() = runTest {
        val section = Section(1, "Test", emptyList())
        whenever(sectionRepo.loadInitialSections()).thenReturn(listOf(section))
        whenever(progressRepo.getCurrentSection()).thenReturn(1)
        
        // Mock riddles order: 1 level with 1 riddle
        whenever(riddlesOrderRepo.getRiddlesOrder()).thenReturn(listOf(listOf(RiddleType.QUIZ_NORMAL)))
        
        // Initial state: Level 1 not finished, 0% retention
        whenever(progressRepo.isLevelFinished(1, 1)).thenReturn(false)
        whenever(progressRepo.getRetention(1)).thenReturn(0)
        
        val viewModel = GameLevelViewModel(progressRepo, sectionRepo, groupsRepo, riddlesOrderRepo, false)
        
        val sectionState = viewModel.state.value.sectionStates[1]!!
        val level1 = sectionState.levels[0]
        
        // Previous levels (none) finished, but 0% retention -> HALF unlocked (progression yes, rays no)
        assertEquals(LevelButtonState.HALF, level1.state)
        assertEquals(R.string.level_locked_need_retention, level1.lockMessageRes)
    }

    @Test
    fun `level is HALF if retention is high but previous level is not finished`() = runTest {
        val section = Section(1, "Test", emptyList())
        whenever(sectionRepo.loadInitialSections()).thenReturn(listOf(section))
        whenever(progressRepo.getCurrentSection()).thenReturn(1)
        
        // Mock riddles order: 3 levels
        whenever(riddlesOrderRepo.getRiddlesOrder()).thenReturn(listOf(
            listOf(RiddleType.QUIZ_NORMAL),
            listOf(RiddleType.QUIZ_NORMAL),
            listOf(RiddleType.QUIZ_NORMAL)
        ))
        
        // Level 1 finished, Level 2 NOT finished
        whenever(progressRepo.isLevelFinished(1, 1)).thenReturn(true)
        whenever(progressRepo.isLevelFinished(1, 2)).thenReturn(false)
        whenever(progressRepo.isLevelFinished(1, 3)).thenReturn(false)
        
        // High retention (100% -> rays reach 12)
        whenever(progressRepo.getRetention(1)).thenReturn(100)
        
        val viewModel = GameLevelViewModel(progressRepo, sectionRepo, groupsRepo, riddlesOrderRepo, false)
        
        val sectionState = viewModel.state.value.sectionStates[1]!!
        val level3 = sectionState.levels[2]
        
        // Level 2 not finished -> progression no. Retention high -> rays yes. -> HALF
        assertEquals(LevelButtonState.HALF, level3.state)
        assertEquals(R.string.level_locked_need_previous, level3.lockMessageRes)
    }
}
