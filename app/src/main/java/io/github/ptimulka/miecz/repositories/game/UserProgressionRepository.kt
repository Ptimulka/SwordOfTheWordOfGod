package io.github.ptimulka.miecz.repositories.game

import io.github.ptimulka.miecz.repositories.ProgressionRepository
import io.github.ptimulka.miecz.repositories.core.UserProgressSerializer
import io.github.ptimulka.miecz.repositories.core.UserProgressStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProgressionRepository @Inject constructor(
    private val store: UserProgressStore
) : ProgressionRepository {

    override fun getCurrentSection(): Int = store.latest.currentSectionId

    override fun setCurrentSection(sectionId: Int) {
        store.update { it.toBuilder().setCurrentSectionId(sectionId).build() }
    }

    override fun isLevelFinished(sectionId: Int, level: Int): Boolean {
        return store.latest.sectionsMap[sectionId]?.finishedLevelsMap?.get(level) ?: false
    }

    override fun setLevelFinished(sectionId: Int, level: Int, finished: Boolean) {
        store.updateSection(sectionId) { it.putFinishedLevels(level, finished) }
    }

    override fun isSiglaFinished(sectionId: Int): Boolean {
        return store.latest.sectionsMap[sectionId]?.siglaFinished ?: false
    }

    override fun setSiglaFinished(sectionId: Int, finished: Boolean) {
        store.updateSection(sectionId) { it.setSiglaFinished(finished) }
    }

    override fun isVerseFinished(sectionId: Int): Boolean {
        return store.latest.sectionsMap[sectionId]?.verseFinished ?: false
    }

    override fun setVerseFinished(sectionId: Int, finished: Boolean) {
        store.updateSection(sectionId) { it.setVerseFinished(finished) }
    }

    override fun areSpecialChallengesFinished(sectionId: Int): Boolean {
        return isSiglaFinished(sectionId) && isVerseFinished(sectionId)
    }

    override fun getFinishedLevelsCount(): Int {
        var count = 0
        store.latest.sectionsMap.values.forEach { section ->
            count += section.finishedLevelsMap.values.count { it }
            if (section.siglaFinished) count++
            if (section.verseFinished) count++
        }
        return count
    }

    override fun getRepeatSectionIndex(): Int = store.latest.repeatSectionIndex

    override fun getRepeatVerseIndex(): Int = store.latest.repeatVerseIndex

    override fun saveRepeatProgress(sectionIndex: Int, verseIndex: Int) {
        store.update { it.toBuilder().setRepeatSectionIndex(sectionIndex).setRepeatVerseIndex(verseIndex).build() }
    }

    override fun setPendingRepeatHint() {
        store.update { it.toBuilder().setPendingRepeatHint(true).build() }
    }

    override fun consumePendingRepeatHint(): Boolean {
        val has = store.latest.pendingRepeatHint
        if (has) store.update { it.toBuilder().setPendingRepeatHint(false).build() }
        return has
    }

    override fun setPendingSectionUnlocked(sectionId: Int) {
        store.update { it.toBuilder().setPendingSectionUnlocked(sectionId).build() }
    }

    override fun consumePendingSectionUnlocked(): Int {
        val id = store.latest.pendingSectionUnlocked
        if (id != 0) store.update { it.toBuilder().setPendingSectionUnlocked(0).build() }
        return if (id == 0) -1 else id
    }

    override fun consumePendingStreakAnimation(): Boolean {
        val has = store.latest.pendingStreakAnimation
        if (has) store.update { it.toBuilder().setPendingStreakAnimation(false).build() }
        return has
    }

    override fun clearAllProgress() {
        store.update { UserProgressSerializer.defaultValue }
    }
}
