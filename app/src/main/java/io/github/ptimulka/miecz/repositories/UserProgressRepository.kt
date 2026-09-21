package io.github.ptimulka.miecz.repositories
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade implementation of [ProgressRepository] that coordinates several specialized repositories.
 * This allows the UI and ViewModels to continue using a single interface while the internal
 * implementation is decoupled and organized into logical sub-repositories.
 */
@Singleton
class UserProgressRepository @Inject constructor(
    private val progressionRepo: ProgressionRepository,
    private val shieldRepo: ShieldRepository,
    private val streakRepo: StreakRepository,
    private val retentionRepo: RetentionRepository,
    private val mnemonicChoiceRepo: MnemonicChoiceRepository,
    private val customSectionRepo: CustomSectionRepository,
    private val achievementRepo: AchievementRepository
) : ProgressRepository {

    companion object {
        const val MAX_SHIELDS = 5
        const val MAX_VERSE_REPEATS_PER_DAY = 10
    }

    // ── Section / level completion ────────────────────────────────────────────
    override fun getCurrentSection(): Int = progressionRepo.getCurrentSection()
    override fun setCurrentSection(sectionId: Int) = progressionRepo.setCurrentSection(sectionId)
    override fun isLevelFinished(sectionId: Int, level: Int): Boolean = progressionRepo.isLevelFinished(sectionId, level)
    override fun setLevelFinished(sectionId: Int, level: Int, finished: Boolean) = progressionRepo.setLevelFinished(sectionId, level, finished)
    override fun isSiglaFinished(sectionId: Int): Boolean = progressionRepo.isSiglaFinished(sectionId)
    override fun setSiglaFinished(sectionId: Int, finished: Boolean) = progressionRepo.setSiglaFinished(sectionId, finished)
    override fun isVerseFinished(sectionId: Int): Boolean = progressionRepo.isVerseFinished(sectionId)
    override fun setVerseFinished(sectionId: Int, finished: Boolean) = progressionRepo.setVerseFinished(sectionId, finished)
    override fun areSpecialChallengesFinished(sectionId: Int): Boolean = progressionRepo.areSpecialChallengesFinished(sectionId)
    override fun getFinishedLevelsCount(): Int = progressionRepo.getFinishedLevelsCount()

    // ── Retention ─────────────────────────────────────────────────────────────
    override fun getRetention(sectionId: Int): Int = retentionRepo.getRetention(sectionId)
    override fun setRetention(sectionId: Int, retention: Int) = retentionRepo.setRetention(sectionId, retention)
    override fun addRetention(sectionId: Int, amount: Int): Int = retentionRepo.addRetention(sectionId, amount)
    override fun applyDailyRetentionDecay() = retentionRepo.applyDailyRetentionDecay()
    override fun isConnectDoneToday(sectionId: Int, riddleType: String): Boolean = retentionRepo.isConnectDoneToday(sectionId, riddleType)
    override fun awardRetentionForConnectLevel(sectionId: Int, riddleType: String): Int = retentionRepo.awardRetentionForConnectLevel(sectionId, riddleType)
    override fun awardRetentionForStandardLevel(sectionId: Int, levelNumber: Int): Int = retentionRepo.awardRetentionForStandardLevel(sectionId, levelNumber)
    override fun retentionContributionForRepeats(count: Int, isLongVerse: Boolean): Int = retentionRepo.retentionContributionForRepeats(count, isLongVerse)

    // ── Aloud verse repeats (per day) ─────────────────────────────────────────
    override fun getVerseRepeatCountToday(sectionId: Int, verseIndex: Int): Int = retentionRepo.getVerseRepeatCountToday(sectionId, verseIndex)
    override fun incrementVerseRepeatToday(sectionId: Int, verseIndex: Int, isLongVerse: Boolean): Int = retentionRepo.incrementVerseRepeatToday(sectionId, verseIndex, isLongVerse)

    // ── Custom sections ───────────────────────────────────────────────────────
    override fun saveCustomSection(sectionId: Int, groupId1: Int, groupId2: Int) = customSectionRepo.saveCustomSection(sectionId, groupId1, groupId2)
    override fun getCustomSectionGroups(sectionId: Int): Pair<Int, Int>? = customSectionRepo.getCustomSectionGroups(sectionId)
    override fun getCustomSectionsCount(): Int = customSectionRepo.getCustomSectionsCount()
    override fun getAllUsedGroupIds(): Set<Int> = customSectionRepo.getAllUsedGroupIds()

    // ── Mnemonic choices ──────────────────────────────────────────────────────
    override fun getMnemonicChoice(sectionId: Int, verseIndex: Int): String? = mnemonicChoiceRepo.getMnemonicChoice(sectionId, verseIndex)
    override fun saveMnemonicChoice(sectionId: Int, verseIndex: Int, choice: String) = mnemonicChoiceRepo.saveMnemonicChoice(sectionId, verseIndex, choice)

    // ── Repeat-screen cursor ──────────────────────────────────────────────────
    override fun getRepeatSectionIndex(): Int = progressionRepo.getRepeatSectionIndex()
    override fun getRepeatVerseIndex(): Int = progressionRepo.getRepeatVerseIndex()
    override fun saveRepeatProgress(sectionIndex: Int, verseIndex: Int) = progressionRepo.saveRepeatProgress(sectionIndex, verseIndex)

    // ── Shields ───────────────────────────────────────────────────────────────
    override fun getShieldsCount(): Int = shieldRepo.getShieldsCount()
    override fun setShieldsCount(count: Int) = shieldRepo.setShieldsCount(count)
    override fun decreaseShields(): Int = shieldRepo.decreaseShields()
    override fun increaseShields(): Int = shieldRepo.increaseShields()
    override fun getLastShieldUpdateTime(): Long = shieldRepo.getLastShieldUpdateTime()
    override fun refreshShields(): Int = shieldRepo.refreshShields()
    override fun getTimeToNextShield(): Long = shieldRepo.getTimeToNextShield()

    // ── Pending UI hints ──────────────────────────────────────────────────────
    override fun setPendingRepeatHint() = progressionRepo.setPendingRepeatHint()
    override fun consumePendingRepeatHint(): Boolean = progressionRepo.consumePendingRepeatHint()
    override fun setPendingSectionUnlocked(sectionId: Int) = progressionRepo.setPendingSectionUnlocked(sectionId)
    override fun consumePendingSectionUnlocked(): Int = progressionRepo.consumePendingSectionUnlocked()
    override fun consumePendingStreakAnimation(): Boolean = progressionRepo.consumePendingStreakAnimation()

    // ── Streaks ───────────────────────────────────────────────────────────────
    override fun getLevelStreak(): Int = streakRepo.getLevelStreak()
    override fun incrementLevelStreak() = streakRepo.incrementLevelStreak()
    override fun resetLevelStreak() = streakRepo.resetLevelStreak()
    override fun getBestLevelStreak(): Int = streakRepo.getBestLevelStreak()
    override fun updateBestLevelStreak(current: Int): Boolean = streakRepo.updateBestLevelStreak(current)
    override fun reconcileBestStreaks() = streakRepo.reconcileBestStreaks()
    override fun hasPlayedToday(): Boolean = streakRepo.hasPlayedToday()
    override fun getCurrentDayStreak(): Int = streakRepo.getCurrentDayStreak()
    override fun getBestDayStreak(): Int = streakRepo.getBestDayStreak()
    override fun updateDayStreak() = streakRepo.updateDayStreak()

    // ── Best times ────────────────────────────────────────────────────────────
    override fun getBestTime(sectionId: Int, riddleType: String): Long = achievementRepo.getBestTime(sectionId, riddleType)
    override fun updateBestTime(sectionId: Int, riddleType: String, elapsedMs: Long): Boolean = achievementRepo.updateBestTime(sectionId, riddleType, elapsedMs)
    override suspend fun getBestTimeOverall(riddleType: String, sectionRepo: SectionRepository, groupsRepo: VersesGroupsRepository): BestTimeEntry? = achievementRepo.getBestTimeOverall(riddleType, sectionRepo, groupsRepo)

    // ── Achievement tallies ───────────────────────────────────────────────────
    override fun getTotalReviewedVerses(): Int = achievementRepo.getTotalReviewedVerses()
    override fun addTotalReviewedVerses(count: Int) = achievementRepo.addTotalReviewedVerses(count)
    override fun getTotalAloudRepeats(): Int = achievementRepo.getTotalAloudRepeats()
    override fun incrementTotalAloudRepeats() = achievementRepo.incrementTotalAloudRepeats()

    // ── Reset ─────────────────────────────────────────────────────────────────
    override fun clearAllProgress() = progressionRepo.clearAllProgress()
}
