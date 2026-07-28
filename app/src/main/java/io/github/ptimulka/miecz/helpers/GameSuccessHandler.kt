package io.github.ptimulka.miecz.helpers

import io.github.ptimulka.miecz.GameActivity.Companion.SECTION_ID_REPEAT_FOR_SHIELDS
import io.github.ptimulka.miecz.data.Riddle
import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.repositories.UserProgressRepository

fun updateProgress(
    sectionId: Int,
    levelNumber: Int,
    userProgressRepository: UserProgressRepository,
    riddles: List<Riddle>
) {
    if (sectionId > 0) {
        // Level completed - update progress
        if (levelNumber != 0) { // Standard level
            userProgressRepository.setLevelFinished(sectionId, levelNumber, true)
        } else { // Special challenge
            val firstRiddleType = riddles.firstOrNull()?.type
            if (firstRiddleType == RiddleType.FILL_WHOLE_SIGLA) {
                userProgressRepository.setSiglaFinished(sectionId, true)
            } else if (firstRiddleType == RiddleType.FILL_WHOLE_VERSE) {
                userProgressRepository.setVerseFinished(sectionId, true)
            }

            // If both special challenges finished, unlock next section
            if (userProgressRepository.areSpecialChallengesFinished(sectionId)) {
                val currentSavedSection = userProgressRepository.getCurrentSection()
                if (sectionId == currentSavedSection) {
                    userProgressRepository.setCurrentSection(sectionId + 1)
                    // Congratulate the user on the levels screen when they return
                    userProgressRepository.setPendingSectionUnlocked(sectionId + 1)
                }
            }
        }
    } else if (sectionId < 0) { // Repeat level (review)

        // Lifetime tally of verses reviewed, shown in achievements
        userProgressRepository.addTotalReviewedVerses(riddles.size)

        if(sectionId == SECTION_ID_REPEAT_FOR_SHIELDS) {
            val reward = if (riddles.size == 10) 2 else 1
            repeat(reward) {
                userProgressRepository.increaseShields()
            }
        }
        val sIdx = userProgressRepository.getRepeatSectionIndex()
        val vIdx = userProgressRepository.getRepeatVerseIndex()
        val currentSectionId = userProgressRepository.getCurrentSection()
        val numSections = currentSectionId - 1
        val solvedCount = riddles.size

        if (numSections > 0) { // Should always be true as repeat level is possible if user finished 2 sections
            // Calculate new state
            val totalSteps = sIdx + solvedCount
            val nextSectionIdx = totalSteps % numSections
            val nextVerseIdx = (vIdx + totalSteps / numSections) % 10

            userProgressRepository.saveRepeatProgress(nextSectionIdx, nextVerseIdx)
        }
    }
}