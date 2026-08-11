package io.github.ptimulka.miecz.helpers

import io.github.ptimulka.miecz.screens.riddles.fill_words.VersePart
import org.junit.Assert.assertEquals
import org.junit.Test

class FillWordsPartBuilderTest {

    @Test
    fun `build parses verse with asterisks as greyed static text`() {
        val verseText = "Normalny *szary* tekst"
        val parts = FillWordsPartBuilder.build(verseText, isEasy = true, moreWords = false)
        
        val greyed = parts.filterIsInstance<VersePart.StaticText>().find { it.isGreyed }
        assertEquals("szary", greyed?.text)
    }

    @Test
    fun `build creates fillable words for eligible words`() {
        // wordCounter: 1 ("Pierwszy"), 2 ("tekst"), 3 ("trzeci")
        // !moreWords, !isEasy -> %3 == 1 -> word 1 ("Pierwszy")
        val verseText = "Pierwszy tekst trzeci"
        val parts = FillWordsPartBuilder.build(verseText, isEasy = false, moreWords = false)
        
        val fillable = parts.filterIsInstance<VersePart.WordToFill>()
        assertEquals(1, fillable.size)
        assertEquals("Pierwszy", fillable[0].correctWord)
        assertEquals("", fillable[0].hint)
    }

    @Test
    fun `build provides hint in easy mode`() {
        // wordCounter: 1 ("Pierwszy"), 2 ("tekst"), 3 ("trzeci")
        // !moreWords, isEasy -> %3 == 0 -> word 3 ("trzeci")
        val verseText = "Pierwszy tekst trzeci"
        val parts = FillWordsPartBuilder.build(verseText, isEasy = true, moreWords = false)
        
        val fillable = parts.filterIsInstance<VersePart.WordToFill>()
        assertEquals(1, fillable.size)
        assertEquals("trzeci", fillable[0].correctWord)
        assertEquals("t", fillable[0].hint)
    }

    @Test
    fun `build handles moreWords true correctly`() {
        val verseText = "Raz dwa trzy cztery"
        // wordCounter: 1, 2, 3, 4
        // moreWords true, isEasy true -> %3 == 1 or 2 -> 1, 2, 4
        val parts = FillWordsPartBuilder.build(verseText, isEasy = true, moreWords = true)
        val fillable = parts.filterIsInstance<VersePart.WordToFill>()
        assertEquals(3, fillable.size)
        assertEquals("Raz", fillable[0].correctWord)
        assertEquals("dwa", fillable[1].correctWord)
        assertEquals("cztery", fillable[2].correctWord)
    }
}
