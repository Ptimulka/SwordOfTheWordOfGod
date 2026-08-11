package io.github.ptimulka.miecz.helpers

import io.github.ptimulka.miecz.data.Verse
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectDataBuilderTest {

    private val verses = listOf(
        Verse("Rdz", 1, "1", "Na początku Bóg stworzył niebo i ziemię."),
        Verse("J", 3, "16", "Tak bowiem Bóg umiłował świat.")
    )

    @Test
    fun `buildPairsData creates correct sigla and verse items`() {
        val (sigla, text) = ConnectDataBuilder.buildPairsData(verses)
        
        assertEquals(2, sigla.size)
        assertEquals(2, text.size)
        
        // Check content (order might be shuffled but let's check one)
        val rdzSigla = sigla.find { it.text == "Rdz 1,1" }
        val rdzText = text.find { it.text == "Na początku Bóg stworzył niebo i ziemię." }
        
        assertEquals(rdzSigla?.id, rdzText?.id)
    }

    @Test
    fun `buildPartsData splits verses correctly`() {
        val (parts1, parts2) = ConnectDataBuilder.buildPartsData(verses)
        
        assertEquals(2, parts1.size)
        assertEquals(2, parts2.size)
        
        // Parts for Rdz 1,1
        val p1 = parts1.find { it.text.contains("Rdz 1,1") }
        val p2 = parts2.find { it.id == p1?.id }
        
        // "Na początku Bóg stworzył niebo i ziemię." (7 words)
        // splitIndex = 7/2 + 1 = 4 -> Na początku Bóg stworzył | niebo i ziemię.
        assertEquals("Rdz 1,1 Na początku Bóg stworzył", p1?.text)
        assertEquals("niebo i ziemię.", p2?.text)
    }
}
