package io.github.ptimulka.miecz.helpers

import io.github.ptimulka.miecz.data.BibleBook
import io.github.ptimulka.miecz.data.BibleReference
import kotlin.random.Random

object BibleDataProvider {
    // List of books with Polish abbreviations and verse counts for each chapter (based on Biblia Tysiąclecia/Vulgata numbering)
    val books = listOf(
        // Old Testament
        BibleBook("Rdz", listOf(31, 25, 24, 26, 32, 22, 24, 22, 29, 32, 32, 20, 18, 24, 21, 16, 27, 33, 38, 18, 34, 24, 20, 67, 34, 35, 46, 22, 35, 43, 55, 32, 20, 31, 29, 43, 36, 30, 23, 23, 57, 38, 34, 34, 28, 34, 31, 22, 33, 26)),
        BibleBook("Wj", listOf(22, 25, 22, 31, 23, 30, 25, 28, 35, 29, 10, 51, 22, 31, 27, 36, 16, 27, 25, 26, 37, 30, 33, 18, 40, 37, 21, 43, 46, 38, 18, 35, 23, 35, 35, 38, 29, 31, 43, 38)),
        BibleBook("Kpł", listOf(17, 16, 17, 35, 19, 23, 38, 36, 24, 20, 47, 8, 59, 57, 33, 34, 16, 30, 37, 27, 24, 33, 44, 23, 55, 46, 34)),
        BibleBook("Lb", listOf(54, 34, 51, 49, 31, 27, 89, 26, 23, 36, 35, 16, 33, 45, 41, 35, 13, 32, 22, 29, 35, 41, 30, 25, 18, 65, 23, 31, 40, 16, 54, 42, 56, 29, 34, 13)),
        BibleBook("Pwt", listOf(46, 37, 29, 49, 33, 25, 26, 20, 29, 22, 32, 32, 18, 29, 23, 22, 20, 22, 21, 20, 23, 29, 25, 22, 19, 19, 26, 68, 29, 20, 30, 52, 29, 12)),
        BibleBook("Joz", listOf(18, 24, 17, 24, 15, 27, 26, 35, 27, 43, 23, 24, 33, 15, 63, 10, 18, 28, 51, 9, 45, 34, 16, 33)),
        BibleBook("Sdz", listOf(36, 23, 31, 24, 31, 40, 25, 35, 57, 18, 40, 15, 25, 20, 20, 31, 13, 31, 30, 48, 25)),
        BibleBook("Rt", listOf(22, 23, 18, 22)),
        BibleBook("1Sm", listOf(28, 36, 21, 22, 12, 21, 17, 22, 27, 27, 15, 25, 23, 52, 35, 23, 58, 30, 24, 42, 15, 23, 29, 22, 44, 25, 12, 25, 11, 31, 13)),
        BibleBook("2Sm", listOf(27, 32, 39, 12, 25, 23, 29, 18, 13, 19, 27, 31, 39, 33, 37, 23, 29, 33, 43, 26, 22, 51, 39, 25)),
        BibleBook("1Krl", listOf(53, 46, 28, 34, 18, 38, 51, 66, 28, 29, 43, 33, 34, 31, 34, 34, 24, 46, 21, 43, 29, 53)),
        BibleBook("2Krl", listOf(18, 25, 27, 44, 27, 33, 20, 29, 37, 36, 21, 21, 25, 29, 38, 20, 41, 37, 37, 21, 26, 20, 37, 20, 30)),
        BibleBook("1Krn", listOf(54, 55, 24, 43, 26, 81, 40, 40, 44, 14, 47, 40, 14, 17, 29, 43, 27, 17, 19, 8, 30, 19, 32, 31, 31, 32, 34, 21, 30)),
        BibleBook("2Krn", listOf(17, 18, 17, 22, 14, 42, 22, 18, 31, 19, 23, 16, 22, 15, 19, 14, 19, 34, 11, 37, 20, 12, 21, 27, 28, 23, 9, 27, 36, 27, 21, 33, 25, 33, 27, 23)),
        BibleBook("Ezd", listOf(11, 70, 13, 24, 17, 22, 28, 36, 15, 44)),
        BibleBook("Ne", listOf(11, 20, 32, 23, 19, 19, 73, 18, 38, 39, 36, 47, 31)),
        BibleBook("Tb", listOf(22, 14, 17, 21, 22, 18, 18, 21, 6, 12, 19, 22, 17, 15)),
        BibleBook("Jdt", listOf(16, 28, 10, 15, 24, 21, 32, 36, 14, 23, 23, 20, 20, 19, 13, 25)),
        BibleBook("Est", listOf(22, 23, 15, 17, 14, 14, 10, 17, 32, 3)), // Hebrew version counts, Greek additions vary
        BibleBook("1Mch", listOf(64, 70, 60, 61, 68, 63, 50, 32, 73, 89, 74, 53, 53, 49, 41, 24)),
        BibleBook("2Mch", listOf(36, 32, 40, 50, 27, 31, 42, 36, 29, 38, 38, 45, 26, 46, 39)),
        BibleBook("Hi", listOf(22, 13, 26, 21, 27, 30, 21, 22, 35, 22, 20, 25, 28, 22, 35, 22, 16, 21, 29, 29, 34, 30, 17, 25, 6, 14, 23, 28, 25, 31, 40, 22, 33, 37, 16, 33, 24, 41, 30, 32, 26, 17)),
        BibleBook("Ps", listOf(6, 12, 9, 9, 13, 11, 18, 10, 21, 18, 7, 9, 6, 7, 5, 11, 15, 51, 15, 10, 14, 32, 6, 10, 22, 12, 14, 9, 11, 13, 25, 11, 22, 23, 28, 13, 40, 23, 14, 18, 14, 12, 5, 27, 18, 12, 10, 15, 21, 23, 21, 11, 7, 9, 24, 14, 12, 12, 18, 14, 9, 13, 12, 11, 14, 20, 8, 36, 37, 6, 24, 20, 28, 23, 11, 13, 21, 72, 13, 20, 17, 8, 19, 13, 14, 17, 7, 19, 53, 17, 16, 16, 5, 23, 11, 13, 12, 9, 9, 5, 8, 29, 22, 35, 45, 48, 43, 14, 31, 7, 10, 10, 9, 8, 18, 19, 2, 29, 176, 7, 8, 9, 4, 8, 5, 6, 5, 6, 8, 8, 3, 18, 3, 3, 21, 26, 9, 8, 24, 14, 10, 8, 12, 15, 21, 10, 20, 14, 9, 6)),
        BibleBook("Prz", listOf(33, 22, 35, 27, 23, 35, 27, 36, 18, 32, 31, 28, 25, 35, 33, 33, 28, 24, 29, 30, 31, 29, 35, 34, 28, 28, 27, 28, 27, 33, 31)),
        BibleBook("Koh", listOf(18, 26, 22, 17, 19, 12, 29, 17, 18, 20, 10, 14)),
        BibleBook("Pnp", listOf(16, 17, 11, 16, 16, 12, 14, 14)),
        BibleBook("Mdr", listOf(16, 24, 19, 20, 23, 25, 30, 21, 18, 21, 26, 27, 19, 31, 19, 29, 21, 25, 22)),
        BibleBook("Syr", listOf(30, 18, 31, 31, 15, 37, 36, 19, 18, 31, 34, 18, 26, 27, 20, 30, 32, 33, 30, 31, 28, 27, 27, 34, 26, 29, 30, 26, 28, 25, 31, 24, 33, 31, 26, 31, 31, 34, 35, 30, 22, 25, 33, 23, 26, 20, 25, 25, 16, 29, 30)),
        BibleBook("Iz", listOf(31, 22, 26, 6, 30, 13, 25, 23, 20, 34, 16, 6, 22, 32, 9, 14, 14, 7, 25, 6, 17, 25, 18, 23, 12, 21, 13, 29, 24, 33, 9, 20, 24, 17, 10, 22, 38, 22, 8, 31, 29, 25, 28, 28, 25, 13, 15, 22, 26, 11, 23, 15, 12, 17, 13, 12, 21, 14, 21, 22, 11, 12, 19, 11, 25, 24)),
        BibleBook("Jr", listOf(19, 37, 25, 31, 31, 30, 34, 23, 25, 25, 23, 17, 27, 22, 21, 21, 27, 23, 15, 18, 14, 30, 40, 10, 38, 24, 22, 17, 32, 24, 40, 44, 26, 22, 19, 32, 21, 28, 18, 16, 18, 22, 13, 30, 5, 28, 7, 47, 39, 46, 64, 34)),
        BibleBook("La", listOf(22, 22, 66, 22, 22)),
        BibleBook("Ba", listOf(22, 35, 37, 37, 9, 72)),
        BibleBook("Ez", listOf(28, 10, 27, 17, 17, 14, 27, 18, 11, 22, 25, 28, 23, 23, 8, 63, 24, 32, 14, 44, 37, 31, 49, 27, 17, 21, 36, 26, 21, 26, 18, 32, 33, 31, 15, 38, 28, 23, 29, 49, 26, 20, 27, 31, 25, 24, 23, 35)),
        BibleBook("Dn", listOf(21, 49, 100, 34, 30, 29, 28, 27, 27, 21, 45, 13, 64, 42)),
        BibleBook("Oz", listOf(9, 25, 5, 19, 15, 11, 16, 14, 17, 15, 11, 15, 15, 10)),
        BibleBook("Jl", listOf(20, 27, 5, 21)),
        BibleBook("Am", listOf(15, 16, 15, 13, 27, 14, 17, 14, 15)),
        BibleBook("Ab", listOf(21)),
        BibleBook("Jon", listOf(16, 11, 10, 11)),
        BibleBook("Mi", listOf(16, 13, 12, 14, 14, 16, 20)),
        BibleBook("Na", listOf(14, 14, 19)),
        BibleBook("Ha", listOf(17, 20, 19)),
        BibleBook("So", listOf(18, 15, 20)),
        BibleBook("Ag", listOf(15, 23)),
        BibleBook("Za", listOf(17, 17, 10, 14, 11, 15, 14, 23, 17, 12, 17, 14, 9, 21)),
        BibleBook("Ml", listOf(14, 17, 24)),

        // New Testament
        BibleBook("Mt", listOf(25, 23, 17, 25, 48, 34, 29, 34, 38, 42, 30, 50, 58, 36, 39, 28, 27, 35, 30, 34, 46, 46, 39, 51, 46, 75, 66, 20)),
        BibleBook("Mk", listOf(45, 28, 35, 41, 43, 56, 37, 38, 50, 52, 33, 44, 37, 72, 47, 20)),
        BibleBook("Łk", listOf(80, 52, 38, 44, 39, 49, 50, 56, 62, 42, 54, 59, 35, 35, 32, 31, 37, 43, 48, 47, 38, 71, 56, 53)),
        BibleBook("J", listOf(51, 25, 36, 54, 47, 71, 53, 59, 41, 42, 57, 50, 38, 31, 27, 33, 26, 40, 42, 31, 25)),
        BibleBook("Dz", listOf(26, 47, 26, 37, 42, 15, 60, 40, 43, 48, 30, 25, 52, 28, 41, 40, 34, 28, 40, 38, 40, 30, 35, 27, 27, 32, 44, 31)),
        BibleBook("Rz", listOf(32, 29, 31, 25, 21, 23, 25, 39, 33, 21, 36, 21, 14, 23, 33, 27)),
        BibleBook("1Kor", listOf(31, 16, 23, 21, 13, 20, 40, 13, 27, 33, 34, 31, 13, 40, 58, 24)),
        BibleBook("2Kor", listOf(24, 17, 18, 18, 21, 18, 16, 24, 15, 18, 33, 21, 13)),
        BibleBook("Ga", listOf(24, 21, 29, 31, 26, 18)),
        BibleBook("Ef", listOf(23, 22, 21, 32, 33, 24)),
        BibleBook("Flp", listOf(30, 30, 21, 23)),
        BibleBook("Kol", listOf(29, 23, 25, 18)),
        BibleBook("1Tes", listOf(10, 20, 13, 18, 28)),
        BibleBook("2Tes", listOf(12, 17, 18)),
        BibleBook("1Tm", listOf(20, 15, 16, 16, 25, 21)),
        BibleBook("2Tm", listOf(18, 26, 17, 22)),
        BibleBook("Tt", listOf(16, 15, 15)),
        BibleBook("Flm", listOf(25)),
        BibleBook("Hbr", listOf(14, 18, 19, 16, 14, 20, 28, 13, 28, 39, 40, 29, 25)),
        BibleBook("Jk", listOf(27, 26, 18, 17, 20)),
        BibleBook("1P", listOf(25, 25, 22, 19, 14)),
        BibleBook("2P", listOf(21, 22, 18)),
        BibleBook("1J", listOf(10, 29, 24, 21, 21)),
        BibleBook("2J", listOf(13)),
        BibleBook("3J", listOf(15)),
        BibleBook("Jud", listOf(25)),
        BibleBook("Ap", listOf(20, 29, 22, 11, 14, 17, 17, 13, 21, 11, 19, 18, 18, 20, 8, 21, 18, 24, 21, 15, 27, 21))
    )

    fun getRandomReference(): BibleReference {
        val book = books.random()
        val chapterIndex = book.versesPerChapter.indices.random()
        val chapter = chapterIndex + 1
        val verseCount = book.versesPerChapter[chapterIndex]

        // 5% chance to create a range
        if (Random.nextInt(1, 101) <= 5) {
            val range = Random.nextInt(1, 3) // 1 or 2 verses range
            val startVerse = Random.nextInt(1, verseCount - range + 1)
            if (startVerse + range <= verseCount) {
                return BibleReference(book.sigla, chapter, startVerse, startVerse + range)
            }
        }

        // Fallback to single verse
        val verse = Random.nextInt(1, verseCount + 1)
        return BibleReference(book.sigla, chapter, verse)
    }
}
