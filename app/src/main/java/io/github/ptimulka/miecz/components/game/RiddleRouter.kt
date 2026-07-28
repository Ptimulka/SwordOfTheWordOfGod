package io.github.ptimulka.miecz.components.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import io.github.ptimulka.miecz.data.Riddle
import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.screens.riddles.*

@Composable
fun RiddleRouter(
    riddle: Riddle,
    sectionId: Int,
    sectionVerses: List<Verse>,
    assetNames: List<String> = emptyList(),
    onSuccess: (elapsedMs: Long?) -> Unit,
    onShieldLoss: () -> Boolean
) {
    val verse = riddle.verse

    key(riddle.type, verse.hashCode()) {
        when (riddle.type) {
            RiddleType.FILL_WHOLE_VERSE -> {
                val verseIndex = sectionVerses.indexOfFirst {
                    it.book == verse.book && it.chapter == verse.chapter && it.number == verse.number
                }
                FillWholeVerseRiddleScreen(
                    verseText = verse.text,
                    book = verse.book,
                    chapter = verse.chapter,
                    number = verse.number,
                    sectionId = sectionId,
                    verseIndex = verseIndex,
                    assetName = assetNames.getOrNull(verseIndex),
                    onSuccess = { onSuccess(null) },
                    onShieldLoss = onShieldLoss
                )
            }
            RiddleType.FILL_WHOLE_SIGLA -> {
                val verseIndex = sectionVerses.indexOfFirst {
                    it.book == verse.book && it.chapter == verse.chapter && it.number == verse.number
                }
                FillWholeSiglaRiddleScreen(
                    verseText = verse.text,
                    book = verse.book,
                    chapter = verse.chapter,
                    number = verse.number,
                    sectionId = sectionId,
                    verseIndex = verseIndex,
                    assetName = assetNames.getOrNull(verseIndex),
                    onSuccess = { onSuccess(null) },
                    onShieldLoss = onShieldLoss
                )
            }
            RiddleType.REPEAT_VERSE -> {
                RepeatVerseRiddleScreen(
                    sectionVerses = sectionVerses,
                    sectionId = sectionId,
                    assetNames = assetNames,
                    onSuccess = { onSuccess(null) }
                )
            }
            RiddleType.CONNECT_PARTS -> {
                ConnectPartsRiddleScreen(
                    sectionVerses = sectionVerses,
                    sectionId = sectionId,
                    assetNames = assetNames,
                    onSuccess = { elapsedMs -> onSuccess(elapsedMs) }
                )
            }
            RiddleType.CONNECT_PAIRS -> {
                ConnectPairsRiddleScreen(
                    sectionVerses = sectionVerses,
                    sectionId = sectionId,
                    assetNames = assetNames,
                    onSuccess = { elapsedMs -> onSuccess(elapsedMs) }
                )
            }
            RiddleType.FILL_MORE_WORDS_EASY,
            RiddleType.FILL_MORE_WORDS_NORMAL -> {
                val verseIndex = sectionVerses.indexOfFirst {
                    it.book == verse.book && it.chapter == verse.chapter && it.number == verse.number
                }
                FillWordsRiddleScreen(
                    verseText = verse.text,
                    book = verse.book,
                    chapter = verse.chapter,
                    number = verse.number,
                    isEasy = riddle.type == RiddleType.FILL_MORE_WORDS_EASY,
                    moreWords = true,
                    sectionId = sectionId,
                    verseIndex = verseIndex,
                    assetName = assetNames.getOrNull(verseIndex),
                    onSuccess = { onSuccess(null) },
                    onShieldLoss = onShieldLoss
                )
            }
            RiddleType.FILL_WORDS_EASY,
            RiddleType.FILL_WORDS_NORMAL -> {
                val verseIndex = sectionVerses.indexOfFirst {
                    it.book == verse.book && it.chapter == verse.chapter && it.number == verse.number
                }
                FillWordsRiddleScreen(
                    verseText = verse.text,
                    book = verse.book,
                    chapter = verse.chapter,
                    number = verse.number,
                    isEasy = riddle.type == RiddleType.FILL_WORDS_EASY,
                    moreWords = false,
                    sectionId = sectionId,
                    verseIndex = verseIndex,
                    assetName = assetNames.getOrNull(verseIndex),
                    onSuccess = { onSuccess(null) },
                    onShieldLoss = onShieldLoss
                )
            }
            RiddleType.FILL_SIGLA_BOOK,
            RiddleType.FILL_SIGLA_CHAPTER,
            RiddleType.FILL_SIGLA_VERSE -> {
                val verseIndex = sectionVerses.indexOfFirst {
                    it.book == verse.book && it.chapter == verse.chapter && it.number == verse.number
                }
                FillSiglaRiddleScreen(
                    verseText = verse.text,
                    book = verse.book,
                    chapter = verse.chapter,
                    number = verse.number,
                    fillType = when(riddle.type) {
                        RiddleType.FILL_SIGLA_BOOK -> FillSiglaType.BOOK
                        RiddleType.FILL_SIGLA_CHAPTER -> FillSiglaType.CHAPTER
                        else -> FillSiglaType.VERSE
                    },
                    sectionId = sectionId,
                    verseIndex = verseIndex,
                    assetName = assetNames.getOrNull(verseIndex),
                    onSuccess = { onSuccess(null) },
                    onShieldLoss = onShieldLoss
                )
            }
            RiddleType.MULTI_QUIZ -> {
                val verseIndex = sectionVerses.indexOfFirst {
                    it.book == verse.book && it.chapter == verse.chapter && it.number == verse.number
                }
                MultiQuizRiddleScreen(
                    verseText = verse.text,
                    book = verse.book,
                    chapter = verse.chapter,
                    number = verse.number,
                    sectionId = sectionId,
                    verseIndex = verseIndex,
                    assetName = assetNames.getOrNull(verseIndex),
                    onSuccess = { onSuccess(null) },
                    onShieldLoss = onShieldLoss
                )
            }
            RiddleType.QUIZ_EASY,
            RiddleType.QUIZ_NORMAL -> {
                val verseIndex = sectionVerses.indexOfFirst {
                    it.book == verse.book && it.chapter == verse.chapter && it.number == verse.number
                }
                QuizRiddleScreen(
                    verseText = verse.text,
                    book = verse.book,
                    chapter = verse.chapter,
                    number = verse.number,
                    isEasy = riddle.type == RiddleType.QUIZ_EASY,
                    sectionVerses = sectionVerses,
                    sectionId = sectionId,
                    verseIndex = verseIndex,
                    assetName = assetNames.getOrNull(verseIndex),
                    onSuccess = { onSuccess(null) },
                    onShieldLoss = onShieldLoss
                )
            }
            RiddleType.WORD_SCRAMBLE_EASY,
            RiddleType.WORD_SCRAMBLE_NORMAL -> {
                val verseIndex = sectionVerses.indexOfFirst {
                    it.book == verse.book && it.chapter == verse.chapter && it.number == verse.number
                }
                WordScrambleRiddleScreen(
                    verseText = verse.text,
                    book = verse.book,
                    chapter = verse.chapter,
                    number = verse.number,
                    isEasy = riddle.type == RiddleType.WORD_SCRAMBLE_EASY,
                    sectionId = sectionId,
                    verseIndex = verseIndex,
                    assetName = assetNames.getOrNull(verseIndex),
                    onSuccess = { onSuccess(null) },
                    onShieldLoss = onShieldLoss
                )
            }
        }
    }
}