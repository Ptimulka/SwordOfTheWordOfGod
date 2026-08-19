package io.github.ptimulka.miecz.repositories

import io.github.ptimulka.miecz.data.Section

interface SectionRepository {
    fun loadSection(resourceId: Int): Section?
    fun loadInitialSections(): List<Section>
    fun loadSectionName(sectionId: Int): String?
}
