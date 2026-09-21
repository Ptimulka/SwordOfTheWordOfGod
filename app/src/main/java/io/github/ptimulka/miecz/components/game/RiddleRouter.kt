package io.github.ptimulka.miecz.components.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import io.github.ptimulka.miecz.data.Riddle
import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.screens.riddles.quiz.QuizRiddleScreen
import io.github.ptimulka.miecz.screens.riddles.fill_words.FillWordsRiddleScreen
import io.github.ptimulka.miecz.screens.riddles.fill_sigla.FillSiglaRiddleScreen
import io.github.ptimulka.miecz.screens.riddles.fill_sigla.FillSiglaType
import io.github.ptimulka.miecz.screens.riddles.multi_quiz.MultiQuizRiddleScreen
import io.github.ptimulka.miecz.screens.riddles.word_scramble.WordScrambleRiddleScreen
import io.github.ptimulka.miecz.screens.riddles.fill_whole_verse.FillWholeVerseRiddleScreen
import io.github.ptimulka.miecz.screens.riddles.fill_whole_sigla.FillWholeSiglaRiddleScreen
import io.github.ptimulka.miecz.screens.riddles.connect.ConnectPairsRiddleScreen
import io.github.ptimulka.miecz.screens.riddles.connect.ConnectPartsRiddleScreen
import io.github.ptimulka.miecz.screens.riddles.repeat_verse.RepeatVerseRiddleScreen

@Composable
fun RiddleRouter(
    riddle: Riddle,
    sectionId: Int,
    riddleIndex: Int,
    sectionVerses: List<Verse>,
    assetNames: List<String> = emptyList(),
    onSuccess: (elapsedMs: Long?) -> Unit,
    onShieldLoss: () -> Boolean
) {
    val verse = riddle.verse
    val verseIndex = sectionVerses.indexOfFirst {
        it.book == verse.book && it.chapter == verse.chapter && it.number == verse.number
    }
    
    val effectiveSectionId = verse.originalSectionId ?: sectionId
    val effectiveVerseIndex = verse.originalVerseIndex ?: verseIndex

    key(riddle.type, verse.hashCode(), riddleIndex) {
        when (riddle.type) {
            RiddleType.FILL_WHOLE_VERSE -> {
                FillWholeVerseRiddleScreen(
                    verseText = verse.text,
                    book = verse.book,
                    chapter = verse.chapter,
                    number = verse.number,
                    sectionId = effectiveSectionId,
                    verseIndex = effectiveVerseIndex,
                    riddleIndex = riddleIndex,
                    assetName = assetNames.getOrNull(verseIndex),
                    onSuccess = { onSuccess(null) },
                    onShieldLoss = onShieldLoss
                )
            }
            RiddleType.FILL_WHOLE_SIGLA -> {
                FillWholeSiglaRiddleScreen(
                    verseText = verse.text,
                    book = verse.book,
                    chapter = verse.chapter,
                    number = verse.number,
                    sectionId = effectiveSectionId,
                    verseIndex = effectiveVerseIndex,
                    riddleIndex = riddleIndex,
                    assetName = assetNames.getOrNull(verseIndex),
                    onSuccess = { onSuccess(null) },
                    onShieldLoss = onShieldLoss
                )
            }
            RiddleType.REPEAT_VERSE -> {
                RepeatVerseRiddleScreen(
                    sectionVerses = sectionVerses,
                    sectionId = sectionId,
                    riddleIndex = riddleIndex,
                    assetNames = assetNames,
                    onSuccess = { onSuccess(null) }
                )
            }
            RiddleType.CONNECT_PARTS -> {
                ConnectPartsRiddleScreen(
                    sectionVerses = sectionVerses,
                    sectionId = sectionId,
                    riddleIndex = riddleIndex,
                    assetNames = assetNames,
                    onSuccess = { elapsedMs -> onSuccess(elapsedMs) }
                )
            }
            RiddleType.CONNECT_PAIRS -> {
                ConnectPairsRiddleScreen(
                    sectionVerses = sectionVerses,
                    sectionId = sectionId,
                    riddleIndex = riddleIndex,
                    assetNames = assetNames,
                    onSuccess = { elapsedMs -> onSuccess(elapsedMs) }
                )
            }
            RiddleType.FILL_MORE_WORDS_EASY,
            RiddleType.FILL_MORE_WORDS_NORMAL -> {
                FillWordsRiddleScreen(
                    verseText = verse.text,
                    book = verse.book,
                    chapter = verse.chapter,
                    number = verse.number,
                    isEasy = riddle.type == RiddleType.FILL_MORE_WORDS_EASY,
                    moreWords = true,
                    sectionId = effectiveSectionId,
                    verseIndex = effectiveVerseIndex,
                    riddleIndex = riddleIndex,
                    assetName = assetNames.getOrNull(verseIndex),
                    onSuccess = { onSuccess(null) },
                    onShieldLoss = onShieldLoss
                )
            }
            RiddleType.FILL_WORDS_EASY,
            RiddleType.FILL_WORDS_NORMAL -> {
                FillWordsRiddleScreen(
                    verseText = verse.text,
                    book = verse.book,
                    chapter = verse.chapter,
                    number = verse.number,
                    isEasy = riddle.type == RiddleType.FILL_WORDS_EASY,
                    moreWords = false,
                    sectionId = effectiveSectionId,
                    verseIndex = effectiveVerseIndex,
                    riddleIndex = riddleIndex,
                    assetName = assetNames.getOrNull(verseIndex),
                    onSuccess = { onSuccess(null) },
                    onShieldLoss = onShieldLoss
                )
            }
            RiddleType.FILL_SIGLA_BOOK,
            RiddleType.FILL_SIGLA_CHAPTER,
            RiddleType.FILL_SIGLA_VERSE -> {
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
                    sectionId = effectiveSectionId,
                    verseIndex = effectiveVerseIndex,
                    riddleIndex = riddleIndex,
                    assetName = assetNames.getOrNull(verseIndex),
                    onSuccess = { onSuccess(null) },
                    onShieldLoss = onShieldLoss
                )
            }
            RiddleType.MULTI_QUIZ -> {
                MultiQuizRiddleScreen(
                    verseText = verse.text,
                    book = verse.book,
                    chapter = verse.chapter,
                    number = verse.number,
                    sectionId = effectiveSectionId,
                    verseIndex = effectiveVerseIndex,
                    riddleIndex = riddleIndex,
                    assetName = assetNames.getOrNull(verseIndex),
                    onSuccess = { onSuccess(null) },
                    onShieldLoss = onShieldLoss
                )
            }
            RiddleType.QUIZ_EASY,
            RiddleType.QUIZ_NORMAL -> {
                QuizRiddleScreen(
                    verseText = verse.text,
                    book = verse.book,
                    chapter = verse.chapter,
                    number = verse.number,
                    isEasy = riddle.type == RiddleType.QUIZ_EASY,
                    sectionVerses = sectionVerses,
                    sectionId = effectiveSectionId,
                    verseIndex = effectiveVerseIndex,
                    riddleIndex = riddleIndex,
                    assetName = assetNames.getOrNull(verseIndex),
                    onSuccess = { onSuccess(null) },
                    onShieldLoss = onShieldLoss
                )
            }
            RiddleType.WORD_SCRAMBLE_EASY,
            RiddleType.WORD_SCRAMBLE_NORMAL -> {
                WordScrambleRiddleScreen(
                    verseText = verse.text,
                    book = verse.book,
                    chapter = verse.chapter,
                    number = verse.number,
                    isEasy = riddle.type == RiddleType.WORD_SCRAMBLE_EASY,
                    sectionId = effectiveSectionId,
                    verseIndex = effectiveVerseIndex,
                    riddleIndex = riddleIndex,
                    assetName = assetNames.getOrNull(verseIndex),
                    onSuccess = { onSuccess(null) },
                    onShieldLoss = onShieldLoss
                )
            }
        }
    }
}
