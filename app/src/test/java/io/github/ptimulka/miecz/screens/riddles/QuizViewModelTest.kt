package io.github.ptimulka.miecz.screens.riddles

import android.graphics.Bitmap
import io.github.ptimulka.miecz.helpers.MainDispatcherRule
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase
import io.github.ptimulka.miecz.screens.riddles.quiz.QuizArgs
import io.github.ptimulka.miecz.screens.riddles.quiz.QuizEvent
import io.github.ptimulka.miecz.screens.riddles.quiz.QuizViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mnemonicRepo: MnemonicRepository = mock()

    private val verse = Verse("Rdz", 1, "1", "Test verse")
    private val defaultArgs = QuizArgs(
        verseText = "Test verse",
        book = "Rdz",
        chapter = 1,
        number = "1",
        isEasy = true,
        sectionVerses = listOf(verse),
        sectionId = 1,
        verseIndex = 0,
        assetName = "asset",
        hasHint = false
    )

    @Test
    fun `correct answer updates phase to Result with correct=true`() = runTest {
        val viewModel = QuizViewModel(defaultArgs, mnemonicRepo, mainDispatcherRule.testDispatcher)

        viewModel.onEvent(QuizEvent.Select("Rdz 1,1"))
        viewModel.onEvent(QuizEvent.Check)

        val state = viewModel.state.value
        assertTrue(state.phase is RiddlePhase.Result)
        assertTrue((state.phase as RiddlePhase.Result).correct)
    }

    @Test
    fun `wrong answer updates phase to Result with correct=false`() = runTest {
        val viewModel = QuizViewModel(defaultArgs, mnemonicRepo, mainDispatcherRule.testDispatcher)

        viewModel.onEvent(QuizEvent.Select("Exo 1,1"))
        viewModel.onEvent(QuizEvent.Check)

        val state = viewModel.state.value
        assertTrue(state.phase is RiddlePhase.Result)
        assertFalse((state.phase as RiddlePhase.Result).correct)
    }

    @Test
    fun `initialization loads hint bitmap if provided`() = runTest {
        val bitmap = mock<Bitmap>()
        whenever(mnemonicRepo.loadActivePicture(any(), any(), anyOrNull())).thenReturn(bitmap)

        val viewModel = QuizViewModel(defaultArgs.copy(hasHint = true), mnemonicRepo, mainDispatcherRule.testDispatcher)
        
        // Wait for background load
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(bitmap, viewModel.state.value.hintBitmap)
    }

    @Test
    fun `checking correct answer with hint updates phase to showing reward`() = runTest {
        val bitmap: Bitmap = mock()
        whenever(mnemonicRepo.loadActivePicture(any(), any(), anyOrNull())).thenReturn(bitmap)
        
        val viewModel = QuizViewModel(defaultArgs.copy(hasHint = true), mnemonicRepo, mainDispatcherRule.testDispatcher)
        
        // Wait for background load
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onEvent(QuizEvent.Select("Rdz 1,1"))
        viewModel.onEvent(QuizEvent.Check)
        
        val phase = viewModel.state.value.phase
        assertTrue(phase is RiddlePhase.ShowingReward)
        assertEquals(bitmap, (phase as RiddlePhase.ShowingReward).bitmap)
    }

    @Test
    fun `dismissing image updates phase to result success`() = runTest {
        val bitmap: Bitmap = mock()
        whenever(mnemonicRepo.loadActivePicture(any(), any(), anyOrNull())).thenReturn(bitmap)

        val viewModel = QuizViewModel(defaultArgs.copy(hasHint = true), mnemonicRepo, mainDispatcherRule.testDispatcher)
        
        // Wait for background load
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(QuizEvent.Select("Rdz 1,1"))
        viewModel.onEvent(QuizEvent.Check)
        viewModel.onEvent(QuizEvent.DismissHint)
        
        val phase = viewModel.state.value.phase
        assertTrue(phase is RiddlePhase.Result)
        assertTrue((phase as RiddlePhase.Result).correct)
    }

    @Test
    fun `dismissing result failure resets to answering phase`() = runTest {
        val viewModel = QuizViewModel(defaultArgs, mnemonicRepo, mainDispatcherRule.testDispatcher)
        val wrongAnswer = viewModel.state.value.answers.first { it != "Rdz 1,1" }
        
        viewModel.onEvent(QuizEvent.Select(wrongAnswer))
        viewModel.onEvent(QuizEvent.Check)
        viewModel.onEvent(QuizEvent.DismissResult)
        
        assertEquals(RiddlePhase.Answering, viewModel.state.value.phase)
        assertEquals(wrongAnswer, viewModel.state.value.selectedAnswer) // Selection should persist
    }
}
