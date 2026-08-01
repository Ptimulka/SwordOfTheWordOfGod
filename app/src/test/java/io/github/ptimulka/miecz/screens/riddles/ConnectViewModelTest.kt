package io.github.ptimulka.miecz.screens.riddles

import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.MainDispatcherRule
import io.github.ptimulka.miecz.screens.riddles.connect.ConnectEvent
import io.github.ptimulka.miecz.screens.riddles.connect.ConnectPairsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConnectViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val verses = listOf(
        Verse("Rdz", 1, "1", "Tekst 1"),
        Verse("J", 1, "1", "Tekst 2")
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `matching a correct pair removes items from state`() = runTest {
        val viewModel = ConnectPairsViewModel(verses, emptyMap())
        val state = viewModel.state.value
        
        val left = state.leftItems.first()
        val right = state.rightItems.first { it.id == left.id }
        
        viewModel.onEvent(ConnectEvent.SelectLeft(left))
        viewModel.onEvent(ConnectEvent.SelectRight(right))
        
        advanceTimeBy(400) // Skip match delay
        
        assertTrue(viewModel.state.value.leftItems.none { it.id == left.id })
    }

    @Test
    fun `incorrect pair triggers lockout`() {
        val viewModel = ConnectPairsViewModel(verses, emptyMap())
        val state = viewModel.state.value
        
        val left = state.leftItems.first()
        val right = state.rightItems.first { it.id != left.id }
        
        viewModel.onEvent(ConnectEvent.SelectLeft(left))
        viewModel.onEvent(ConnectEvent.SelectRight(right))
        
        assertTrue(viewModel.state.value.isLocked)
        assertNotNull(viewModel.state.value.wrongPair)
    }
}
