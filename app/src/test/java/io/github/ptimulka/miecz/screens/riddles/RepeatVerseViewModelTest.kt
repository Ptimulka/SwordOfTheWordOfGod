package io.github.ptimulka.miecz.screens.riddles

import android.graphics.Bitmap
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import io.github.ptimulka.miecz.repositories.ProgressRepository
import io.github.ptimulka.miecz.screens.riddles.repeat_verse.RepeatVerseArgs
import io.github.ptimulka.miecz.screens.riddles.repeat_verse.RepeatVerseEvent
import io.github.ptimulka.miecz.screens.riddles.repeat_verse.RepeatVerseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class RepeatVerseViewModelTest {

    private val progressRepo: ProgressRepository = mock()
    private val mnemonicRepo: MnemonicRepository = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val verse = Verse("Gen", 1, "1", "Początek")
    private val args = RepeatVerseArgs(1, listOf(verse), listOf("asset"))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selectVerse updates state and thumbnail hint`() = runTest {
        val bitmap = mock<Bitmap>()
        whenever(mnemonicRepo.loadActivePicture(any(), any(), anyOrNull())).thenReturn(bitmap)
        
        val viewModel = RepeatVerseViewModel(args, progressRepo, mnemonicRepo, testDispatcher)
        
        viewModel.onEvent(RepeatVerseEvent.SelectVerse(0))
        
        val state = viewModel.state.value
        assertEquals(0, state.selectedIndex)
        assertEquals(bitmap, state.hintBitmap)
    }

    @Test
    fun `processResult with high similarity increments repeats`() = runTest {
        whenever(progressRepo.incrementVerseRepeatToday(any(), any())).thenReturn(1)
        val viewModel = RepeatVerseViewModel(args, progressRepo, mnemonicRepo, testDispatcher)
        
        viewModel.onEvent(RepeatVerseEvent.SelectVerse(0))
        viewModel.onEvent(RepeatVerseEvent.ProcessResult("Początek"))
        
        verify(progressRepo).incrementVerseRepeatToday(1, 0)
        assertEquals(1, viewModel.state.value.repeatCount)
    }

    @Test
    fun `processResult with low similarity does not increment`() = runTest {
        val viewModel = RepeatVerseViewModel(args, progressRepo, mnemonicRepo, testDispatcher)
        
        viewModel.onEvent(RepeatVerseEvent.SelectVerse(0))
        viewModel.onEvent(RepeatVerseEvent.ProcessResult("Koniec"))
        
        verify(progressRepo, never()).incrementVerseRepeatToday(any(), any())
    }

    @Test
    fun `initialization loads all thumbnails`() = runTest {
        val bitmap = mock<Bitmap>()
        whenever(mnemonicRepo.loadActivePicture(any(), any(), anyOrNull())).thenReturn(bitmap)
        
        val viewModel = RepeatVerseViewModel(args, progressRepo, mnemonicRepo, testDispatcher)
        
        assertEquals(1, viewModel.state.value.thumbnails.size)
        assertEquals(bitmap, viewModel.state.value.thumbnails[0])
    }
}
