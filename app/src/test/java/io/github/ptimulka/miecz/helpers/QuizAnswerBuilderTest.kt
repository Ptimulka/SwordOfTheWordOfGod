package io.github.ptimulka.miecz.helpers

import io.github.ptimulka.miecz.data.Verse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizAnswerBuilderTest {

    @Test
    fun `build returns correct number of answers for easy mode`() {
        val answers = QuizAnswerBuilder.build(
            correctAnswer = "Rdz 1,1",
            isEasy = true,
            sectionVerses = emptyList()
        )
        assertEquals(4, answers.size)
        assertTrue(answers.contains("Rdz 1,1"))
    }

    @Test
    fun `build returns correct number of answers for normal mode`() {
        val answers = QuizAnswerBuilder.build(
            correctAnswer = "Rdz 1,1",
            isEasy = false,
            sectionVerses = emptyList()
        )
        assertEquals(6, answers.size)
        assertTrue(answers.contains("Rdz 1,1"))
    }

    @Test
    fun `build includes other verses from the section when not easy`() {
        val sectionVerses = listOf(
            Verse("Rdz", 1, "2", "Tekst 2"),
            Verse("Rdz", 1, "3", "Tekst 3"),
            Verse("Rdz", 1, "4", "Tekst 4")
        )
        val answers = QuizAnswerBuilder.build(
            correctAnswer = "Rdz 1,1",
            isEasy = false,
            sectionVerses = sectionVerses
        )
        assertEquals(6, answers.size)
        assertTrue(answers.contains("Rdz 1,1"))
        assertTrue(answers.contains("Rdz 1,2"))
        assertTrue(answers.contains("Rdz 1,3"))
        assertTrue(answers.contains("Rdz 1,4"))
    }
}
