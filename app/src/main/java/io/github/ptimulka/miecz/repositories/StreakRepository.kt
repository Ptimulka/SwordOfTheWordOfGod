package io.github.ptimulka.miecz.repositories

interface StreakRepository {
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
}
