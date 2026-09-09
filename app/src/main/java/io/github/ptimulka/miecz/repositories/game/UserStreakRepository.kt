package io.github.ptimulka.miecz.repositories.game

import io.github.ptimulka.miecz.repositories.StreakRepository
import io.github.ptimulka.miecz.repositories.core.UserProgressStore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserStreakRepository @Inject constructor(
    private val store: UserProgressStore,
    private val currentTimeProvider: () -> Long
) : StreakRepository {

    override fun getLevelStreak(): Int = store.latest.levelStreak

    override fun incrementLevelStreak() {
        store.update { it.toBuilder().setLevelStreak(it.levelStreak + 1).build() }
    }

    override fun resetLevelStreak() {
        store.update { it.toBuilder().setLevelStreak(0).build() }
    }

    override fun getBestLevelStreak(): Int = store.latest.bestLevelStreak

    override fun updateBestLevelStreak(current: Int): Boolean {
        val best = getBestLevelStreak()
        return if (current > best) {
            store.update { it.toBuilder().setBestLevelStreak(current).build() }
            true
        } else false
    }

    override fun reconcileBestStreaks() {
        store.update { user ->
            val builder = user.toBuilder()
            if (user.levelStreak > user.bestLevelStreak) builder.setBestLevelStreak(user.levelStreak)
            if (user.dayStreak > user.bestDayStreak) builder.setBestDayStreak(user.dayStreak)
            builder.build()
        }
    }

    override fun hasPlayedToday(): Boolean = store.latest.lastPlayedDate == todayString()

    override fun getCurrentDayStreak(): Int {
        val lastPlayed = store.latest.lastPlayedDate
        return if (lastPlayed == todayString() || lastPlayed == yesterdayString()) {
            store.latest.dayStreak
        } else {
            0
        }
    }

    override fun getBestDayStreak(): Int = store.latest.bestDayStreak

    override fun updateDayStreak() {
        val today = todayString()
        val lastPlayed = store.latest.lastPlayedDate
        if (lastPlayed == today) return

        val yesterday = yesterdayString()

        store.update { user ->
            val newStreak = if (lastPlayed == yesterday) user.dayStreak + 1 else 1
            val newBest = maxOf(newStreak, user.bestDayStreak)
            user.toBuilder()
                .setLastPlayedDate(today)
                .setDayStreak(newStreak)
                .setBestDayStreak(newBest)
                .setPendingStreakAnimation(true)
                .build()
        }
    }

    private fun todayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(currentTimeProvider()))
    }

    private fun yesterdayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.timeInMillis = currentTimeProvider()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return sdf.format(cal.time)
    }
}
