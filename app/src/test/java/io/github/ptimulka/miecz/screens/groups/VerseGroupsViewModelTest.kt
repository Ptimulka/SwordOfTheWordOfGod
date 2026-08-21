package io.github.ptimulka.miecz.screens.groups

import io.github.ptimulka.miecz.helpers.MainDispatcherRule
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import io.github.ptimulka.miecz.repositories.SectionRepository
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository
import io.github.ptimulka.miecz.repositories.ProgressRepository
import io.github.ptimulka.miecz.data.VerseGroup
import io.github.ptimulka.miecz.data.Section
import io.github.ptimulka.miecz.data.Verse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
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
    private val assetList = setOf("asset1", "asset2")

    @Before
    fun setup() = runTest {
        whenever(sectionRepo.loadInitialSections()).thenReturn(emptyList())
        whenever(groupsRepo.loadVerseGroups()).thenReturn(emptyList())
        whenever(progressRepo.getCurrentSection()).thenReturn(1)
        whenever(progressRepo.getCustomSectionsCount()).thenReturn(0)
    }

    @Test
    fun `initialization loads empty lists by default`() = runTest {
        val viewModel = VerseGroupsViewModel(sectionRepo, groupsRepo, progressRepo, mnemonicRepo, assetList)
        
        assertEquals(emptyList<Section>(), viewModel.state.value.filteredSections)
        assertEquals(emptyList<VerseGroup>(), viewModel.state.value.filteredGroups)
    }

    @Test
    fun `updateSearch filters sections and groups`() = runTest {
        val section = Section(1, "Genesis", emptyList())
        whenever(sectionRepo.loadInitialSections()).thenReturn(listOf(section))
        
        val viewModel = VerseGroupsViewModel(sectionRepo, groupsRepo, progressRepo, mnemonicRepo, assetList)
        
        viewModel.onEvent(VerseGroupsEvent.UpdateSearch("gen"))
        assertEquals(1, viewModel.state.value.filteredSections.size)
        
        viewModel.onEvent(VerseGroupsEvent.UpdateSearch("xyz"))
        assertEquals(0, viewModel.state.value.filteredSections.size)
    }

    @Test
    fun `updateSearch filters by sigla`() = runTest {
        val verse = Verse("J", 3, "16", "Tak bowiem Bóg umiłował świat")
        val section = Section(1, "Genesis", listOf(verse))
        whenever(sectionRepo.loadInitialSections()).thenReturn(listOf(section))
        
        val viewModel = VerseGroupsViewModel(sectionRepo, groupsRepo, progressRepo, mnemonicRepo, assetList)
        
        viewModel.onEvent(VerseGroupsEvent.UpdateSearch("J 3,16"))
        assertEquals(1, viewModel.state.value.filteredSections.size)
    }

    @Test
    fun `updateSearch filters by verse text`() = runTest {
        val verse = Verse("J", 3, "16", "Tak bowiem Bóg umiłował świat")
        val group = VerseGroup(1, "Miłość", listOf(verse))
        whenever(groupsRepo.loadVerseGroups()).thenReturn(listOf(group))
        
        val viewModel = VerseGroupsViewModel(sectionRepo, groupsRepo, progressRepo, mnemonicRepo, assetList)
        
        viewModel.onEvent(VerseGroupsEvent.UpdateSearch("umiłował"))
        assertEquals(1, viewModel.state.value.filteredGroups.size)
    }

    @Test
    fun `clearSearch resets filters`() = runTest {
        val viewModel = VerseGroupsViewModel(sectionRepo, groupsRepo, progressRepo, mnemonicRepo, assetList)
        
        viewModel.onEvent(VerseGroupsEvent.UpdateSearch("query"))
        viewModel.onEvent(VerseGroupsEvent.ClearSearch)
        
        assertEquals("", viewModel.state.value.searchQuery)
    }

    @Test
    fun `toggleExpand updates state`() = runTest {
        val viewModel = VerseGroupsViewModel(sectionRepo, groupsRepo, progressRepo, mnemonicRepo, assetList)
        
        viewModel.onEvent(VerseGroupsEvent.ToggleExpand(1))
        assertEquals(1, viewModel.state.value.expandedId)
        
        viewModel.onEvent(VerseGroupsEvent.ToggleExpand(1))
        assertNull(viewModel.state.value.expandedId)
    }

    @Test
    fun `loadData builds custom sections and mappings correctly`() = runTest {
        val group1 = VerseGroup(1, "Group 1", emptyList())
        val group2 = VerseGroup(2, "Group 2", emptyList())
        whenever(groupsRepo.loadVerseGroups()).thenReturn(listOf(group1, group2))
        whenever(progressRepo.getCurrentSection()).thenReturn(5)
        whenever(progressRepo.getCustomSectionsCount()).thenReturn(1)
        whenever(progressRepo.getCustomSectionGroups(5)).thenReturn(1 to 2)

        val viewModel = VerseGroupsViewModel(sectionRepo, groupsRepo, progressRepo, mnemonicRepo, assetList)
        
        val state = viewModel.state.value
        assertEquals(0, state.filteredSections.size) // No base sections in setup, and custom sections are excluded from list
        assertEquals(5, state.groupToSectionMap[1])
        assertEquals(5, state.groupToSectionMap[2])
        assertEquals(2, state.groupAssetNames.size)
    }

    @Test
    fun `loadData includes base sections but excludes custom sections from list`() = runTest {
        val baseSection = Section(1, "Base", emptyList())
        whenever(sectionRepo.loadInitialSections()).thenReturn(listOf(baseSection))
        whenever(progressRepo.getCustomSectionsCount()).thenReturn(1)
        whenever(progressRepo.getCustomSectionGroups(5)).thenReturn(1 to 2)

        val viewModel = VerseGroupsViewModel(sectionRepo, groupsRepo, progressRepo, mnemonicRepo, assetList)
        
        val state = viewModel.state.value
        assertEquals(1, state.filteredSections.size)
        assertEquals(1, state.filteredSections[0].id)
    }
}
