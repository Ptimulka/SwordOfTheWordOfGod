package io.github.ptimulka.miecz.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Unit tests for [BookNameNormalizer.getCanonicalSigla], which maps user spellings to a sigla. */
class BookNameNormalizerTest {

    @Test
    fun `canonical sigla is accepted case-insensitively`() {
        assertEquals("Rdz", BookNameNormalizer.getCanonicalSigla("Rdz"))
        assertEquals("Rdz", BookNameNormalizer.getCanonicalSigla("rdz"))
    }

    @Test
    fun `full Polish book name maps to its sigla`() {
        assertEquals("Rdz", BookNameNormalizer.getCanonicalSigla("Księga Rodzaju"))
        assertEquals("Ps", BookNameNormalizer.getCanonicalSigla("psalmów"))
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        assertEquals("Rdz", BookNameNormalizer.getCanonicalSigla("  rodzaju  "))
    }

    @Test
    fun `numbered-book aliases resolve to the right sigla`() {
        assertEquals("1Sm", BookNameNormalizer.getCanonicalSigla("1 samuela"))
        assertEquals("2Krl", BookNameNormalizer.getCanonicalSigla("2 królewska"))
    }

    @Test
    fun `unknown input returns null`() {
        assertNull(BookNameNormalizer.getCanonicalSigla("Silmarillion"))
        assertNull(BookNameNormalizer.getCanonicalSigla(""))
    }
}
