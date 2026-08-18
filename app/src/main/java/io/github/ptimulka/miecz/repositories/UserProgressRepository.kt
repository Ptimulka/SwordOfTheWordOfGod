package io.github.ptimulka.miecz.repositories

import androidx.datastore.core.DataStore
import io.github.ptimulka.miecz.data.SectionProgress
import io.github.ptimulka.miecz.data.UserProgress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class UserProgressRepository @Inject constructor(
    private val dataStore: DataStore<UserProgress>,
    private val currentTimeProvider: () -> Long,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProgressRepository {

    companion object {
        const val MAX_SHIELDS = 5
        private const val MIN_SHIELDS = 0
        private const val SHIELD_REGEN_TIME_MS = 30 * 60 * 1000L // 30 minutes in milliseconds
        
        const val MAX_VERSE_REPEATS_PER_DAY = 10
        private const val MAX_RETENTION = 100
        private const val RETENTION_DAILY_DECAY = 5
        private const val RETENTION_GAIN_PER_CONNECT = 2
        private const val RETENTION_GAIN_PER_STANDARD_LEVEL = 3
    }

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    
    @Volatile
    private var cachedProgress: UserProgress = runBlocking { dataStore.data.first() }

    init {
        dataStore.data.onEach { cachedProgress = it }.launchIn(scope)
    }

    private fun update(action: (UserProgress) -> UserProgress) {
        cachedProgress = action(cachedProgress)
        scope.launch {
            dataStore.updateData { action(it) }
        }
    }

    private fun updateSection(sectionId: Int, action: (SectionProgress.Builder) -> Unit) {
        update { user ->
            val sectionBuilder = user.sectionsMap[sectionId]?.toBuilder() ?: SectionProgress.newBuilder()
            action(sectionBuilder)
            user.toBuilder()
                .putSections(sectionId, sectionBuilder.build())
                .build()
        }
    }

    override fun getCurrentSection(): Int = cachedProgress.currentSectionId

    override fun setCurrentSection(sectionId: Int) {
        update { it.toBuilder().setCurrentSectionId(sectionId).build() }
    }

    override fun isLevelFinished(sectionId: Int, level: Int): Boolean {
        return cachedProgress.sectionsMap[sectionId]?.finishedLevelsMap?.get(level) ?: false
    }

    override fun setLevelFinished(sectionId: Int, level: Int, finished: Boolean) {
        updateSection(sectionId) { it.putFinishedLevels(level, finished) }
    }

    override fun isSiglaFinished(sectionId: Int): Boolean {
        return cachedProgress.sectionsMap[sectionId]?.siglaFinished ?: false
    }

    override fun setSiglaFinished(sectionId: Int, finished: Boolean) {
        updateSection(sectionId) { it.setSiglaFinished(finished) }
    }

    override fun isVerseFinished(sectionId: Int): Boolean {
        return cachedProgress.sectionsMap[sectionId]?.verseFinished ?: false
    }

    override fun setVerseFinished(sectionId: Int, finished: Boolean) {
        updateSection(sectionId) { it.setVerseFinished(finished) }
    }

    override fun areSpecialChallengesFinished(sectionId: Int): Boolean {
        return isSiglaFinished(sectionId) && isVerseFinished(sectionId)
    }

    override fun getRetention(sectionId: Int): Int {
        return cachedProgress.sectionsMap[sectionId]?.retention ?: 0
    }

    override fun setRetention(sectionId: Int, retention: Int) {
        updateSection(sectionId) { it.setRetention(retention) }
    }

    override fun addRetention(sectionId: Int, amount: Int): Int {
        val old = getRetention(sectionId)
        val new = (old + amount).coerceIn(0, MAX_RETENTION)
        if (new != old) setRetention(sectionId, new)
        return new - old
    }

    override fun applyDailyRetentionDecay() {
        val today = todayString()
        val last = cachedProgress.retentionDecayDate.takeIf { it.isNotEmpty() }
        if (last == null || last == today) {
            update { it.toBuilder().setRetentionDecayDate(today).build() }
            return
        }
        val days = daysBetween(last, today)
        if (days <= 0) {
            update { it.toBuilder().setRetentionDecayDate(today).build() }
            return
        }
        val decay = RETENTION_DAILY_DECAY * days
        update { user ->
            val builder = user.toBuilder()
            user.sectionsMap.forEach { (id, section) ->
                builder.putSections(id, section.toBuilder().setRetention((section.retention - decay).coerceAtLeast(0)).build())
            }
            builder.setRetentionDecayDate(today).build()
        }
    }

    private fun daysBetween(from: String, to: String): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val f = sdf.parse(from)?.time ?: return 0
            val t = sdf.parse(to)?.time ?: return 0
            ((t - f) / (24L * 60 * 60 * 1000)).toInt()
        } catch (_: Exception) {
            0
        }
    }

    override fun isConnectDoneToday(sectionId: Int, riddleType: String): Boolean {
        val section = cachedProgress.sectionsMap[sectionId] ?: return false
        return if (riddleType == "CONNECT_PARTS") {
            section.connectDoneDateParts == todayString()
        } else {
            section.connectDoneDatePairs == todayString()
        }
    }

    override fun awardRetentionForConnectLevel(sectionId: Int, riddleType: String): Int {
        if (isConnectDoneToday(sectionId, riddleType)) return 0
        updateSection(sectionId) {
            if (riddleType == "CONNECT_PARTS") it.setConnectDoneDateParts(todayString())
            else it.setConnectDoneDatePairs(todayString())
        }
        return addRetention(sectionId, RETENTION_GAIN_PER_CONNECT)
    }

    override fun awardRetentionForStandardLevel(sectionId: Int, levelNumber: Int): Int {
        if (isLevelFinished(sectionId, levelNumber)) return 0
        return addRetention(sectionId, RETENTION_GAIN_PER_STANDARD_LEVEL)
    }

    override fun getVerseRepeatCountToday(sectionId: Int, verseIndex: Int): Int {
        val section = cachedProgress.sectionsMap[sectionId] ?: return 0
        if (section.verseRepeatDate != todayString()) return 0
        return section.verseRepeatCountsTodayMap[verseIndex] ?: 0
    }

    override fun retentionContributionForRepeats(count: Int): Int = when {
        count >= 10 -> 3
        count >= 8 -> 2
        count >= 5 -> 1
        else -> 0
    }

    override fun incrementVerseRepeatToday(sectionId: Int, verseIndex: Int): Int {
        val current = getVerseRepeatCountToday(sectionId, verseIndex)
        val next = (current + 1).coerceAtMost(MAX_VERSE_REPEATS_PER_DAY)
        val delta = retentionContributionForRepeats(next) - retentionContributionForRepeats(current)
        val today = todayString()
        
        updateSection(sectionId) { s ->
            if (s.verseRepeatDate != today) {
                s.clearVerseRepeatCountsToday()
                s.setVerseRepeatDate(today)
            }
            s.putVerseRepeatCountsToday(verseIndex, next)
            if (delta > 0) {
                s.setRetention((s.retention + delta).coerceAtMost(MAX_RETENTION))
            }
        }
        return next
    }

    override fun saveCustomSection(sectionId: Int, groupId1: Int, groupId2: Int) {
        update { it.toBuilder().putCustomSections(sectionId, "$groupId1,$groupId2").build() }
    }

    override fun getCustomSectionGroups(sectionId: Int): Pair<Int, Int>? {
        val data = cachedProgress.customSectionsMap[sectionId] ?: return null
        val parts = data.split(",")
        return if (parts.size == 2) {
            Pair(parts[0].toInt(), parts[1].toInt())
        } else null
    }

    override fun getCustomSectionsCount(): Int = cachedProgress.customSectionsCount

    override fun getAllUsedGroupIds(): Set<Int> {
        val usedIds = mutableSetOf<Int>()
        cachedProgress.customSectionsMap.values.forEach { data ->
            val parts = data.split(",")
            if (parts.size == 2) {
                usedIds.add(parts[0].toInt())
                usedIds.add(parts[1].toInt())
            }
        }
        return usedIds
    }

    override fun getMnemonicChoice(sectionId: Int, verseIndex: Int): String? {
        return cachedProgress.sectionsMap[sectionId]?.mnemonicChoicesMap?.get(verseIndex)
    }

    override fun saveMnemonicChoice(sectionId: Int, verseIndex: Int, choice: String) {
        updateSection(sectionId) { it.putMnemonicChoices(verseIndex, choice) }
    }

    override fun getRepeatSectionIndex(): Int = cachedProgress.repeatSectionIndex

    override fun getRepeatVerseIndex(): Int = cachedProgress.repeatVerseIndex

    override fun saveRepeatProgress(sectionIndex: Int, verseIndex: Int) {
        update { it.toBuilder().setRepeatSectionIndex(sectionIndex).setRepeatVerseIndex(verseIndex).build() }
    }

    override fun getShieldsCount(): Int = cachedProgress.shieldsCount

    override fun setShieldsCount(count: Int) {
        val validatedCount = count.coerceIn(MIN_SHIELDS, MAX_SHIELDS)
        update { it.toBuilder().setShieldsCount(validatedCount).build() }
    }

    override fun decreaseShields(): Int {
        val current = getShieldsCount()
        if (current == MAX_SHIELDS) {
            update { it.toBuilder().setLastShieldUpdateTime(currentTimeProvider()).build() }
        }
        val next = (current - 1).coerceAtLeast(MIN_SHIELDS)
        setShieldsCount(next)
        return next
    }

    override fun increaseShields(): Int {
        val current = getShieldsCount()
        val next = (current + 1).coerceAtMost(MAX_SHIELDS)
        setShieldsCount(next)
        return next
    }

    override fun getLastShieldUpdateTime(): Long = cachedProgress.lastShieldUpdateTime

    override fun refreshShields(): Int {
        val currentShields = getShieldsCount()
        if (currentShields >= MAX_SHIELDS) return MAX_SHIELDS

        val lastUpdate = getLastShieldUpdateTime()
        val currentTime = currentTimeProvider()

        if (lastUpdate == 0L) {
            update { it.toBuilder().setLastShieldUpdateTime(currentTime).build() }
            return currentShields
        }

        val elapsed = currentTime - lastUpdate

        if (elapsed >= SHIELD_REGEN_TIME_MS) {
            val shieldsToAdd = (elapsed / SHIELD_REGEN_TIME_MS).toInt()
            val newCount = (currentShields + shieldsToAdd).coerceAtMost(MAX_SHIELDS)

            update { user ->
                val builder = user.toBuilder().setShieldsCount(newCount)
                if (newCount >= MAX_SHIELDS) {
                    builder.setLastShieldUpdateTime(0)
                } else {
                    builder.setLastShieldUpdateTime(lastUpdate + (shieldsToAdd * SHIELD_REGEN_TIME_MS))
                }
                builder.build()
            }
            return newCount
        }
        return currentShields
    }

    override fun setPendingRepeatHint() {
        update { it.toBuilder().setPendingRepeatHint(true).build() }
    }

    override fun consumePendingRepeatHint(): Boolean {
        val has = cachedProgress.pendingRepeatHint
        if (has) update { it.toBuilder().setPendingRepeatHint(false).build() }
        return has
    }

    override fun setPendingSectionUnlocked(sectionId: Int) {
        update { it.toBuilder().setPendingSectionUnlocked(sectionId).build() }
    }

    override fun consumePendingSectionUnlocked(): Int {
        val id = cachedProgress.pendingSectionUnlocked
        if (id != 0) update { it.toBuilder().setPendingSectionUnlocked(0).build() }
        return if (id == 0) -1 else id
    }

    override fun getTimeToNextShield(): Long {
        if (getShieldsCount() >= MAX_SHIELDS) return 0L
        val lastUpdate = getLastShieldUpdateTime()
        if (lastUpdate == 0L) return 0L

        val elapsed = currentTimeProvider() - lastUpdate
        if (elapsed >= SHIELD_REGEN_TIME_MS) return 0L

        return (SHIELD_REGEN_TIME_MS - (elapsed % SHIELD_REGEN_TIME_MS)).coerceAtLeast(0L)
    }

    override fun getLevelStreak(): Int = cachedProgress.levelStreak

    override fun incrementLevelStreak() {
        update { it.toBuilder().setLevelStreak(it.levelStreak + 1).build() }
    }

    override fun resetLevelStreak() {
        update { it.toBuilder().setLevelStreak(0).build() }
    }

    override fun getBestLevelStreak(): Int = cachedProgress.bestLevelStreak

    override fun updateBestLevelStreak(current: Int): Boolean {
        val best = getBestLevelStreak()
        return if (current > best) {
            update { it.toBuilder().setBestLevelStreak(current).build() }
            true
        } else false
    }

    override fun reconcileBestStreaks() {
        update { user ->
            val builder = user.toBuilder()
            if (user.levelStreak > user.bestLevelStreak) builder.setBestLevelStreak(user.levelStreak)
            if (user.dayStreak > user.bestDayStreak) builder.setBestDayStreak(user.dayStreak)
            builder.build()
        }
    }

    private fun todayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(java.util.Date(currentTimeProvider()))
    }

    override fun hasPlayedToday(): Boolean = cachedProgress.lastPlayedDate == todayString()

    override fun getCurrentDayStreak(): Int = cachedProgress.dayStreak

    override fun getBestDayStreak(): Int = cachedProgress.bestDayStreak

    override fun updateDayStreak() {
        val today = todayString()
        val lastPlayed = cachedProgress.lastPlayedDate
        if (lastPlayed == today) return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = sdf.format(cal.time)

        update { user ->
            val newStreak = if (lastPlayed == yesterday) user.dayStreak + 1 else 1
            val newBest = maxOf(newStreak, user.bestDayStreak)
            user.toBuilder()
                .setLastPlayedDate(today)
                .setDayStreak(newStreak)
                .setBestDayStreak(newBest)
                .build()
        }
    }

    override fun getBestTime(sectionId: Int, riddleType: String): Long {
        return cachedProgress.sectionsMap[sectionId]?.bestTimesMap?.get(riddleType) ?: -1L
    }

    override fun updateBestTime(sectionId: Int, riddleType: String, elapsedMs: Long): Boolean {
        val current = getBestTime(sectionId, riddleType)
        return if (current < 0 || elapsedMs < current) {
            updateSection(sectionId) { it.putBestTimes(riddleType, elapsedMs) }
            true
        } else false
    }

    override fun getBestTimeOverall(
        riddleType: String,
        sectionRepo: SectionRepository,
        groupsRepo: VersesGroupsRepository
    ): BestTimeEntry? {
        val verseGroups by lazy { groupsRepo.loadVerseGroups().associateBy { it.id } }
        return cachedProgress.sectionsMap.entries
            .mapNotNull { (id, section) ->
                val timeMs = section.bestTimesMap[riddleType] ?: return@mapNotNull null
                val sectionName = sectionRepo.loadSectionName(id)
                    ?: getCustomSectionGroups(id)?.let { (id1, id2) ->
                        val g1 = verseGroups[id1]?.name ?: id1.toString()
                        val g2 = verseGroups[id2]?.name ?: id2.toString()
                        "$g1 i $g2"
                    } ?: "$id"
                BestTimeEntry(id, sectionName, timeMs)
            }
            .minByOrNull { it.timeMs }
    }

    override fun getTotalReviewedVerses(): Int = cachedProgress.totalReviewedVerses

    override fun addTotalReviewedVerses(count: Int) {
        if (count <= 0) return
        update { it.toBuilder().setTotalReviewedVerses(it.totalReviewedVerses + count).build() }
    }

    override fun getTotalAloudRepeats(): Int = cachedProgress.totalAloudRepeats

    override fun incrementTotalAloudRepeats() {
        update { it.toBuilder().setTotalAloudRepeats(it.totalAloudRepeats + 1).build() }
    }

    override fun clearAllProgress() {
        update { UserProgressSerializer.defaultValue }
    }

    override fun getFinishedLevelsCount(): Int {
        var count = 0
        cachedProgress.sectionsMap.values.forEach { section ->
            count += section.finishedLevelsMap.values.count { it }
            if (section.siglaFinished) count++
            if (section.verseFinished) count++
        }
        return count
    }
}
