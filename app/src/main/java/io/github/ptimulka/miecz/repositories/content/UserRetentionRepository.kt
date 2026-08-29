package io.github.ptimulka.miecz.repositories.content

import io.github.ptimulka.miecz.data.Constants
import io.github.ptimulka.miecz.repositories.RetentionRepository
import io.github.ptimulka.miecz.repositories.core.UserProgressStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRetentionRepository @Inject constructor(
    private val store: UserProgressStore,
    private val currentTimeProvider: () -> Long
) : RetentionRepository {

    override fun getRetention(sectionId: Int): Int {
        return store.latest.sectionsMap[sectionId]?.retention ?: 0
    }

    override fun setRetention(sectionId: Int, retention: Int) {
        store.updateSection(sectionId) { it.setRetention(retention) }
    }

    override fun addRetention(sectionId: Int, amount: Int): Int {
        val old = getRetention(sectionId)
        val new = (old + amount).coerceIn(0, Constants.MAX_RETENTION)
        if (new != old) setRetention(sectionId, new)
        return new - old
    }

    override fun applyDailyRetentionDecay() {
        val today = todayString()
        val last = store.latest.retentionDecayDate.takeIf { it.isNotEmpty() }
        if (last == null || last == today) {
            store.update { it.toBuilder().setRetentionDecayDate(today).build() }
            return
        }
        val days = daysBetween(last, today)
        if (days <= 0) {
            store.update { it.toBuilder().setRetentionDecayDate(today).build() }
            return
        }
        val decay = Constants.RETENTION_DECAY_PER_DAY * days
        store.update { user ->
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
            ((t - f) / Constants.DAY_DURATION.inWholeMilliseconds).toInt()
        } catch (_: Exception) {
            0
        }
    }

    override fun isConnectDoneToday(sectionId: Int, riddleType: String): Boolean {
        val section = store.latest.sectionsMap[sectionId] ?: return false
        return if (riddleType == "CONNECT_PARTS") {
            section.connectDoneDateParts == todayString()
        } else {
            section.connectDoneDatePairs == todayString()
        }
    }

    override fun awardRetentionForConnectLevel(sectionId: Int, riddleType: String): Int {
        if (isConnectDoneToday(sectionId, riddleType) || areChallengesFinished(sectionId)) return 0
        store.updateSection(sectionId) {
            if (riddleType == "CONNECT_PARTS") it.setConnectDoneDateParts(todayString())
            else it.setConnectDoneDatePairs(todayString())
        }
        return addRetention(sectionId, Constants.RETENTION_REWARD_CONNECT_LEVEL)
    }

    override fun awardRetentionForStandardLevel(sectionId: Int, levelNumber: Int): Int {
        val finished = store.latest.sectionsMap[sectionId]?.finishedLevelsMap?.get(levelNumber) ?: false
        if (finished || areChallengesFinished(sectionId)) return 0
        return addRetention(sectionId, Constants.RETENTION_REWARD_STANDARD_LEVEL)
    }

    override fun getVerseRepeatCountToday(sectionId: Int, verseIndex: Int): Int {
        val section = store.latest.sectionsMap[sectionId] ?: return 0
        if (section.verseRepeatDate != todayString()) return 0
        return section.verseRepeatCountsTodayMap[verseIndex] ?: 0
    }

    override fun retentionContributionForRepeats(count: Int): Int = when {
        count >= Constants.REPEAT_THRESHOLD_HIGH -> Constants.REPEAT_REWARD_HIGH
        count >= Constants.REPEAT_THRESHOLD_MEDIUM -> Constants.REPEAT_REWARD_MEDIUM
        count >= Constants.REPEAT_THRESHOLD_LOW -> Constants.REPEAT_REWARD_LOW
        else -> 0
    }

    override fun incrementVerseRepeatToday(sectionId: Int, verseIndex: Int): Int {
        val current = getVerseRepeatCountToday(sectionId, verseIndex)
        val next = (current + 1).coerceAtMost(Constants.MAX_VERSE_REPEATS_PER_DAY)
        val delta = retentionContributionForRepeats(next) - retentionContributionForRepeats(current)
        val today = todayString()
        
        val finished = areChallengesFinished(sectionId)

        store.updateSection(sectionId) { s ->
            if (s.verseRepeatDate != today) {
                s.clearVerseRepeatCountsToday()
                s.setVerseRepeatDate(today)
            }
            s.putVerseRepeatCountsToday(verseIndex, next)
            if (delta > 0 && !finished) {
                s.setRetention((s.retention + delta).coerceAtMost(Constants.MAX_RETENTION))
            }
        }
        return next
    }

    private fun areChallengesFinished(sectionId: Int): Boolean {
        val section = store.latest.sectionsMap[sectionId] ?: return false
        return section.siglaFinished && section.verseFinished
    }

    private fun todayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(currentTimeProvider()))
    }
}
