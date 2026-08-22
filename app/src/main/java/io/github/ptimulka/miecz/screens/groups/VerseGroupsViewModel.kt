package io.github.ptimulka.miecz.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ptimulka.miecz.data.Constants
import io.github.ptimulka.miecz.data.Section
import io.github.ptimulka.miecz.data.VerseGroup
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import io.github.ptimulka.miecz.repositories.SectionRepository
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository
import io.github.ptimulka.miecz.repositories.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class VerseGroupsViewModel @Inject constructor(
    private val sectionRepo: SectionRepository,
    private val groupsRepo: VersesGroupsRepository,
    private val progressRepo: ProgressRepository,
    private val mnemonicRepo: MnemonicRepository,
    @param:Named("assetList") private val assetList: Set<String>
) : ViewModel() {

    private val _state = MutableStateFlow(VerseGroupsUiState(existingAssets = assetList))
    val state = _state.asStateFlow()

    private var allSections: List<Section> = emptyList()
    private var allGroups: List<VerseGroup> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val baseSections = sectionRepo.loadInitialSections()
            allGroups = groupsRepo.loadVerseGroups()
            
            val currentSectionId = progressRepo.getCurrentSection()
            val customCount = progressRepo.getCustomSectionsCount()
            
            val customSections = (1..customCount).mapNotNull { index ->
                val sectionId = Constants.CUSTOM_SECTION_START_ID + index - 1
                progressRepo.getCustomSectionGroups(sectionId)?.let { (id1, id2) ->
                    val g1 = allGroups.find { it.id == id1 }
                    val g2 = allGroups.find { it.id == id2 }
                    if (g1 != null && g2 != null) {
                        val verses = g1.verses + g2.verses
                        Section(id = sectionId, name = "${g1.name} i ${g2.name}", verses = verses)
                    } else null
                }
            }

            allSections = baseSections

            val groupToSection = mutableMapOf<Int, Int>()
            val groupAssetNames = mutableMapOf<Int, List<String>>()

            // Map which group belongs to which unlocked custom section
            customSections.forEach { section ->
                if (section.id <= currentSectionId) {
                    progressRepo.getCustomSectionGroups(section.id)?.let { (id1, id2) ->
                        groupToSection[id1] = section.id
                        groupToSection[id2] = section.id
                    }
                }
            }

            // Map asset names for groups
            allGroups.forEach { group ->
                groupAssetNames[group.id] = group.verses.mapIndexed { i, v ->
                    "group%03d_%d_%s%d-%s.webp".format(group.id, i + 1, v.book, v.chapter, v.number.replace(".", "-"))
                }
            }

            _state.update { it.copy(
                filteredSections = allSections,
                filteredGroups = allGroups,
                groupToSectionMap = groupToSection,
                groupAssetNames = groupAssetNames
            ) }
        }
    }

    fun onEvent(event: VerseGroupsEvent) {
        when (event) {
            is VerseGroupsEvent.UpdateSearch -> {
                _state.update { it.copy(searchQuery = event.query) }
                filter(event.query)
            }
            VerseGroupsEvent.ClearSearch -> {
                _state.update { it.copy(searchQuery = "") }
                filter("")
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
            VerseGroupsEvent.DismissPreview -> {
                _state.update { it.copy(previewImage = null) }
            }
        }
    }

    private fun filter(query: String) {
        if (query.isBlank()) {
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
}
