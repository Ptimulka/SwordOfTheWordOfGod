package io.github.ptimulka.miecz.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiQuizAnswerBuilderTest {

    @Test
    fun `build returns lists with correct answer included`() {
        val answers = MultiQuizAnswerBuilder.build(
            correctBook = "Rdz",
            correctChapter = "1",
            correctVerse = "1"
        )
        
        assertEquals(4, answers.books.size)
        assertTrue(answers.books.contains("Rdz"))
        
        assertEquals(4, answers.chapters.size)
        assertTrue(answers.chapters.contains("1"))
        
        assertEquals(4, answers.verses.size)
        assertTrue(answers.verses.contains("1"))
    }
}
