package io.github.ptimulka.miecz.data

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Central repository for global constants to avoid magic numbers.
 */
object Constants {
    // --- Progression ---
    const val BASE_SECTIONS_COUNT = 4
    const val LEVELS_PER_SECTION = 12
    const val CUSTOM_SECTION_START_ID = 5
    const val VERSES_PER_SECTION = 10
    const val SPECIAL_CHALLENGE_RIDDLES_COUNT = 10
    const val RANDOM_VERSE_COUNTDOWN_SECONDS = 10
    
    // --- Retention ---
    const val MAX_RETENTION = 100
    const val RETENTION_DECAY_PER_DAY = 5
    const val RETENTION_REWARD_STANDARD_LEVEL = 3
    const val RETENTION_REWARD_CONNECT_LEVEL = 2
    
    // --- Rays ---
    const val RAYS_MIN_RETENTION = 4
    const val RAYS_STEP_RETENTION = 8
    
    // --- Verse Repeats ---
    const val LONG_VERSE_WORD_THRESHOLD = 20
    const val MAX_VERSE_REPEATS_PER_DAY = 10
    const val REPEAT_THRESHOLD_LOW = 5
    const val REPEAT_THRESHOLD_MEDIUM = 8
    const val REPEAT_THRESHOLD_HIGH = 10
    const val REPEAT_REWARD_LOW = 1
    const val REPEAT_REWARD_MEDIUM = 2
    const val REPEAT_REWARD_HIGH = 3
    const val REPEAT_REWARD_LONG_LOW = 2
    const val REPEAT_REWARD_LONG_MEDIUM = 3
    const val REPEAT_REWARD_LONG_HIGH = 4
    
    // --- Timeouts & Delays ---
    val SHIELD_REFRESH_DELAY = 1.minutes
    val SHIELD_INFO_REFRESH_INTERVAL = 1.seconds
    const val SHIELD_INFO_AUTO_HIDE_ITERATIONS = 7
    val LAMP_INFO_AUTO_HIDE_DELAY = 7.seconds
    val REPEAT_HINT_AUTO_HIDE_DELAY = 30.seconds
    val REVIEW_REFRESH_DELAY = 10.seconds
    val MATCH_ANIMATION_DURATION = 300.milliseconds
    val WRONG_PAIR_LOCKOUT_DURATION = 2.seconds
    val COUNTDOWN_TICK_DURATION = 1.seconds
    val SHIELD_REGEN_TIME = 30.minutes
    val DAY_DURATION = 1.days
    
    // --- Review Rewards ---
    const val REVIEW_BONUS_COUNT_THRESHOLD = 10
    const val REVIEW_REWARD_DEFAULT = 1
    const val REVIEW_REWARD_BONUS = 2
    
    // --- Selection ---
    const val MAX_CUSTOM_SECTION_GROUPS = 2
    
    // --- Riddle Config ---
    const val MULTI_QUIZ_WRONG_ANSWERS_COUNT = 3
}
