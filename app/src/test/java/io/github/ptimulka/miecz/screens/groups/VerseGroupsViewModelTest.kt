package io.github.ptimulka.miecz.screens.groups

import io.github.ptimulka.miecz.data.Section
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.data.VerseGroup
import io.github.ptimulka.miecz.helpers.MainDispatcherRule
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import io.github.ptimulka.miecz.repositories.ProgressRepository
import io.github.ptimulka.miecz.repositories.SectionRepository
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class VerseGroupsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sectionRepo: SectionRepository = mock()
    private val groupsRepo: VersesGroupsRepository = mock()
    private val progressRepo: ProgressRepository = mock()
    private val mnemonicRepo: MnemonicRepository = mock()

    private val verse = Verse("Jan", 3, "16", "Tak bowiem Bóg umiłował świat")
    private val section = Section(1, "Ewangelie", listOf(verse), emptyList())
    private val group = VerseGroup(2, "Listy", listOf(verse))

    @Test
    fun `initial load displays all sections and groups`() = runTest {
        whenever(sectionRepo.loadInitialSections()).thenReturn(listOf(section))
        whenever(groupsRepo.loadVerseGroups()).thenReturn(listOf(group))
        
        val viewModel = VerseGroupsViewModel(sectionRepo, groupsRepo, progressRepo, mnemonicRepo, emptySet())
        
        val state = viewModel.state.value
        assertEquals(1, state.filteredSections.size)
        assertEquals(1, state.filteredGroups.size)
        assertEquals("Ewangelie", state.filteredSections[0].name)
    }

    @Test
    fun `search filters by name`() = runTest {
        whenever(sectionRepo.loadInitialSections()).thenReturn(listOf(section))
        whenever(groupsRepo.loadVerseGroups()).thenReturn(listOf(group))
        val viewModel = VerseGroupsViewModel(sectionRepo, groupsRepo, progressRepo, mnemonicRepo, emptySet())
        
        viewModel.onEvent(VerseGroupsEvent.UpdateSearch("Ewan"))
        
        val state = viewModel.state.value
        assertEquals(1, state.filteredSections.size)
        assertEquals(0, state.filteredGroups.size)
    }

    @Test
    fun `search filters by verse text`() = runTest {
        whenever(sectionRepo.loadInitialSections()).thenReturn(listOf(section))
        whenever(groupsRepo.loadVerseGroups()).thenReturn(listOf(group))
        val viewModel = VerseGroupsViewModel(sectionRepo, groupsRepo, progressRepo, mnemonicRepo, emptySet())
        
        viewModel.onEvent(VerseGroupsEvent.UpdateSearch("umiłował"))
        
        val state = viewModel.state.value
        assertEquals(1, state.filteredSections.size)
        assertEquals(1, state.filteredGroups.size)
    }

    @Test
    fun `toggleExpand updates expandedId`() = runTest {
        whenever(sectionRepo.loadInitialSections()).thenReturn(listOf(section))
        whenever(groupsRepo.loadVerseGroups()).thenReturn(listOf(group))
        val viewModel = VerseGroupsViewModel(sectionRepo, groupsRepo, progressRepo, mnemonicRepo, emptySet())
        
        viewModel.onEvent(VerseGroupsEvent.ToggleExpand(10))
        assertEquals(10, viewModel.state.value.expandedId)
        
        viewModel.onEvent(VerseGroupsEvent.ToggleExpand(10))
        assertEquals(null, viewModel.state.value.expandedId)
    }
}
