package io.github.ptimulka.miecz.screens.riddles

import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase
import io.github.ptimulka.miecz.screens.riddles.fill_whole_verse.FillWholeVerseArgs
import io.github.ptimulka.miecz.screens.riddles.fill_whole_verse.FillWholeVerseEvent
import io.github.ptimulka.miecz.screens.riddles.fill_whole_verse.FillWholeVerseViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FillWholeVerseViewModelTest {

    private val defaultArgs = FillWholeVerseArgs(
        verseText = "Tak bowiem Bóg umiłował *świat* że syna swego dał aby każdy kto wierzy miał życie wieczne.",
        book = "J",
        chapter = 3,
        number = "16"
    )

    @Test
    fun `checking correct verse updates phase to success`() {
        val viewModel = FillWholeVerseViewModel(defaultArgs)
        
        viewModel.onEvent(FillWholeVerseEvent.UpdateInput("Tak bowiem Bóg umiłował świat " +
                "że syna swego dał aby każdy kto wierzy miał życie wieczne."))
        viewModel.onEvent(FillWholeVerseEvent.Check)
        
        val phase = viewModel.state.value.phase
        assertTrue(phase is RiddlePhase.Result)
        assertTrue((phase as RiddlePhase.Result).correct)
    }

    @Test
    fun `checking verse without optional parts is still correct`() {
        val viewModel = FillWholeVerseViewModel(defaultArgs)
        
        viewModel.onEvent(FillWholeVerseEvent.UpdateInput("Tak bowiem Bóg umiłował " +
                "że syna swego dał aby każdy kto wierzy miał życie wieczne."))
        viewModel.onEvent(FillWholeVerseEvent.Check)
        
        assertTrue((viewModel.state.value.phase as RiddlePhase.Result).correct)
    }

    @Test
    fun `ignores case and diacritics during check`() {
        val viewModel = FillWholeVerseViewModel(defaultArgs)
        
        // Mixed case and no diacritics
        viewModel.onEvent(FillWholeVerseEvent.UpdateInput("TAK BOWIEM BOG UMILOWAL SWIAT " +
                "ZE SYNA SWEGO DAL ABY KAZDY KTO WIERZY MIAL ZYCIE WIECZNE."))
        viewModel.onEvent(FillWholeVerseEvent.Check)
        
        val state = viewModel.state.value
        assertTrue((state.phase as RiddlePhase.Result).correct)
        assertEquals(100f, state.similarityScore)
    }

    @Test
    fun `similarity threshold is respected`() {
        val viewModel = FillWholeVerseViewModel(defaultArgs)
        
        // Slightly misspelled word (bowiem vs albowiem)
        // If > 90% it's correct. 
        viewModel.onEvent(FillWholeVerseEvent.UpdateInput("Tak albowiem Bóg umiłował świat " +
                "że syna swego dał aby każdy kto wierzy miał zycie wieczne."))
        viewModel.onEvent(FillWholeVerseEvent.Check)
        
        assertTrue((viewModel.state.value.phase as RiddlePhase.Result).correct)
        assertTrue(viewModel.state.value.similarityScore >= 90f)
    }

    @Test
    fun `low similarity results in failure and provides diffs`() {
        val viewModel = FillWholeVerseViewModel(defaultArgs)
        
        viewModel.onEvent(FillWholeVerseEvent.UpdateInput("Inny tekst."))
        viewModel.onEvent(FillWholeVerseEvent.Check)
        
        val state = viewModel.state.value
        assertFalse((state.phase as RiddlePhase.Result).correct)
        assertTrue(state.diffs.isNotEmpty())
    }
}
