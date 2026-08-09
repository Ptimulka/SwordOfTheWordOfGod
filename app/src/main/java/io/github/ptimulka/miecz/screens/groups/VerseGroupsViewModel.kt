package io.github.ptimulka.miecz.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import io.github.ptimulka.miecz.repositories.ProgressRepository
import io.github.ptimulka.miecz.repositories.SectionRepository
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VerseGroupsViewModel(
    private val sectionRepo: SectionRepository,
    private val groupsRepo: VersesGroupsRepository,
    private val progressRepo: ProgressRepository,
    private val mnemonicRepo: MnemonicRepository,
    private val assetList: Set<String>
) : ViewModel() {

    private val allSections = sectionRepo.loadInitialSections()
    private val allGroups = groupsRepo.loadVerseGroups()
    private val groupToSectionMap = buildGroupToSectionMap()
    private val groupAssetNamesMap = allGroups.associate { group ->
        group.id to group.verses.mapIndexed { index, verse ->
            "group%03d_%d_%s%d-%s.webp".format(
                group.id, index + 1, verse.book, verse.chapter,
                verse.number.replace(".", "-")
            )
        }
    }

    private val _state = MutableStateFlow(
        VerseGroupsUiState(
            filteredSections = allSections,
            filteredGroups = allGroups,
            groupToSectionMap = groupToSectionMap,
            existingAssets = assetList,
            groupAssetNames = groupAssetNamesMap
        )
    )
    val state = _state.asStateFlow()

    fun onEvent(event: VerseGroupsEvent) {
        when (event) {
            is VerseGroupsEvent.UpdateSearch -> {
                _state.update { it.copy(searchQuery = event.query) }
                applyFilter()
            }
            VerseGroupsEvent.ClearSearch -> {
                _state.update { it.copy(searchQuery = "") }
                applyFilter()
            }
            is VerseGroupsEvent.ToggleExpand -> {
                _state.update { it.copy(expandedId = if (it.expandedId == event.id) null else event.id) }
            }
            is VerseGroupsEvent.ShowPreview -> {
                viewModelScope.launch {
                    val bitmap = mnemonicRepo.loadDefaultPicture(event.assetName)
                    _state.update { it.copy(previewImage = bitmap) }
                }
            }
            VerseGroupsEvent.DismissPreview -> _state.update { it.copy(previewImage = null) }
        }
    }

    private fun applyFilter() {
        val query = _state.value.searchQuery.trim()
        if (query.isEmpty()) {
            _state.update { it.copy(filteredSections = allSections, filteredGroups = allGroups) }
        } else {
            val filteredSections = allSections.filter { matches(it.name, it.verses, query) }
            val filteredGroups = allGroups.filter { matches(it.name, it.verses, query) }
            _state.update { it.copy(filteredSections = filteredSections, filteredGroups = filteredGroups) }
        }
    }

    private fun matches(name: String, verses: List<io.github.ptimulka.miecz.data.Verse>, query: String): Boolean {
        if (name.contains(query, ignoreCase = true)) return true
        return verses.any { verse ->
            val sigla = "${verse.book} ${verse.chapter},${verse.number}"
            val cleanText = verse.text.replace('_', ' ').replace("*", "")
            sigla.contains(query, ignoreCase = true) || cleanText.contains(query, ignoreCase = true)
        }
    }

    private fun buildGroupToSectionMap(): Map<Int, Int> {
        val count = progressRepo.getCustomSectionsCount()
        return (1..count).flatMap { index ->
            val sectionId = 5 + index - 1
            progressRepo.getCustomSectionGroups(sectionId)?.let { (groupId1, groupId2) ->
                listOf(groupId1 to sectionId, groupId2 to sectionId)
            } ?: emptyList()
        }.toMap()
    }
}
