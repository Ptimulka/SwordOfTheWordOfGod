package io.github.ptimulka.miecz.helpers

import io.github.ptimulka.miecz.screens.riddles.fill_words.VersePart

object FillWordsPartBuilder {
    fun build(
        verseText: String,
        isEasy: Boolean,
        moreWords: Boolean
    ): List<VersePart> {
        val parts = mutableListOf<VersePart>()
        val normalizedText = verseText.replace('_', ' ')
        val segments = normalizedText.split('*')

        var wordCounter = 0
        segments.forEachIndexed { index, segment ->
            val isInsideAsterisks = index % 2 != 0

            if (isInsideAsterisks) {
                parts.add(VersePart.StaticText(segment, isGreyed = true))
            } else {
                val words = segment.split(' ').filter { it.isNotBlank() }
                for (wordWithPunctuation in words) {
                    val coreWord = wordWithPunctuation.filter { it.isLetterOrDigit() }
                    val punctuation = wordWithPunctuation.filterNot { it.isLetterOrDigit() }
                    val isEligible = coreWord.length >= 3

                    if (isEligible) {
                        wordCounter++
                    }

                    val shouldBeFilled = if (moreWords) {
                        if (isEasy) {
                            wordCounter % 3 == 1 || wordCounter % 3 == 2
                        } else { // normal
                            wordCounter % 3 == 0 || wordCounter % 3 == 2
                        }
                    } else { // not moreWords
                        if (isEasy) {
                            wordCounter % 3 == 0
                        } else { // normal
                            wordCounter % 3 == 1
                        }
                    }

                    if (isEligible && shouldBeFilled) {
                        val hint = if (isEasy) coreWord.first().toString() else ""
                        parts.add(VersePart.WordToFill(coreWord, hint))
                        if (punctuation.isNotEmpty()) {
                            parts.add(VersePart.StaticText(punctuation))
                        }
                    } else {
                        parts.add(VersePart.StaticText(wordWithPunctuation))
                    }
                }
            }
        }
        return parts
    }
}
