package io.github.ptimulka.miecz.repositories.meta

import io.github.ptimulka.miecz.repositories.BestTimeEntry
import io.github.ptimulka.miecz.repositories.AchievementRepository
import io.github.ptimulka.miecz.repositories.CustomSectionRepository
import io.github.ptimulka.miecz.repositories.SectionRepository
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository
import io.github.ptimulka.miecz.repositories.core.UserProgressStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserAchievementRepository @Inject constructor(
    private val store: UserProgressStore,
    private val customSectionRepo: CustomSectionRepository
) : AchievementRepository {

    override fun getTotalReviewedVerses(): Int = store.latest.totalReviewedVerses

    override fun addTotalReviewedVerses(count: Int) {
        if (count <= 0) return
        store.update { it.toBuilder().setTotalReviewedVerses(it.totalReviewedVerses + count).build() }
    }

    override fun getTotalAloudRepeats(): Int = store.latest.totalAloudRepeats

    override fun incrementTotalAloudRepeats() {
        store.update { it.toBuilder().setTotalAloudRepeats(it.totalAloudRepeats + 1).build() }
    }

    override fun getBestTime(sectionId: Int, riddleType: String): Long {
        return store.latest.sectionsMap[sectionId]?.bestTimesMap?.get(riddleType) ?: -1L
    }

    override fun updateBestTime(sectionId: Int, riddleType: String, elapsedMs: Long): Boolean {
        val current = getBestTime(sectionId, riddleType)
        return if (current < 0 || elapsedMs < current) {
            store.updateSection(sectionId) { it.putBestTimes(riddleType, elapsedMs) }
            true
        } else false
    }

    override fun getBestTimeOverall(
        riddleType: String,
        sectionRepo: SectionRepository,
        groupsRepo: VersesGroupsRepository
    ): BestTimeEntry? {
        val verseGroups by lazy { groupsRepo.loadVerseGroups().associateBy { it.id } }
        return store.latest.sectionsMap.entries
            .mapNotNull { (id, section) ->
                val timeMs = section.bestTimesMap[riddleType] ?: return@mapNotNull null
                val sectionName = sectionRepo.loadSectionName(id)
                    ?: customSectionRepo.getCustomSectionGroups(id)?.let { (id1, id2) ->
                        val g1 = verseGroups[id1]?.name ?: id1.toString()
                        val g2 = verseGroups[id2]?.name ?: id2.toString()
                        "$g1 i $g2"
                    } ?: "$id"
                BestTimeEntry(id, sectionName, timeMs)
            }
            .minByOrNull { it.timeMs }
    }
}
