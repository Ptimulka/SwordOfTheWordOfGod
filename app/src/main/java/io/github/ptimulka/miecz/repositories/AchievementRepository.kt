package io.github.ptimulka.miecz.repositories

interface AchievementRepository {
    fun getTotalReviewedVerses(): Int
    fun addTotalReviewedVerses(count: Int)
    fun getTotalAloudRepeats(): Int
    fun incrementTotalAloudRepeats()
    fun getBestTime(sectionId: Int, riddleType: String): Long
    fun updateBestTime(sectionId: Int, riddleType: String, elapsedMs: Long): Boolean
    fun getBestTimeOverall(
        riddleType: String,
        sectionRepo: SectionRepository,
        groupsRepo: VersesGroupsRepository
    ): BestTimeEntry?
}
