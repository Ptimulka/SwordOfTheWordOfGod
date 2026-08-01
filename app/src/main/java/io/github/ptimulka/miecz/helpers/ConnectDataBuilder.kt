package io.github.ptimulka.miecz.helpers

import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.screens.riddles.connect.ConnectItem

object ConnectDataBuilder {

    fun buildPairsData(verses: List<Verse>): Pair<List<ConnectItem>, List<ConnectItem>> {
        val shuffledVerses = verses.shuffled()

        val siglaItems = shuffledVerses.map { ConnectItem(it.hashCode(), "${it.book} ${it.chapter},${it.number}") }
        val verseItems = shuffledVerses.map { ConnectItem(it.hashCode(), it.text.replace("_", " ")) }

        val reorderedSigla = siglaItems.customReorder(startIndex = 3)
        val reorderedVerses = verseItems.customReorder(startIndex = 2)

        val finalSigla = reorderedSigla.take(3).shuffled() + reorderedSigla.drop(3)
        val finalVerses = reorderedVerses.take(3).shuffled() + reorderedVerses.drop(3)

        return finalSigla to finalVerses
    }

    fun buildPartsData(verses: List<Verse>): Pair<List<ConnectItem>, List<ConnectItem>> {
        val shuffledVerses = verses.shuffled()
        val parts = shuffledVerses.map { splitVerse(it) }

        val part1Items = parts.map { ConnectItem(it.id, it.part1) }
        val part2Items = parts.map { ConnectItem(it.id, it.part2) }

        val reorderedPart1s = part1Items.customReorder(startIndex = 3)
        val reorderedPart2s = part2Items.customReorder(startIndex = 2)

        val finalPart1s = reorderedPart1s.take(3).shuffled() + reorderedPart1s.drop(3)
        val finalPart2s = reorderedPart2s.take(3).shuffled() + reorderedPart2s.drop(3)

        return finalPart1s to finalPart2s
    }

    private fun splitVerse(verse: Verse): SplitResult {
        val text = verse.text.replace("_", " ")
        val words = text.split(' ')
        var splitIndex = words.size / 2

        if (words.size % 2 != 0) {
            splitIndex++
        }

        if (splitIndex > 0 && words[splitIndex - 1].length < 3) {
            splitIndex--
        }

        val part1 = words.subList(0, splitIndex).joinToString(" ")
        val part2 = words.subList(splitIndex, words.size).joinToString(" ")

        return SplitResult(verse.hashCode(), "${verse.book} ${verse.chapter},${verse.number} $part1", part2)
    }

    private data class SplitResult(val id: Int, val part1: String, val part2: String)

    private fun <T> List<T>.customReorder(startIndex: Int): List<T> {
        if (this.size < startIndex + 1) return this

        val result = this.toMutableList()
        for (i in startIndex until result.size - 1 step 2) {
            val temp = result[i]
            result[i] = result[i + 1]
            result[i + 1] = temp
        }
        return result
    }
}
