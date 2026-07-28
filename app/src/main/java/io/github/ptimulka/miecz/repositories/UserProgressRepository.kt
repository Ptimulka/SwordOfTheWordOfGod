package io.github.ptimulka.miecz.repositories

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar

class UserProgressRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_progress", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CURRENT_SECTION = "current_section"
        private const val KEY_LEVEL_FINISHED_PREFIX = "level_finished_"
        private const val KEY_SIGLA_FINISHED_PREFIX = "sigla_finished_"
        private const val KEY_VERSE_FINISHED_PREFIX = "verse_finished_"
        private const val KEY_RETENTION_PREFIX = "retention_"
        private const val KEY_CONNECT_DONE_DATE_PREFIX = "connect_done_date_"
        private const val RETENTION_GAIN_PER_CONNECT = 2
        private const val RETENTION_GAIN_PER_STANDARD_LEVEL = 3
        private const val MAX_RETENTION = 100
        private const val KEY_VERSE_REPEAT_COUNT_PREFIX = "verse_repeat_count_"
        private const val KEY_VERSE_REPEAT_DATE_PREFIX = "verse_repeat_date_"
        const val MAX_VERSE_REPEATS_PER_DAY = 10
        private const val KEY_RETENTION_DECAY_DATE = "retention_decay_date"
        private const val RETENTION_DAILY_DECAY = 5
        private const val KEY_CUSTOM_SECTION_PREFIX = "custom_section_"
        private const val KEY_CUSTOM_SECTIONS_COUNT = "custom_sections_count"
        private const val KEY_REPEAT_SECTION_INDEX = "repeat_section_index"
        private const val KEY_REPEAT_VERSE_INDEX = "repeat_verse_index"
        private const val KEY_SHIELDS_COUNT = "shields_count"
        const val MAX_SHIELDS = 5
        private const val MIN_SHIELDS = 0
        private const val KEY_LAST_SHIELD_UPDATE = "last_shield_update_time"
        private const val SHIELD_REGEN_TIME_MS = 30 * 60 * 1000L // 30 minutes in milliseconds
        private const val KEY_PENDING_REPEAT_HINT = "pending_repeat_hint"
        private const val KEY_PENDING_SECTION_UNLOCKED = "pending_section_unlocked"
        private const val KEY_BEST_TIME_PREFIX = "best_time_"
        private const val KEY_LEVEL_STREAK = "level_streak"
        private const val KEY_BEST_LEVEL_STREAK = "best_level_streak"
        private const val KEY_LAST_PLAYED_DATE = "last_played_date"
        private const val KEY_DAY_STREAK = "day_streak"
        private const val KEY_BEST_DAY_STREAK = "best_day_streak"
        private const val KEY_TOTAL_REVIEWED_VERSES = "total_reviewed_verses"
        private const val KEY_TOTAL_ALOUD_REPEATS = "total_aloud_repeats"
    }

    fun getCurrentSection(): Int {
        return prefs.getInt(KEY_CURRENT_SECTION, 1)
    }

    fun setCurrentSection(sectionId: Int) {
        prefs.edit().putInt(KEY_CURRENT_SECTION, sectionId).apply()
    }

    fun isLevelFinished(sectionId: Int, level: Int): Boolean {
        return prefs.getBoolean("${KEY_LEVEL_FINISHED_PREFIX}${sectionId}_$level", false)
    }

    fun setLevelFinished(sectionId: Int, level: Int, finished: Boolean) {
        prefs.edit().putBoolean("${KEY_LEVEL_FINISHED_PREFIX}${sectionId}_$level", finished).apply()
    }

    fun isSiglaFinished(sectionId: Int): Boolean {
        return prefs.getBoolean("$KEY_SIGLA_FINISHED_PREFIX$sectionId", false)
    }

    fun setSiglaFinished(sectionId: Int, finished: Boolean) {
        prefs.edit().putBoolean("$KEY_SIGLA_FINISHED_PREFIX$sectionId", finished).apply()
    }

    fun isVerseFinished(sectionId: Int): Boolean {
        return prefs.getBoolean("$KEY_VERSE_FINISHED_PREFIX$sectionId", false)
    }

    fun setVerseFinished(sectionId: Int, finished: Boolean) {
        prefs.edit().putBoolean("$KEY_VERSE_FINISHED_PREFIX$sectionId", finished).apply()
    }

    fun areSpecialChallengesFinished(sectionId: Int): Boolean {
        return isSiglaFinished(sectionId) && isVerseFinished(sectionId)
    }

    fun getRetention(sectionId: Int): Int {
        return prefs.getInt("$KEY_RETENTION_PREFIX$sectionId", 0)
    }

    fun setRetention(sectionId: Int, retention: Int) {
        prefs.edit().putInt("$KEY_RETENTION_PREFIX$sectionId", retention).apply()
    }

    /** Adds [amount] retention to the section (capped at 100) and returns how much was actually gained. */
    fun addRetention(sectionId: Int, amount: Int): Int {
        val old = getRetention(sectionId)
        val new = (old + amount).coerceIn(0, MAX_RETENTION)
        if (new != old) setRetention(sectionId, new)
        return new - old
    }

    /**
     * Applies a daily retention decay of RETENTION_DAILY_DECAY% per elapsed day to every section
     * (floored at 0). Idempotent within a day. Finished sections still display 100% regardless.
     */
    fun applyDailyRetentionDecay() {
        val today = todayString()
        val last = prefs.getString(KEY_RETENTION_DECAY_DATE, null)
        if (last == null || last == today) {
            prefs.edit().putString(KEY_RETENTION_DECAY_DATE, today).apply()
            return
        }
        val days = daysBetween(last, today)
        if (days <= 0) {
            prefs.edit().putString(KEY_RETENTION_DECAY_DATE, today).apply()
            return
        }
        val decay = RETENTION_DAILY_DECAY * days
        val editor = prefs.edit()
        for ((key, value) in prefs.all) {
            if (key.startsWith(KEY_RETENTION_PREFIX) && value is Int) {
                editor.putInt(key, (value - decay).coerceAtLeast(0))
            }
        }
        editor.putString(KEY_RETENTION_DECAY_DATE, today)
        editor.apply()
    }

    private fun daysBetween(from: String, to: String): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val f = sdf.parse(from)?.time ?: return 0
            val t = sdf.parse(to)?.time ?: return 0
            ((t - f) / (24L * 60 * 60 * 1000)).toInt()
        } catch (_: Exception) {
            0
        }
    }

    private fun connectDoneDateKey(sectionId: Int, riddleType: String) =
        "$KEY_CONNECT_DONE_DATE_PREFIX${sectionId}_$riddleType"

    /** True if the given connect level (parts/pairs) was already completed today for this section. */
    fun isConnectDoneToday(sectionId: Int, riddleType: String): Boolean {
        return prefs.getString(connectDoneDateKey(sectionId, riddleType), "") == todayString()
    }

    /**
     * Awards retention for finishing a connect parts/pairs level, but only the first time per day
     * for each section + riddle type. Returns how much retention was actually gained (0 if already
     * done today or already at the cap).
     */
    fun awardRetentionForConnectLevel(sectionId: Int, riddleType: String): Int {
        if (isConnectDoneToday(sectionId, riddleType)) return 0
        prefs.edit().putString(connectDoneDateKey(sectionId, riddleType), todayString()).apply()
        return addRetention(sectionId, RETENTION_GAIN_PER_CONNECT)
    }

    /**
     * Awards retention for finishing a standard level (1-12), but only for its very first ever
     * completion. Must be called BEFORE the level is marked finished. Returns how much retention
     * was actually gained (0 if the level was already finished before, or already at the cap).
     */
    fun awardRetentionForStandardLevel(sectionId: Int, levelNumber: Int): Int {
        if (isLevelFinished(sectionId, levelNumber)) return 0
        return addRetention(sectionId, RETENTION_GAIN_PER_STANDARD_LEVEL)
    }

    /** How many times the given verse in the given section was repeated today (0..MAX_VERSE_REPEATS_PER_DAY). */
    fun getVerseRepeatCountToday(sectionId: Int, verseIndex: Int): Int {
        val dateKey = "$KEY_VERSE_REPEAT_DATE_PREFIX${sectionId}_$verseIndex"
        if (prefs.getString(dateKey, "") != todayString()) return 0
        return prefs.getInt("$KEY_VERSE_REPEAT_COUNT_PREFIX${sectionId}_$verseIndex", 0)
    }

    /**
     * Retention (%) a single verse contributes for a given number of repeats today:
     * 5 repeats → 1%, 8 → 2%, 10 → 3%. Max 3% per verse (→ up to 30% per 10-verse section per day).
     */
    fun retentionContributionForRepeats(count: Int): Int = when {
        count >= 10 -> 3
        count >= 8 -> 2
        count >= 5 -> 1
        else -> 0
    }

    /** Increments today's repeat count for the verse (capped at MAX_VERSE_REPEATS_PER_DAY) and returns the new value. */
    fun incrementVerseRepeatToday(sectionId: Int, verseIndex: Int): Int {
        val current = getVerseRepeatCountToday(sectionId, verseIndex)
        val next = (current + 1).coerceAtMost(MAX_VERSE_REPEATS_PER_DAY)
        // Award the retention delta when a repeat threshold (5/8/10) is crossed
        val delta = retentionContributionForRepeats(next) - retentionContributionForRepeats(current)
        val editor = prefs.edit()
            .putString("$KEY_VERSE_REPEAT_DATE_PREFIX${sectionId}_$verseIndex", todayString())
            .putInt("$KEY_VERSE_REPEAT_COUNT_PREFIX${sectionId}_$verseIndex", next)
        if (delta > 0) {
            val newRetention = (getRetention(sectionId) + delta).coerceAtMost(MAX_RETENTION)
            editor.putInt("$KEY_RETENTION_PREFIX$sectionId", newRetention)
        }
        editor.apply()
        return next
    }

    fun saveCustomSection(sectionId: Int, groupId1: Int, groupId2: Int) {
        prefs.edit()
            .putString("$KEY_CUSTOM_SECTION_PREFIX$sectionId", "$groupId1,$groupId2")
            .putInt(KEY_CUSTOM_SECTIONS_COUNT, getCustomSectionsCount().coerceAtLeast(sectionId - 4)) // Assuming first custom is 5
            .apply()
    }

    fun getCustomSectionGroups(sectionId: Int): Pair<Int, Int>? {
        val data = prefs.getString("$KEY_CUSTOM_SECTION_PREFIX$sectionId", null) ?: return null
        val parts = data.split(",")
        return if (parts.size == 2) {
            Pair(parts[0].toInt(), parts[1].toInt())
        } else null
    }

    fun getCustomSectionsCount(): Int {
        return prefs.getInt(KEY_CUSTOM_SECTIONS_COUNT, 0)
    }

    fun getAllUsedGroupIds(): Set<Int> {
        val usedIds = mutableSetOf<Int>()
        val count = getCustomSectionsCount()
        for (i in 1..count) {
            val sectionId = 5 + i - 1
            getCustomSectionGroups(sectionId)?.let {
                usedIds.add(it.first)
                usedIds.add(it.second)
            }
        }
        return usedIds
    }

    fun getRepeatSectionIndex(): Int {
        return prefs.getInt(KEY_REPEAT_SECTION_INDEX, 0)
    }

    fun getRepeatVerseIndex(): Int {
        return prefs.getInt(KEY_REPEAT_VERSE_INDEX, 0)
    }

    fun saveRepeatProgress(sectionIndex: Int, verseIndex: Int) {
        prefs.edit()
            .putInt(KEY_REPEAT_SECTION_INDEX, sectionIndex)
            .putInt(KEY_REPEAT_VERSE_INDEX, verseIndex)
            .apply()
    }

    fun getShieldsCount(): Int {
        return prefs.getInt(KEY_SHIELDS_COUNT, MAX_SHIELDS)
    }

    fun setShieldsCount(count: Int) {
        val validatedCount = count.coerceIn(MIN_SHIELDS, MAX_SHIELDS)
        prefs.edit().putInt(KEY_SHIELDS_COUNT, validatedCount).apply()
    }

    fun decreaseShields(): Int {
        val current = getShieldsCount()
        if(current == MAX_SHIELDS) {
            saveShieldUpdateTime(System.currentTimeMillis())
        }
        val next = (current - 1).coerceAtLeast(MIN_SHIELDS)
        setShieldsCount(next)
        return next
    }

    fun increaseShields(): Int {
        val current = getShieldsCount()
        val next = (current + 1).coerceAtMost(MAX_SHIELDS)
        setShieldsCount(next)
        return next
    }

    fun getLastShieldUpdateTime(): Long {
        return prefs.getLong(KEY_LAST_SHIELD_UPDATE, 0L)
    }

    private fun saveShieldUpdateTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_SHIELD_UPDATE, time).apply()
    }

    fun refreshShields(): Int {
        val currentShields = getShieldsCount()
        if (currentShields >= MAX_SHIELDS) {
            return MAX_SHIELDS
        }

        val lastUpdate = getLastShieldUpdateTime()
        val currentTime = System.currentTimeMillis()

        if (lastUpdate == 0L) {
            saveShieldUpdateTime(currentTime)
            return currentShields
        }

        val elapsed = currentTime - lastUpdate

        if (elapsed >= SHIELD_REGEN_TIME_MS) {
            val shieldsToAdd = (elapsed / SHIELD_REGEN_TIME_MS).toInt()
            val newCount = (currentShields + shieldsToAdd).coerceAtMost(MAX_SHIELDS)

            setShieldsCount(newCount)

            if (newCount >= MAX_SHIELDS) {
                prefs.edit().remove(KEY_LAST_SHIELD_UPDATE).apply()
            } else {
                // Save the timestamp of the last "full" regeneration,
                // preserving the remainder (e.g., if 35 min elapsed, 5 min is carried over to the next shield)
                val newUpdateTime = lastUpdate + (shieldsToAdd * SHIELD_REGEN_TIME_MS)
                saveShieldUpdateTime(newUpdateTime)
            }
            return newCount
        }
        return currentShields
    }

    fun setPendingRepeatHint() {
        prefs.edit().putBoolean(KEY_PENDING_REPEAT_HINT, true).apply()
    }

    fun consumePendingRepeatHint(): Boolean {
        val has = prefs.getBoolean(KEY_PENDING_REPEAT_HINT, false)
        if (has) prefs.edit().remove(KEY_PENDING_REPEAT_HINT).apply()
        return has
    }

    /** Remembers that a new section was just unlocked, so the levels screen can congratulate the user. */
    fun setPendingSectionUnlocked(sectionId: Int) {
        prefs.edit().putInt(KEY_PENDING_SECTION_UNLOCKED, sectionId).apply()
    }

    /** Returns the just-unlocked section id (and clears it), or -1 if none is pending. */
    fun consumePendingSectionUnlocked(): Int {
        val id = prefs.getInt(KEY_PENDING_SECTION_UNLOCKED, -1)
        if (id != -1) prefs.edit().remove(KEY_PENDING_SECTION_UNLOCKED).apply()
        return id
    }

    fun getTimeToNextShield(): Long {
        if (getShieldsCount() >= MAX_SHIELDS) return 0L

        val lastUpdate = getLastShieldUpdateTime()
        if (lastUpdate == 0L) return 0L

        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastUpdate

        if (elapsed >= SHIELD_REGEN_TIME_MS) return 0L

        val remaining = SHIELD_REGEN_TIME_MS - (elapsed % SHIELD_REGEN_TIME_MS)
        return remaining.coerceAtLeast(0L)
    }

    fun getLevelStreak(): Int = prefs.getInt(KEY_LEVEL_STREAK, 0)

    fun incrementLevelStreak() {
        prefs.edit().putInt(KEY_LEVEL_STREAK, getLevelStreak() + 1).apply()
    }

    fun resetLevelStreak() {
        prefs.edit().putInt(KEY_LEVEL_STREAK, 0).apply()
    }

    fun getBestLevelStreak(): Int = prefs.getInt(KEY_BEST_LEVEL_STREAK, 0)

    // Returns true if this is a new record
    fun updateBestLevelStreak(current: Int): Boolean {
        val best = getBestLevelStreak()
        return if (current > best) {
            prefs.edit().putInt(KEY_BEST_LEVEL_STREAK, current).apply()
            true
        } else false
    }

    /** Repairs the invariant "best >= current" for both streaks (e.g. for legacy data). */
    fun reconcileBestStreaks() {
        val editor = prefs.edit()
        if (getLevelStreak() > getBestLevelStreak()) {
            editor.putInt(KEY_BEST_LEVEL_STREAK, getLevelStreak())
        }
        if (getCurrentDayStreak() > getBestDayStreak()) {
            editor.putInt(KEY_BEST_DAY_STREAK, getCurrentDayStreak())
        }
        editor.apply()
    }

    private fun todayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    fun hasPlayedToday(): Boolean = prefs.getString(KEY_LAST_PLAYED_DATE, "") == todayString()

    fun getCurrentDayStreak(): Int = prefs.getInt(KEY_DAY_STREAK, 0)

    fun getBestDayStreak(): Int = prefs.getInt(KEY_BEST_DAY_STREAK, 0)

    fun updateDayStreak() {
        val today = todayString()
        val lastPlayed = prefs.getString(KEY_LAST_PLAYED_DATE, "") ?: ""
        if (lastPlayed == today) return

        val sdf = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = sdf.format(cal.time)

        val newStreak = if (lastPlayed == yesterday) getCurrentDayStreak() + 1 else 1
        val newBest = maxOf(newStreak, getBestDayStreak())
        prefs.edit()
            .putString(KEY_LAST_PLAYED_DATE, today)
            .putInt(KEY_DAY_STREAK, newStreak)
            .putInt(KEY_BEST_DAY_STREAK, newBest)
            .apply()
    }

    fun getBestTime(sectionId: Int, riddleType: String): Long {
        return prefs.getLong("${KEY_BEST_TIME_PREFIX}${sectionId}_$riddleType", -1L)
    }

    // Returns true if this is a new record
    fun updateBestTime(sectionId: Int, riddleType: String, elapsedMs: Long): Boolean {
        val current = getBestTime(sectionId, riddleType)
        return if (current < 0 || elapsedMs < current) {
            prefs.edit().putLong("${KEY_BEST_TIME_PREFIX}${sectionId}_$riddleType", elapsedMs).apply()
            true
        } else false
    }

    data class BestTimeEntry(val sectionId: Int, val sectionName: String, val timeMs: Long)

    fun getBestTimeOverall(riddleType: String, context: android.content.Context): BestTimeEntry? {
        val prefix = KEY_BEST_TIME_PREFIX
        val suffix = "_$riddleType"
        val sectionRepo = SectionRepository(context)
        val verseGroups by lazy { VersesGroupsRepository(context).loadVerseGroups().associateBy { it.id } }
        return prefs.all
            .filter { (key, _) -> key.startsWith(prefix) && key.endsWith(suffix) }
            .mapNotNull { (key, value) ->
                val sectionId = key.removePrefix(prefix).removeSuffix(suffix).toIntOrNull() ?: return@mapNotNull null
                val timeMs = (value as? Long) ?: return@mapNotNull null
                val sectionName = sectionRepo.loadSectionName(sectionId)
                    ?: getCustomSectionGroups(sectionId)?.let { (id1, id2) ->
                        val g1 = verseGroups[id1]?.name ?: id1.toString()
                        val g2 = verseGroups[id2]?.name ?: id2.toString()
                        "$g1 i $g2"
                    } ?: "$sectionId"
                BestTimeEntry(sectionId, sectionName, timeMs)
            }
            .minByOrNull { it.timeMs }
    }

    // ── Achievement tallies ───────────────────────────────────────────────────

    // Total reviewed verses (lifetime, from Review)
    fun getTotalReviewedVerses(): Int = prefs.getInt(KEY_TOTAL_REVIEWED_VERSES, 0)

    fun addTotalReviewedVerses(count: Int) {
        if (count <= 0) return
        prefs.edit().putInt(KEY_TOTAL_REVIEWED_VERSES, getTotalReviewedVerses() + count).apply()
    }

    // Total aloud repetitions (lifetime, from "Wymawiaj wersety")
    fun getTotalAloudRepeats(): Int = prefs.getInt(KEY_TOTAL_ALOUD_REPEATS, 0)

    fun incrementTotalAloudRepeats() {
        prefs.edit().putInt(KEY_TOTAL_ALOUD_REPEATS, getTotalAloudRepeats() + 1).apply()
    }

    /**
     * Wipes every stored progress value: current section, finished levels and challenges,
     * retention, streaks, best times, shields and achievement tallies.
     */
    fun clearAllProgress() {
        prefs.edit().clear().apply()
    }

    // Count of finished standard levels + special challenges (sigla/verse)
    fun getFinishedLevelsCount(): Int {
        var count = 0
        for ((key, value) in prefs.all) {
            if (value == true &&
                (key.startsWith(KEY_LEVEL_FINISHED_PREFIX) ||
                    key.startsWith(KEY_SIGLA_FINISHED_PREFIX) ||
                    key.startsWith(KEY_VERSE_FINISHED_PREFIX))
            ) {
                count++
            }
        }
        return count
    }
}
