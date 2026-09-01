package io.github.ptimulka.miecz.repositories

interface ProgressRepository {

    // ── Section / level completion ────────────────────────────────────────────
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

    // ── Retention ─────────────────────────────────────────────────────────────
    fun getRetention(sectionId: Int): Int
    fun setRetention(sectionId: Int, retention: Int)
    fun addRetention(sectionId: Int, amount: Int): Int
    fun applyDailyRetentionDecay()
    fun isConnectDoneToday(sectionId: Int, riddleType: String): Boolean
    fun awardRetentionForConnectLevel(sectionId: Int, riddleType: String): Int
    fun awardRetentionForStandardLevel(sectionId: Int, levelNumber: Int): Int
    fun retentionContributionForRepeats(count: Int): Int

    // ── Aloud verse repeats (per day) ─────────────────────────────────────────
    fun getVerseRepeatCountToday(sectionId: Int, verseIndex: Int): Int
    fun incrementVerseRepeatToday(sectionId: Int, verseIndex: Int): Int

    // ── Custom sections ───────────────────────────────────────────────────────
    fun saveCustomSection(sectionId: Int, groupId1: Int, groupId2: Int)
    fun getCustomSectionGroups(sectionId: Int): Pair<Int, Int>?
    fun getCustomSectionsCount(): Int
    fun getAllUsedGroupIds(): Set<Int>

    // ── Mnemonic choices ──────────────────────────────────────────────────────
    fun getMnemonicChoice(sectionId: Int, verseIndex: Int): String?
    fun saveMnemonicChoice(sectionId: Int, verseIndex: Int, choice: String)

    // ── Repeat-screen cursor ──────────────────────────────────────────────────
    fun getRepeatSectionIndex(): Int
    fun getRepeatVerseIndex(): Int
    fun saveRepeatProgress(sectionIndex: Int, verseIndex: Int)

    // ── Shields ───────────────────────────────────────────────────────────────
    fun getShieldsCount(): Int
    fun setShieldsCount(count: Int)
    fun decreaseShields(): Int
    fun increaseShields(): Int
    fun getLastShieldUpdateTime(): Long
    fun refreshShields(): Int
    fun getTimeToNextShield(): Long

    // ── Pending UI hints ──────────────────────────────────────────────────────
    fun setPendingRepeatHint()
    fun consumePendingRepeatHint(): Boolean
    fun setPendingSectionUnlocked(sectionId: Int)
    fun consumePendingSectionUnlocked(): Int
    fun consumePendingStreakAnimation(): Boolean

    // ── Streaks ───────────────────────────────────────────────────────────────
    fun getLevelStreak(): Int
    fun incrementLevelStreak()
    fun resetLevelStreak()
    fun getBestLevelStreak(): Int
    fun updateBestLevelStreak(current: Int): Boolean
    fun reconcileBestStreaks()
    fun hasPlayedToday(): Boolean
    fun getCurrentDayStreak(): Int
    fun getBestDayStreak(): Int
    fun updateDayStreak()

    // ── Best times ────────────────────────────────────────────────────────────
    fun getBestTime(sectionId: Int, riddleType: String): Long
    fun updateBestTime(sectionId: Int, riddleType: String, elapsedMs: Long): Boolean
    suspend fun getBestTimeOverall(
        riddleType: String,
        sectionRepo: SectionRepository,
        groupsRepo: VersesGroupsRepository
    ): BestTimeEntry?

    // ── Achievement tallies ───────────────────────────────────────────────────
    fun getTotalReviewedVerses(): Int
    fun addTotalReviewedVerses(count: Int)
    fun getTotalAloudRepeats(): Int
    fun incrementTotalAloudRepeats()

    // ── Reset ─────────────────────────────────────────────────────────────────
    fun clearAllProgress()
}
