package io.github.ptimulka.miecz.repositories

interface CustomSectionRepository {
    fun saveCustomSection(sectionId: Int, groupId1: Int, groupId2: Int)
    fun getCustomSectionGroups(sectionId: Int): Pair<Int, Int>?
    fun getCustomSectionsCount(): Int
    fun getAllUsedGroupIds(): Set<Int>
}
