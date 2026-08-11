package io.github.ptimulka.miecz.helpers

import io.github.ptimulka.miecz.data.Verse
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewVerseProviderTest {

    private val section1Verses = (1..10).map { Verse("K$it", 1, "1", "Tekst $it") }
    private val section2Verses = (11..20).map { Verse("K$it", 2, "1", "Tekst $it") }
    
    private val knownSections = listOf(
        section1Verses to section1Verses.map { "asset" },
        section2Verses to section2Verses.map { "asset" }
    )

    @Test
    fun `pickReviewVerses selects sequential verses across sections`() {
        // Start at section 0, verse 0. Pick 5.
        val (verses, _) = ReviewVerseProvider.pickReviewVerses(knownSections, 5, 0, 0)
        
        assertEquals(5, verses.size)
        assertEquals("K1", verses[0].book)  // Section 0, index 0
        assertEquals("K11", verses[1].book) // Section 1, index 0
        assertEquals("K2", verses[2].book)  // Section 0, index 1
        assertEquals("K12", verses[3].book) // Section 1, index 1
        assertEquals("K3", verses[4].book)  // Section 0, index 2
    }

    @Test
    fun `pickReviewVerses handles wrap around correctly`() {
        // Start at section 1, verse 9 (last verse). Pick 3.
        // Section indices: 1, 0, 1
        // Verse indices: 9, (9+1)/2 = 0, (9+2)/2 = 0 ? 
        // Logic: (startVerseIdx + (startSectionIdx + i) / numSections) % 10
        // i=0: (9 + (1+0)/2) % 10 = 9 % 10 = 9.
        // i=1: (9 + (1+1)/2) % 10 = 10 % 10 = 0.
        // i=2: (9 + (1+2)/2) % 10 = 10 % 10 = 0.
        
        val (verses, _) = ReviewVerseProvider.pickReviewVerses(knownSections, 3, 1, 9)
        
        assertEquals(3, verses.size)
        assertEquals("K20", verses[0].book) // Section 1, index 9
        assertEquals("K1", verses[1].book)  // Section 0, index 0
        assertEquals("K11", verses[2].book) // Section 1, index 0
    }
}
