package io.github.ptimulka.miecz.repositories

interface ProgressionRepository {
    fun getCurrentSection(): Int
    fun setCurrentSection(sectionId: Int)
    fun isLevelFinished(sectionId: Int, level: Int): Boolean
    fun setLevelFinished(sectionId: Int, level: Int, finished: Boolean)
    fun isSiglaFinished(sectionId: Int): Boolean
    fun setSiglaFinished(sectionId: Int, finished: Boolean)
    fun isVerseFinished(sectionId: Int): Boolean
    fun setVerseFinished(sectionId: Int, finished: Boolean)
    fun areSpecialChallengesFinished(sectionId: Int): Boolean
    fun getFinishedLevelsCount(): Int
    fun getRepeatSectionIndex(): Int
    fun getRepeatVerseIndex(): Int
    fun saveRepeatProgress(sectionIndex: Int, verseIndex: Int)
    fun setPendingRepeatHint()
    fun consumePendingRepeatHint(): Boolean
    fun setPendingSectionUnlocked(sectionId: Int)
    fun consumePendingSectionUnlocked(): Int
    fun clearAllProgress()
}
