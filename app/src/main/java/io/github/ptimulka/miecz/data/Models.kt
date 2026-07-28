package io.github.ptimulka.miecz.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// --- Bible Models ---

data class BibleBook(
    val sigla: String,
    val versesPerChapter: List<Int>
)

data class BibleReference(
    val book: String,
    val chapter: Int,
    val startVerse: Int,
    val endVerse: Int? = null
) {
    override fun toString(): String {
        return if (endVerse != null) {
            "$book $chapter,$startVerse-$endVerse"
        } else {
            "$book $chapter,$startVerse"
        }
    }
}

// --- Structure Models ---

@Parcelize
data class Verse(
    val book: String,
    val chapter: Int,
    val number: String,
    val text: String
) : Parcelable

@Parcelize
data class VerseGroup(
    val id: Int,
    val name: String,
    val verses: List<Verse>
) : Parcelable

@Parcelize
data class Section(
    val id: Int,
    val name: String,
    val verses: List<Verse>,
    val assetNames: List<String> = emptyList()
) : Parcelable

@Parcelize
data class WordItem(
    val id: Int,
    val text: String
) : Parcelable

// --- Game Models ---

enum class RiddleType {
    CONNECT_PARTS,
    CONNECT_PAIRS,
    REPEAT_VERSE,
    FILL_MORE_WORDS_EASY,
    FILL_MORE_WORDS_NORMAL,
    FILL_WORDS_EASY,
    FILL_WORDS_NORMAL,
    FILL_WHOLE_SIGLA,
    FILL_WHOLE_VERSE,
    FILL_SIGLA_BOOK,
    FILL_SIGLA_CHAPTER,
    FILL_SIGLA_VERSE,
    MULTI_QUIZ,
    QUIZ_EASY,
    QUIZ_NORMAL,
    WORD_SCRAMBLE_EASY,
    WORD_SCRAMBLE_NORMAL
}

@Parcelize
data class Riddle(
    val type: RiddleType,
    val verse: Verse
) : Parcelable