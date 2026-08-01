package io.github.ptimulka.miecz.helpers

import org.junit.Assert.assertEquals
import org.junit.Test

class WordScramblePartBuilderTest {

    @Test
    fun `build parses verse into words and handles underscores in easy mode`() {
        val verseText = "Na_początku Bóg"
        val words = WordScramblePartBuilder.build(verseText, isEasy = true)
        
        assertEquals(2, words.size)
        assertEquals("Na początku", words[0])
        assertEquals("Bóg", words[1])
    }

    @Test
    fun `build parses verse into words and replaces underscores in normal mode`() {
        val verseText = "Na_początku Bóg"
        val words = WordScramblePartBuilder.build(verseText, isEasy = false)
        
        assertEquals(3, words.size)
        assertEquals("Na", words[0])
        assertEquals("początku", words[1])
        assertEquals("Bóg", words[2])
    }

    @Test
    fun `build handles asterisk groups as single words`() {
        val verseText = "Normalny *tekst w gwiazdkach* koniec"
        val words = WordScramblePartBuilder.build(verseText, isEasy = true)
        
        assertEquals(3, words.size)
        assertEquals("Normalny", words[0])
        assertEquals("tekst w gwiazdkach", words[1])
        assertEquals("koniec", words[2])
    }
}
