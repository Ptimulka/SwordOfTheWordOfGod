package io.github.ptimulka.miecz.helpers

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the pure text utilities that back answer grading. These run on the JVM with no
 * Android dependencies, so they are fast and safe to run on every build.
 */
class TextHelpersTest {

    // --- foldPolishChars ---

    @Test
    fun `foldPolishChars replaces every Polish diacritic with its base letter`() {
        assertEquals("acelnoszz", foldPolishChars("ąćęłńóśźż"))
        assertEquals("ACELNOSZZ", foldPolishChars("ĄĆĘŁŃÓŚŹŻ"))
    }

    @Test
    fun `foldPolishChars leaves plain ASCII untouched`() {
        assertEquals("Bog jest miloscia", foldPolishChars("Bog jest miloscia"))
    }

    // --- normalizeVerseText ---

    @Test
    fun `normalizeVerseText lowercases, folds accents and strips punctuation`() {
        assertEquals("bog jest miloscia", normalizeVerseText("Bóg jest miłością!"))
    }

    @Test
    fun `normalizeVerseText trims surrounding whitespace`() {
        assertEquals("amen", normalizeVerseText("  Amen.  "))
    }

    @Test
    fun `normalizeVerseText keeps digits`() {
        assertEquals("psalm 23", normalizeVerseText("Psalm 23"))
    }

    // --- calculateWordSimilarity (LCS-based Dice coefficient, 0..100) ---

    @Test
    fun `two empty lists are fully similar`() {
        assertEquals(100f, calculateWordSimilarity(emptyList(), emptyList()), 0.001f)
    }

    @Test
    fun `one empty list is zero similar`() {
        assertEquals(0f, calculateWordSimilarity(listOf("a"), emptyList()), 0.001f)
        assertEquals(0f, calculateWordSimilarity(emptyList(), listOf("a")), 0.001f)
    }

    @Test
    fun `identical word lists score 100`() {
        val words = listOf("bog", "jest", "miloscia")
        assertEquals(100f, calculateWordSimilarity(words, words), 0.001f)
    }

    @Test
    fun `one wrong word out of four scores 75`() {
        assertEquals(
            75f,
            calculateWordSimilarity(listOf("a", "b", "c", "x"), listOf("a", "b", "c", "d")),
            0.001f
        )
    }

    @Test
    fun `completely different lists score 0`() {
        assertEquals(0f, calculateWordSimilarity(listOf("a", "b"), listOf("x", "y")), 0.001f)
    }

    @Test
    fun `word order is preserved by the LCS - a swap is penalized`() {
        // LCS of [a,b] vs [b,a] is 1 -> 2*1/(2+2)*100 = 50
        assertEquals(50f, calculateWordSimilarity(listOf("a", "b"), listOf("b", "a")), 0.001f)
    }

    // --- formatTime ---

    @Test
    fun `formatTime shows seconds only under a minute`() {
        assertEquals("0s", formatTime(0))
        assertEquals("2s", formatTime(1500))   // rounds to nearest second
        assertEquals("59s", formatTime(59_000))
    }

    @Test
    fun `formatTime shows minutes and zero-padded seconds at or above a minute`() {
        assertEquals("1:00", formatTime(60_000))
        assertEquals("1:05", formatTime(65_000))
        assertEquals("10:09", formatTime(609_000))
    }
}
