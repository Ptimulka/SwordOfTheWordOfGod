package io.github.ptimulka.miecz.repositories

interface RetentionRepository {
    fun getRetention(sectionId: Int): Int
    fun setRetention(sectionId: Int, retention: Int)
    fun addRetention(sectionId: Int, amount: Int): Int
    fun applyDailyRetentionDecay()
    fun isConnectDoneToday(sectionId: Int, riddleType: String): Boolean
    fun awardRetentionForConnectLevel(sectionId: Int, riddleType: String): Int
    fun awardRetentionForStandardLevel(sectionId: Int, levelNumber: Int): Int
    fun retentionContributionForRepeats(count: Int, isLongVerse: Boolean = false): Int
    fun getVerseRepeatCountToday(sectionId: Int, verseIndex: Int): Int
    fun incrementVerseRepeatToday(sectionId: Int, verseIndex: Int, isLongVerse: Boolean = false): Int
}
