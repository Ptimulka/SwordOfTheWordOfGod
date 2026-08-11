package io.github.ptimulka.miecz.helpers

data class MultiQuizAnswers(
    val books: List<String>,
    val chapters: List<String>,
    val verses: List<String>
)

object MultiQuizAnswerBuilder {
    fun build(
        correctBook: String,
        correctChapter: String,
        correctVerse: String
    ): MultiQuizAnswers {
        val wrongBooks = mutableSetOf<String>()
        val wrongChapters = mutableSetOf<String>()
        val wrongVerses = mutableSetOf<String>()

        while (wrongBooks.size < 3) {
            val randomRef = BibleDataProvider.getRandomReference()
            if (randomRef.book != correctBook) {
                wrongBooks.add(randomRef.book)
            }
        }

        while (wrongChapters.size < 3) {
            val randomRef = BibleDataProvider.getRandomReference()
            if (randomRef.chapter.toString() != correctChapter) {
                wrongChapters.add(randomRef.chapter.toString())
            }
        }

        while (wrongVerses.size < 3) {
            val randomRef = BibleDataProvider.getRandomReference()
            val versePart = if (randomRef.endVerse != null) {
                "${randomRef.startVerse}-${randomRef.endVerse}"
            } else {
                randomRef.startVerse.toString()
            }
            if (versePart != correctVerse) {
                wrongVerses.add(versePart)
            }
        }

        return MultiQuizAnswers(
            books = (wrongBooks + correctBook).shuffled(),
            chapters = (wrongChapters + correctChapter).shuffled(),
            verses = (wrongVerses + correctVerse).shuffled()
        )
    }
}
