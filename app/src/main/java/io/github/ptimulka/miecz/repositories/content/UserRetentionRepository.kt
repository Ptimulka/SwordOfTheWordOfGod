package io.github.ptimulka.miecz.repositories.content

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
        val new = (old + amount).coerceIn(0, 100)
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
        val decay = 5 * days
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
            ((t - f) / (24L * 60 * 60 * 1000)).toInt()
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
        if (isConnectDoneToday(sectionId, riddleType)) return 0
        store.updateSection(sectionId) {
            if (riddleType == "CONNECT_PARTS") it.setConnectDoneDateParts(todayString())
            else it.setConnectDoneDatePairs(todayString())
        }
        return addRetention(sectionId, 2)
    }

    override fun awardRetentionForStandardLevel(sectionId: Int, levelNumber: Int): Int {
        val finished = store.latest.sectionsMap[sectionId]?.finishedLevelsMap?.get(levelNumber) ?: false
        if (finished) return 0
        return addRetention(sectionId, 3)
    }

    override fun getVerseRepeatCountToday(sectionId: Int, verseIndex: Int): Int {
        val section = store.latest.sectionsMap[sectionId] ?: return 0
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
        val next = (current + 1).coerceAtMost(10)
        val delta = retentionContributionForRepeats(next) - retentionContributionForRepeats(current)
        val today = todayString()
        
        store.updateSection(sectionId) { s ->
            if (s.verseRepeatDate != today) {
                s.clearVerseRepeatCountsToday()
                s.setVerseRepeatDate(today)
            }
            s.putVerseRepeatCountsToday(verseIndex, next)
            if (delta > 0) {
                s.setRetention((s.retention + delta).coerceAtMost(100))
            }
        }
        return next
    }

    private fun todayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(currentTimeProvider()))
    }
}
