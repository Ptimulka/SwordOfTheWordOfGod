package io.github.ptimulka.miecz.screens.game

import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val progressRepo: io.github.ptimulka.miecz.repositories.ProgressRepository = mock()
    private val verse = Verse("Jan", 3, "16", "Text")
    private val riddleTypes = listOf(RiddleType.QUIZ_NORMAL.name)

    @Test
    fun `initialization shuffles riddles and loads shields`() = runTest {
        whenever(progressRepo.getShieldsCount()).thenReturn(3)
        
        val viewModel = GameViewModel(1, "Section", 1, listOf(verse), emptyList(), riddleTypes, progressRepo, false)
        
        val state = viewModel.state.value
        assertEquals(1, state.riddles.size)
        assertEquals(3, state.shieldsCount)
    }

    @Test
    fun `onRiddleSuccess increments index or completes level`() = runTest {
        val manyRiddles = listOf(RiddleType.QUIZ_NORMAL.name, RiddleType.QUIZ_EASY.name)
        val viewModel = GameViewModel(1, "Section", 1, listOf(verse, verse), emptyList(), manyRiddles, progressRepo, false)
        
        assertEquals(0, viewModel.state.value.currentIndex)
        
        viewModel.onEvent(GameEvent.OnRiddleSuccess())
        assertEquals(1, viewModel.state.value.currentIndex)
        
        viewModel.onEvent(GameEvent.OnRiddleSuccess())
        // Should trigger dialog or finish
        assertFalse(viewModel.state.value.dialogState is GameDialogState.None)
    }

    @Test
    fun `onShieldLoss decrements shields and shows dialog at zero`() = runTest {
        whenever(progressRepo.getShieldsCount()).thenReturn(1)
        val viewModel = GameViewModel(1, "Section", 1, listOf(verse), emptyList(), riddleTypes, progressRepo, false)
        
        viewModel.onEvent(GameEvent.OnShieldLoss)
        
        // Mock decreaseShields return
        whenever(progressRepo.getShieldsCount()).thenReturn(0)
        viewModel.onEvent(GameEvent.OnShieldLoss)
        
        assertEquals(0, viewModel.state.value.shieldsCount)
        assertEquals(GameDialogState.NoShields, viewModel.state.value.dialogState)
    }

    @Test
    fun `waterfall shows record before success`() = runTest {
        val connectTypes = listOf(RiddleType.CONNECT_PAIRS.name)
        whenever(progressRepo.updateBestTime(any(), any(), any())).thenReturn(true)
        
        val viewModel = GameViewModel(1, "Section", 1, listOf(verse), emptyList(), connectTypes, progressRepo, false)
        
        viewModel.onEvent(GameEvent.OnRiddleSuccess())
        
        assertTrue(viewModel.state.value.dialogState is GameDialogState.NewRecord)
        
        viewModel.onEvent(GameEvent.ConfirmDialog)
        // In this case for Connect it finishes or shows retention
        // Let's assume no retention gained
    }

    @Test
    fun `complex game session sequence with mistake and records`() = runTest {
        val mixedRiddles = listOf(RiddleType.QUIZ_NORMAL.name, RiddleType.QUIZ_EASY.name, RiddleType.FILL_SIGLA_BOOK.name)
        whenever(progressRepo.getShieldsCount()).thenReturn(5)
        whenever(progressRepo.updateBestLevelStreak(any())).thenReturn(true)
        
        val viewModel = GameViewModel(1, "Section", 1, listOf(verse, verse, verse), emptyList(), mixedRiddles, progressRepo, false)
        
        // 1. Success 
        viewModel.onEvent(GameEvent.OnRiddleSuccess())
        assertEquals(1, viewModel.state.value.currentIndex)
        
        // 2. Mistake
        whenever(progressRepo.getShieldsCount()).thenReturn(4)
        viewModel.onEvent(GameEvent.OnShieldLoss)
        assertEquals(4, viewModel.state.value.shieldsCount)
        
        // 3. Success
        viewModel.onEvent(GameEvent.OnRiddleSuccess())
        assertEquals(2, viewModel.state.value.currentIndex)
        
        // 4. Final Success -> Trigger Waterfall
        // No streak because mistake was made
        viewModel.onEvent(GameEvent.OnRiddleSuccess())
        
        // Generic success dialog since it was many riddles
        assertEquals(GameDialogState.GenericSuccess, viewModel.state.value.dialogState)
        
        viewModel.onEvent(GameEvent.ConfirmDialog)
        // Verify finish effect or next dialog
    }
}
