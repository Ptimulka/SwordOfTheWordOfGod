package io.github.ptimulka.miecz.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Unit tests for [parseVerse], which reads the semicolon-delimited verse CSV format. */
class VerseParserTest {

    @Test
    fun `parses a well-formed line into a Verse`() {
        val verse = parseVerse("Rdz;1;1;Na początku Bóg stworzył niebo i ziemię.")
        assertEquals("Rdz", verse?.book)
        assertEquals(1, verse?.chapter)
        assertEquals("1", verse?.number)
        assertEquals("Na początku Bóg stworzył niebo i ziemię.", verse?.text)
    }

    @Test
    fun `keeps semicolons that appear inside the verse text`() {
        val verse = parseVerse("Ps;23;1;Pan jest pasterzem; niczego mi nie braknie.")
        assertEquals("Pan jest pasterzem; niczego mi nie braknie.", verse?.text)
    }

    @Test
    fun `preserves a verse range in the number field`() {
        val verse = parseVerse("J;3;16-17;Tak bowiem Bóg umiłował świat.")
        assertEquals("16-17", verse?.number)
    }

    @Test
    fun `trims whitespace around the fields`() {
        val verse = parseVerse(" Rdz ; 1 ; 1 ; tekst ")
        assertEquals("Rdz", verse?.book)
        assertEquals(1, verse?.chapter)
        assertEquals("1", verse?.number)
        assertEquals("tekst", verse?.text)
    }

    @Test
    fun `non-numeric chapter falls back to 0`() {
        assertEquals(0, parseVerse("Rdz;abc;1;tekst")?.chapter)
    }

    @Test
    fun `returns null for blank input`() {
        assertNull(parseVerse(""))
        assertNull(parseVerse("   "))
    }

    @Test
    fun `returns null when there are too few fields`() {
        assertNull(parseVerse("Rdz;1;1"))
    }
}
