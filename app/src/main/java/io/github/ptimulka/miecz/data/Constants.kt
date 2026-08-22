package io.github.ptimulka.miecz.data

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
    
    // --- Retention ---
    const val MAX_RETENTION = 100
    const val RETENTION_DECAY_PER_DAY = 5
    const val RETENTION_REWARD_STANDARD_LEVEL = 3
    const val RETENTION_REWARD_CONNECT_LEVEL = 2
    
    // --- Rays ---
    const val RAYS_MIN_RETENTION = 4
    const val RAYS_STEP_RETENTION = 8
    
    // --- Verse Repeats ---
    const val MAX_VERSE_REPEATS_PER_DAY = 10
    const val REPEAT_THRESHOLD_LOW = 5
    const val REPEAT_THRESHOLD_MEDIUM = 8
    const val REPEAT_THRESHOLD_HIGH = 10
    const val REPEAT_REWARD_LOW = 1
    const val REPEAT_REWARD_MEDIUM = 2
    const val REPEAT_REWARD_HIGH = 3
    
    // --- Timeouts & Delays ---
    const val SHIELD_REFRESH_DELAY_MS = 60000L
    const val SHIELD_INFO_REFRESH_MS = 1000L
    const val SHIELD_INFO_AUTO_HIDE_ITERATIONS = 7
    const val LAMP_INFO_AUTO_HIDE_MS = 7000L
    const val REPEAT_HINT_AUTO_HIDE_MS = 30000L
    const val REVIEW_REFRESH_DELAY_MS = 10000L
    const val MATCH_ANIMATION_MS = 300L
    const val WRONG_PAIR_LOCKOUT_MS = 2000L
    
    // --- Review Rewards ---
    const val REVIEW_BONUS_COUNT_THRESHOLD = 10
    const val REVIEW_REWARD_DEFAULT = 1
    const val REVIEW_REWARD_BONUS = 2
    
    // --- Selection ---
    const val MAX_CUSTOM_SECTION_GROUPS = 2
    
    // --- Riddle Config ---
    const val MULTI_QUIZ_WRONG_ANSWERS_COUNT = 3
}
