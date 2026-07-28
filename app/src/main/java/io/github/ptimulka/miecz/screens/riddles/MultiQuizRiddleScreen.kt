package io.github.ptimulka.miecz.screens.riddles

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import io.github.ptimulka.miecz.repositories.MnemonicPicturesRepository
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.helpers.BibleDataProvider
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText

private data class AnswerParts(val book: String, val chapter: String, val verse: String)

@Composable
fun MultiQuizRiddleScreen(
    verseText: String,
    book: String,
    chapter: Int,
    number: String,
    sectionId: Int = 0,
    verseIndex: Int = 0,
    assetName: String? = null,
    onSuccess: () -> Unit,
    onShieldLoss: () -> Boolean
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current
    val hintBitmap = remember(sectionId, verseIndex) {
        if (assetName != null || (sectionId > 0 && verseIndex >= 0))
            MnemonicPicturesRepository(context).loadActivePicture(sectionId, verseIndex, assetName)
        else null
    }

    val correctAnswer = AnswerParts(book, chapter.toString(), number)

    val answers = rememberSaveable(correctAnswer) {
        val wrongBooks = mutableSetOf<String>()
        val wrongChapters = mutableSetOf<String>()
        val wrongVerses = mutableSetOf<String>()

        while (wrongBooks.size < 3) {
            val randomRef = BibleDataProvider.getRandomReference()
            if (randomRef.book != correctAnswer.book) {
                wrongBooks.add(randomRef.book)
            }
        }

        while (wrongChapters.size < 3) {
            val randomRef = BibleDataProvider.getRandomReference()
            if (randomRef.chapter.toString() != correctAnswer.chapter) {
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
            if (versePart != correctAnswer.verse) {
                wrongVerses.add(versePart)
            }
        }

        Triple(
            (wrongBooks + correctAnswer.book).shuffled(), 
            (wrongChapters + correctAnswer.chapter).shuffled(), 
            (wrongVerses + correctAnswer.verse).shuffled()
        )
    }

    var selectedBook by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedChapter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedVerse by rememberSaveable { mutableStateOf<String?>(null) }

    var wrongBook by rememberSaveable { mutableStateOf<String?>(null) }
    var wrongChapter by rememberSaveable { mutableStateOf<String?>(null) }
    var wrongVerse by rememberSaveable { mutableStateOf<String?>(null) }
    var showResultDialog by rememberSaveable { mutableStateOf(false) }
    var isAnswerCorrect by rememberSaveable { mutableStateOf(false) }
    var showImagePreview by remember { mutableStateOf(false) }

    val annotatedVerseText = remember(verseText) {
        buildAnnotatedVerseText(verseText)
    }

    if (showImagePreview && hintBitmap != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { showImagePreview = false; showResultDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = hintBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.85f)
            )
        }
        return
    }

    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = { 
                showResultDialog = false 
                if (isAnswerCorrect) onSuccess()
            },
            title = { Text(if (isAnswerCorrect) stringResource(R.string.correct_answer) else stringResource(R.string.wrong_answer), color = if (isAnswerCorrect) colorResource(R.color.correct_answer_green) else Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text(if (isAnswerCorrect) stringResource(R.string.success_message) else stringResource(R.string.failure_message)) },
            confirmButton = {
                TextButton(onClick = { 
                    showResultDialog = false
                    if (isAnswerCorrect) onSuccess()
                }) {
                    Text(stringResource(R.string.ok_button))
                }
            }
        )
    }

    val onCheckClick = {
        wrongBook = if (selectedBook != correctAnswer.book) selectedBook else null
        wrongChapter = if (selectedChapter != correctAnswer.chapter) selectedChapter else null
        wrongVerse = if (selectedVerse != correctAnswer.verse) selectedVerse else null

        isAnswerCorrect = wrongBook == null && wrongChapter == null && wrongVerse == null
        if (isAnswerCorrect && hintBitmap != null) {
            showImagePreview = true
        } else {
            showResultDialog = if (!isAnswerCorrect) { !onShieldLoss() } else true
        }
    }
    
    val clearWrongSelections = {
        wrongBook = null
        wrongChapter = null
        wrongVerse = null
    }

    if (isLandscape) {
        Row(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                .weight(1f)
                .fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text(
                    text = annotatedVerseText,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                AnswerColumns(
                    answers,
                    selectedBook,
                    selectedChapter,
                    selectedVerse,
                    { selectedBook = it; clearWrongSelections() },
                    { selectedChapter = it; clearWrongSelections() },
                    { selectedVerse = it; clearWrongSelections() },
                    wrongBook,
                    wrongChapter,
                    wrongVerse,
                    isLandscape = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                CheckButton(selectedBook != null && selectedChapter != null && selectedVerse != null, onCheckClick, isLandscape = true)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Verse Text
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = annotatedVerseText,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
            AnswerColumns(
                answers,
                selectedBook,
                selectedChapter,
                selectedVerse,
                { selectedBook = it; clearWrongSelections() },
                { selectedChapter = it; clearWrongSelections() },
                { selectedVerse = it; clearWrongSelections() },
                wrongBook,
                wrongChapter,
                wrongVerse,
                isLandscape = false
            )
            Spacer(modifier = Modifier.height(32.dp))
            CheckButton(selectedBook != null && selectedChapter != null && selectedVerse != null, onCheckClick, isLandscape = false)
        }
    }
}

@Composable
fun AnswerColumns(
    answers: Triple<List<String>, List<String>, List<String>>,
    selectedBook: String?,
    selectedChapter: String?,
    selectedVerse: String?,
    onBookSelected: (String) -> Unit,
    onChapterSelected: (String) -> Unit,
    onVerseSelected: (String) -> Unit,
    wrongBook: String?,
    wrongChapter: String?,
    wrongVerse: String?,
    isLandscape: Boolean
) {
    val answerButtonModifier = if (isLandscape) {
        Modifier.height(40.dp)
    } else {
        Modifier.height(50.dp)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Book Column
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(id = R.string.book_caption), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            answers.first.forEach { bookAnswer ->
                AnswerButton(bookAnswer, selectedBook == bookAnswer, { onBookSelected(it) }, bookAnswer == wrongBook, answerButtonModifier)
            }
        }
        // Chapter Column
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(id = R.string.chapter_caption), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            answers.second.forEach { chapterAnswer ->
                AnswerButton(chapterAnswer, selectedChapter == chapterAnswer, { onChapterSelected(it) }, chapterAnswer == wrongChapter, answerButtonModifier)
            }
        }
        // Verse Column
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(id = R.string.verse_caption), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            answers.third.forEach { verseAnswer ->
                AnswerButton(verseAnswer, selectedVerse == verseAnswer, { onVerseSelected(it) }, verseAnswer == wrongVerse, answerButtonModifier)
            }
        }
    }
}

@Composable
fun CheckButton(enabled: Boolean, onClick: () -> Unit, isLandscape: Boolean) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) 40.dp else 50.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.game_button_yellow_dark))
    ) {
        Text(stringResource(id = R.string.check_button), fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AnswerButton(
    text: String,
    isSelected: Boolean,
    onClick: (String) -> Unit,
    isWrong: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onClick(text) },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                isWrong -> colorResource(R.color.wrong_answer_highlight)
                isSelected -> colorResource(id = R.color.game_button_yellow_dark)
                else -> MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(text, fontSize = if(modifier.height(0.dp) == Modifier.height(40.dp)) 14.sp else 16.sp)
    }
}
