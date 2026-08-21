package io.github.ptimulka.miecz.repositories

import io.github.ptimulka.miecz.data.Section

interface SectionRepository {
    suspend fun loadSection(resourceId: Int): Section?
    suspend fun loadInitialSections(): List<Section>
    suspend fun loadSectionName(sectionId: Int): String?
}
