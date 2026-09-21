package io.github.ptimulka.miecz.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VerseTest {

    @Test
    fun `isLong returns false for short verses`() {
        val text = "Short verse with few words."
        val verse = Verse("Test", 1, "1", text)
        assertFalse(verse.isLong)
    }

    @Test
    fun `isLong returns true for verses with 20 or more words`() {
        val words = List(20) { "word" }.joinToString(" ")
        val verse = Verse("Test", 1, "1", words)
        assertTrue(verse.isLong)
    }

    @Test
    fun `isLong handles underscores as spaces`() {
        val words = List(20) { "word" }.joinToString("_")
        val verse = Verse("Test", 1, "1", words)
        assertTrue(verse.isLong)
    }

    @Test
    fun `isLong ignores text in asterisks`() {
        // 10 words outside, 15 words inside *...*
        val visibleWords = List(10) { "visible" }.joinToString(" ")
        val hiddenWords = List(15) { "hidden" }.joinToString(" ")
        val text = "$visibleWords *$hiddenWords*"
        
        val verse = Verse("Test", 1, "1", text)
        
        // Total is 25, but only 10 should be counted
        assertFalse(verse.isLong)
    }

    @Test
    fun `isLong counts words correctly with mixed formatting`() {
        // 19 words outside, 5 words inside *...*, underscores used
        val part1 = List(10) { "word" }.joinToString("_")
        val part2 = List(9) { "word" }.joinToString(" ")
        val optional = List(5) { "opt" }.joinToString(" ")
        val text = "$part1 $part2 *$optional*"
        
        val verse = Verse("Test", 1, "1", text)
        assertFalse(verse.isLong) // 19 < 20
        
        val longerText = "$text word"
        assertTrue(Verse("Test", 1, "1", longerText).isLong) // 20 >= 20
    }
}
