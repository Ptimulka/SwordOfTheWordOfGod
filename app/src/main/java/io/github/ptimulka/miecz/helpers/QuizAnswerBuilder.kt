package io.github.ptimulka.miecz.helpers

import io.github.ptimulka.miecz.data.Verse

object QuizAnswerBuilder {
    fun build(
        correctAnswer: String,
        isEasy: Boolean,
        sectionVerses: List<Verse>
    ): List<String> {
        val wrongAnswers = mutableListOf<String>()
        val totalAnswers = if (isEasy) 4 else 6
        val wrongAnswersNeeded = totalAnswers - 1

        if (!isEasy) {
            val otherVerses = sectionVerses
                .filter { ref -> "${ref.book} ${ref.chapter},${ref.number}" != correctAnswer }
                .shuffled()
            otherVerses.take(3).forEach { wrongAnswers.add("${it.book} ${it.chapter},${it.number}") }
        }

        while (wrongAnswers.size < wrongAnswersNeeded) {
            val randomRef = BibleDataProvider.getRandomReference()
            val randomRefString = randomRef.toString()
            if (randomRefString != correctAnswer && !wrongAnswers.contains(randomRefString)) {
                wrongAnswers.add(randomRefString)
            }
        }
        return (wrongAnswers + correctAnswer).shuffled()
    }
}
