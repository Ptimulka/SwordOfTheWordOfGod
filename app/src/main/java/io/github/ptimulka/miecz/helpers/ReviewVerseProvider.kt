package io.github.ptimulka.miecz.helpers

import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.repositories.ProgressRepository
import io.github.ptimulka.miecz.repositories.SectionRepository
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository

object ReviewVerseProvider {

    suspend fun loadKnownVerses(
        currentSectionId: Int,
        sectionRepository: SectionRepository,
        userProgressRepository: ProgressRepository,
        versesGroupsRepository: VersesGroupsRepository
    ): List<Pair<List<Verse>, List<String>>> {
        val sectionsList = mutableListOf<Pair<List<Verse>, List<String>>>()

        // Base sections
        val baseSections = listOf(
            R.raw.section01, R.raw.section02, R.raw.section03, R.raw.section04
        )
        baseSections.forEachIndexed { index, resId ->
            val sectionId = index + 1
            if (sectionId < currentSectionId) {
                sectionRepository.loadSection(resId)?.let {
                    if (it.verses.isNotEmpty()) sectionsList.add(it.verses to it.assetNames)
                }
            }
        }

        // Custom sections
        val customCount = userProgressRepository.getCustomSectionsCount()
        val allGroups = versesGroupsRepository.loadVerseGroups()
        for (i in 1..customCount) {
            val sectionId = 5 + i - 1
            if (sectionId < currentSectionId) {
                userProgressRepository.getCustomSectionGroups(sectionId)?.let { (id1, id2) ->
                    val g1 = allGroups.find { it.id == id1 }
                    val g2 = allGroups.find { it.id == id2 }
                    val g1Verses = g1?.verses ?: emptyList()
                    val g2Verses = g2?.verses ?: emptyList()
                    val allVerses = g1Verses + g2Verses
                    val allNames = g1Verses.mapIndexed { j, v ->
                        "group%03d_%d_%s%d-%s.webp".format(id1, j + 1, v.book, v.chapter, v.number.replace(".", "-"))
                    } + g2Verses.mapIndexed { j, v ->
                        "group%03d_%d_%s%d-%s.webp".format(id2, j + 1, v.book, v.chapter, v.number.replace(".", "-"))
                    }
                    val nameMap = allVerses.zip(allNames).toMap()
                    val sectionVerses = allVerses.distinct()
                    val sectionAssetNames = sectionVerses.map { nameMap[it] ?: "" }
                    if (sectionVerses.isNotEmpty()) {
                        sectionsList.add(sectionVerses to sectionAssetNames)
                    }
                }
            }
        }
        return sectionsList
    }

    fun pickReviewVerses(
        knownVersesSections: List<Pair<List<Verse>, List<String>>>,
        selectedCount: Int,
        startSectionIdx: Int,
        startVerseIdx: Int
    ): Pair<List<Verse>, List<String>> {
        val numSections = knownVersesSections.size
        if (numSections == 0) return emptyList<Verse>() to emptyList()

        val reviewVerses = mutableListOf<Verse>()
        val reviewAssetNames = mutableListOf<String>()

        (0 until selectedCount).forEach { i ->
            val currentSectionIdx = (startSectionIdx + i) % numSections
            // Assume 10 verses per section
            val currentVerseIdx = (startVerseIdx + (startSectionIdx + i) / numSections) % 10

            val (sectionVerses, sectionAssetNames) = knownVersesSections[currentSectionIdx]
            val verseIndexInList = currentVerseIdx % sectionVerses.size
            reviewVerses.add(sectionVerses[verseIndexInList])
            reviewAssetNames.add(sectionAssetNames.getOrElse(verseIndexInList) { "" })
        }

        return reviewVerses to reviewAssetNames
    }

    fun getShuffledRiddleTypes(count: Int): ArrayList<String> {
        val availableRiddleTypes = listOf(
            RiddleType.QUIZ_NORMAL.name,
            RiddleType.MULTI_QUIZ.name,
            RiddleType.WORD_SCRAMBLE_EASY.name,
            RiddleType.WORD_SCRAMBLE_NORMAL.name,
            RiddleType.FILL_SIGLA_BOOK.name,
            RiddleType.FILL_SIGLA_CHAPTER.name,
            RiddleType.FILL_SIGLA_VERSE.name,
            RiddleType.FILL_WORDS_EASY.name,
            RiddleType.FILL_WORDS_NORMAL.name,
            RiddleType.FILL_MORE_WORDS_EASY.name,
            RiddleType.FILL_MORE_WORDS_NORMAL.name
        )
        return ArrayList(availableRiddleTypes.shuffled().take(count))
    }
}
