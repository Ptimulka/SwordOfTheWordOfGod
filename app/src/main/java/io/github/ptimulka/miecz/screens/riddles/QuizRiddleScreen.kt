package io.github.ptimulka.miecz.screens.riddles

import android.content.res.Configuration
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.game.FullscreenImageOverlay
import io.github.ptimulka.miecz.components.game.RiddleResultDialog
import io.github.ptimulka.miecz.components.game.rememberMnemonicPicture
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.BibleDataProvider
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText

@Composable
fun QuizRiddleScreen(
    verseText: String,
    book: String,
    chapter: Int,
    number: String,
    isEasy: Boolean,
    sectionVerses: List<Verse> = emptyList(),
    sectionId: Int = 0,
    verseIndex: Int = 0,
    assetName: String? = null,
    onSuccess: () -> Unit,
    onShieldLoss: () -> Boolean
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val hintBitmap = rememberMnemonicPicture(sectionId, verseIndex, assetName)

    val correctAnswer = remember(book, chapter, number) { "$book $chapter,$number" }

    val answers = rememberSaveable(
        saver = listSaver(
            save = { stateList -> stateList.toList() },
            restore = { it.toMutableList() }
        )
    ) {
        val wrongAnswers = mutableListOf<String>()
        val totalAnswers = if (isEasy) 4 else 6
        val wrongAnswersNeeded = totalAnswers - 1

        if (!isEasy) {
            val otherVerses = sectionVerses
                .filter { ref -> "${ref.book} ${ref.chapter},${ref.number}" != correctAnswer }
                .shuffled()
            otherVerses.take(3).forEach { wrongAnswers.add("${it.book} ${it.chapter},${it.number}") }
        }

        while (wrongAnswers.size < wrongAnswersNeeded) {
            val randomRef = BibleDataProvider.getRandomReference()
            val randomRefString = randomRef.toString()
            if (randomRefString != correctAnswer && !wrongAnswers.contains(randomRefString)) {
                wrongAnswers.add(randomRefString)
            }
        }
        (wrongAnswers + correctAnswer).shuffled().toMutableList()
    }
    
    var selectedAnswer by rememberSaveable { mutableStateOf<String?>(null) }
    var showResultDialog by rememberSaveable { mutableStateOf(false) }
    var isAnswerCorrect by rememberSaveable { mutableStateOf(false) }
    var showImagePreview by remember { mutableStateOf(false) }

    if (showImagePreview && hintBitmap != null) {
        FullscreenImageOverlay(hintBitmap) { showImagePreview = false; showResultDialog = true }
        return
    }

    if (showResultDialog) {
        RiddleResultDialog(
            isCorrect = isAnswerCorrect,
            onConfirm = {
                showResultDialog = false
                if (isAnswerCorrect) onSuccess()
            }
        )
    }

    val onCheckClick = {
        isAnswerCorrect = (selectedAnswer == correctAnswer)
        if (isAnswerCorrect && hintBitmap != null) {
            showImagePreview = true
        } else {
            showResultDialog = if (!isAnswerCorrect) { !onShieldLoss() } else true
        }
    }

    if (isLandscape) {
        LandscapeQuizLayout(verseText, answers, selectedAnswer, { selectedAnswer = it }, onCheckClick)
    } else {
        PortraitQuizLayout(verseText, answers, selectedAnswer, { selectedAnswer = it }, onCheckClick)
    }
}

@Composable
fun PortraitQuizLayout(
    verseText: String,
    answers: List<String>,
    selectedAnswer: String?,
    onAnswerSelected: (String) -> Unit,
    onCheckClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Verse Text Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buildAnnotatedVerseText(verseText),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState())
            )
        }

        QuizAnswerArea(answers, selectedAnswer, onAnswerSelected, onCheckClick, Modifier.fillMaxWidth(), isLandscape = false)
    }
}

@Composable
fun LandscapeQuizLayout(
    verseText: String,
    answers: List<String>,
    selectedAnswer: String?,
    onAnswerSelected: (String) -> Unit,
    onCheckClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Verse Text
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buildAnnotatedVerseText(verseText),
                style = MaterialTheme.typography.titleLarge, // Slightly smaller for landscape
                textAlign = TextAlign.Center,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right side: Answers and Button
        QuizAnswerArea(answers, selectedAnswer, onAnswerSelected, onCheckClick, Modifier.weight(1f), isLandscape = true)
    }
}

@Composable
fun QuizAnswerArea(
    answers: List<String>,
    selectedAnswer: String?,
    onAnswerSelected: (String) -> Unit,
    onCheckClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLandscape: Boolean
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Answer Buttons Area
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 16.dp) // Less space in landscape
        ) {
            val answerButtonModifier = Modifier.height(if (isLandscape) 48.dp else 60.dp)
            val isEasy = answers.size <= 4
            val useTwoColumns = isLandscape || !isEasy

            if (useTwoColumns) {
                for (i in answers.indices step 2) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val answer1 = answers[i]
                        Box(Modifier.weight(1f)) { AnswerButton(answer1, selectedAnswer == answer1, { onAnswerSelected(answer1) }, answerButtonModifier) }
                        if (i + 1 < answers.size) {
                            val answer2 = answers[i + 1]
                            Box(Modifier.weight(1f)) { AnswerButton(answer2, selectedAnswer == answer2, { onAnswerSelected(answer2) }, answerButtonModifier) }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                answers.forEach { answer ->
                    AnswerButton(answer, selectedAnswer == answer, { onAnswerSelected(answer) }, answerButtonModifier)
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 32.dp))

        // Check Button
        Button(
            onClick = onCheckClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isLandscape) 40.dp else 50.dp),
            enabled = selectedAnswer != null,
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.game_button_yellow_dark))
        ) {
            Text(stringResource(id = R.string.check_button), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AnswerButton(
    answer: String, 
    isSelected: Boolean, 
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) colorResource(id = R.color.game_button_yellow_dark) else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(answer, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

