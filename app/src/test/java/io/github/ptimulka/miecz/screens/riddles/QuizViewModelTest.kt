package io.github.ptimulka.miecz.screens.riddles

import io.github.ptimulka.miecz.helpers.MainDispatcherRule
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase
import io.github.ptimulka.miecz.screens.riddles.quiz.QuizArgs
import io.github.ptimulka.miecz.screens.riddles.quiz.QuizEvent
import io.github.ptimulka.miecz.screens.riddles.quiz.QuizViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class QuizViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mnemonicRepo: MnemonicRepository = mock()
    private val defaultArgs = QuizArgs(
        verseText = "Na początku...",
        book = "Rdz",
        chapter = 1,
        number = "1",
        isEasy = true,
        sectionVerses = emptyList(),
        sectionId = 1,
        verseIndex = 0,
        assetName = null,
        hasHint = false
    )

    @Test
    fun `initial state has answers and correct verse text`() {
        val viewModel = QuizViewModel(defaultArgs, mnemonicRepo, mainDispatcherRule.testDispatcher)
        val state = viewModel.state.value
        
        assertEquals("Na początku...", state.verseText)
        assertEquals(4, state.answers.size)
        assertEquals(RiddlePhase.Answering, state.phase)
    }

    @Test
    fun `selecting an answer updates state`() {
        val viewModel = QuizViewModel(defaultArgs, mnemonicRepo, mainDispatcherRule.testDispatcher)
        viewModel.onEvent(QuizEvent.Select("Rdz 1,1"))
        
        assertEquals("Rdz 1,1", viewModel.state.value.selectedAnswer)
        assertTrue(viewModel.state.value.checkEnabled)
    }

    @Test
    fun `checking correct answer updates phase to result success`() {
        val viewModel = QuizViewModel(defaultArgs, mnemonicRepo, mainDispatcherRule.testDispatcher)
        viewModel.onEvent(QuizEvent.Select("Rdz 1,1"))
        viewModel.onEvent(QuizEvent.Check)
        
        val phase = viewModel.state.value.phase
        assertTrue(phase is RiddlePhase.Result)
        assertTrue((phase as RiddlePhase.Result).correct)
    }

    @Test
    fun `checking correct answer with hint updates phase to showing reward`() {
        val bitmap: android.graphics.Bitmap = mock()
        whenever(mnemonicRepo.loadActivePicture(any(), any(), anyOrNull())).thenReturn(bitmap)
        
        val viewModel = QuizViewModel(defaultArgs.copy(hasHint = true), mnemonicRepo, mainDispatcherRule.testDispatcher)
        
        // Wait for background load
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onEvent(QuizEvent.Select("Rdz 1,1"))
        viewModel.onEvent(QuizEvent.Check)
        
        assertEquals(RiddlePhase.ShowingReward, viewModel.state.value.phase)
    }

    @Test
    fun `dismissing image updates phase to result success`() {
        val bitmap: android.graphics.Bitmap = mock()
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
    fun `checking wrong answer updates phase to result failure`() {
        val viewModel = QuizViewModel(defaultArgs, mnemonicRepo, mainDispatcherRule.testDispatcher)
        // Find a wrong answer from the generated answers
        val wrongAnswer = viewModel.state.value.answers.first { it != "Rdz 1,1" }
        
        viewModel.onEvent(QuizEvent.Select(wrongAnswer))
        viewModel.onEvent(QuizEvent.Check)
        
        val phase = viewModel.state.value.phase
        assertTrue(phase is RiddlePhase.Result)
        assertTrue(!(phase as RiddlePhase.Result).correct)
    }

    @Test
    fun `dismissing result failure resets to answering phase`() {
        val viewModel = QuizViewModel(defaultArgs, mnemonicRepo, mainDispatcherRule.testDispatcher)
        val wrongAnswer = viewModel.state.value.answers.first { it != "Rdz 1,1" }
        
        viewModel.onEvent(QuizEvent.Select(wrongAnswer))
        viewModel.onEvent(QuizEvent.Check)
        viewModel.onEvent(QuizEvent.DismissResult)
        
        assertEquals(RiddlePhase.Answering, viewModel.state.value.phase)
        assertEquals(wrongAnswer, viewModel.state.value.selectedAnswer) // Selection should persist
    }
}
