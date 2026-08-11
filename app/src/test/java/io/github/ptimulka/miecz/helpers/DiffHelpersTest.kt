package io.github.ptimulka.miecz.helpers

import org.junit.Assert.assertEquals
import org.junit.Test

class DiffHelpersTest {

    @Test
    fun `diffWords correctly identifies equal, inserted, and deleted words`() {
        val original = "Na początku Bóg stworzył"
        val revised = "Na poczatku Pan stworzył niebo"
        
        val diffs = diffWords(original, revised)
        
        // Expected parts:
        // Na (EQUAL)
        // początku (DELETE)
        // poczatku (INSERT)
        // Bóg (DELETE)
        // Pan (INSERT)
        // stworzył (EQUAL)
        // niebo (INSERT)
        
        assertEquals(7, diffs.size)
        assertEquals(DiffType.EQUAL, diffs[0].type)
        assertEquals("Na", diffs[0].text)
        
        assertEquals(DiffType.DELETE, diffs[1].type)
        assertEquals("początku", diffs[1].text)
        
        assertEquals(DiffType.DELETE, diffs[2].type)
        assertEquals("Bóg", diffs[2].text)
        
        assertEquals(DiffType.INSERT, diffs[3].type)
        assertEquals("poczatku", diffs[3].text)
        
        assertEquals(DiffType.INSERT, diffs[4].type)
        assertEquals("Pan", diffs[4].text)
        
        assertEquals(DiffType.EQUAL, diffs[5].type)
        assertEquals("stworzył", diffs[5].text)
        
        assertEquals(DiffType.INSERT, diffs[6].type)
        assertEquals("niebo", diffs[6].text)
    }
}
