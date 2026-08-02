package io.github.ptimulka.miecz.screens.review

import io.github.ptimulka.miecz.helpers.MainDispatcherRule
import io.github.ptimulka.miecz.repositories.ProgressRepository
import io.github.ptimulka.miecz.repositories.SectionRepository
import io.github.ptimulka.miecz.repositories.UserProgressRepository
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ReviewViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val progressRepo: ProgressRepository = mock()
    private val sectionRepo: SectionRepository = mock()
    private val groupsRepo: VersesGroupsRepository = mock()

    @Test
    fun `initial load sets shield count and availability`() {
        whenever(progressRepo.getCurrentSection()).thenReturn(3)
        whenever(progressRepo.getShieldsCount()).thenReturn(3)
        
        val viewModel = ReviewViewModel(progressRepo, sectionRepo, groupsRepo, "Review")
        
        val state = viewModel.state.value
        assertEquals(3, state.shieldsCount)
    }

    @Test
    fun `reward calculation is correct for 10 riddles`() {
        whenever(progressRepo.getCurrentSection()).thenReturn(3)
        whenever(progressRepo.getShieldsCount()).thenReturn(2) // 5 - 2 = 3 potential. Max reward is 2.
        
        val viewModel = ReviewViewModel(progressRepo, sectionRepo, groupsRepo, "Review")
        viewModel.onEvent(ReviewEvent.SetCount(10))
        
        assertEquals(2, viewModel.state.value.maxPossibleReward)
    }

    @Test
    fun `play for shields is forced to false if shields are full`() {
        whenever(progressRepo.getCurrentSection()).thenReturn(3)
        whenever(progressRepo.getShieldsCount()).thenReturn(UserProgressRepository.MAX_SHIELDS)
        
        val viewModel = ReviewViewModel(progressRepo, sectionRepo, groupsRepo, "Review")
        viewModel.onEvent(ReviewEvent.SetPlayForShields(true)) // Try to enable
        
        assertFalse(viewModel.state.value.playForShields)
    }
}
