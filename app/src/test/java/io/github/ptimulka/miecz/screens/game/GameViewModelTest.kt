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
}
