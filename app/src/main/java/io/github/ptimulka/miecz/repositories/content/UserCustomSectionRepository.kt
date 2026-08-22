package io.github.ptimulka.miecz.repositories.content

import io.github.ptimulka.miecz.repositories.CustomSectionRepository
import io.github.ptimulka.miecz.repositories.core.UserProgressStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserCustomSectionRepository @Inject constructor(
    private val store: UserProgressStore
) : CustomSectionRepository {

    override fun saveCustomSection(sectionId: Int, groupId1: Int, groupId2: Int) {
        store.update { it.toBuilder().putCustomSections(sectionId, "$groupId1,$groupId2").build() }
    }

    override fun getCustomSectionGroups(sectionId: Int): Pair<Int, Int>? {
        val data = store.latest.customSectionsMap[sectionId] ?: return null
        val parts = data.split(",")
        return if (parts.size == 2) {
            Pair(parts[0].toInt(), parts[1].toInt())
        } else null
    }

    override fun getCustomSectionsCount(): Int = store.latest.customSectionsCount

    override fun getAllUsedGroupIds(): Set<Int> {
        val usedIds = mutableSetOf<Int>()
        store.latest.customSectionsMap.values.forEach { data ->
            val parts = data.split(",")
            if (parts.size == 2) {
                usedIds.add(parts[0].toInt())
                usedIds.add(parts[1].toInt())
            }
        }
        return usedIds
    }
}
