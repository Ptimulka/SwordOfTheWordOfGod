package io.github.ptimulka.miecz.screens.groups

import android.graphics.Bitmap
import io.github.ptimulka.miecz.data.Section
import io.github.ptimulka.miecz.data.VerseGroup

data class VerseGroupsUiState(
    val searchQuery: String = "",
    val filteredSections: List<Section> = emptyList(),
    val filteredGroups: List<VerseGroup> = emptyList(),
    val groupToSectionMap: Map<Int, Int> = emptyMap(),
    val expandedId: Int? = null,
    val previewImage: Bitmap? = null,
    val existingAssets: Set<String> = emptySet(),
    val groupAssetNames: Map<Int, List<String>> = emptyMap()
)
