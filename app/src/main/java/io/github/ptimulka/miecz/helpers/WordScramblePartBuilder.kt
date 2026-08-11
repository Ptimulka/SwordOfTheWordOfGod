package io.github.ptimulka.miecz.helpers

import java.util.regex.Pattern

object WordScramblePartBuilder {
    fun build(
        verseText: String,
        isEasy: Boolean
    ): List<String> {
        val words = mutableListOf<String>()
        val textToParse = if (isEasy) verseText else verseText.replace('_', ' ')
        val matcher = Pattern.compile("\\*(.*?)\\*|\\S+").matcher(textToParse)
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                words.add(matcher.group(1)!!)
            } else {
                var word = matcher.group()
                if (isEasy) {
                    word = word.replace('_', ' ')
                }
                words.add(word)
            }
        }
        return words
    }
}
