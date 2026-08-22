package io.github.ptimulka.miecz.screens.riddles.quiz

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.game.FullscreenImageOverlay
import io.github.ptimulka.miecz.components.game.RiddleCheckButton
import io.github.ptimulka.miecz.components.game.RiddleResultDialog
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText
import io.github.ptimulka.miecz.screens.riddles.base.RiddleEffect
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase

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
    riddleIndex: Int = 0,
    assetName: String? = null,
    onSuccess: () -> Unit,
    onShieldLoss: () -> Boolean
) {
    val args = remember(verseText, book, chapter, number, isEasy, sectionVerses, sectionId, verseIndex, riddleIndex, assetName) {
        QuizArgs(
            verseText = verseText,
            book = book,
            chapter = chapter,
            number = number,
            isEasy = isEasy,
            sectionVerses = sectionVerses,
            sectionId = sectionId,
            verseIndex = verseIndex,
            assetName = assetName,
            hasHint = true
        )
    }

    val vm: QuizViewModel = hiltViewModel<QuizViewModel, QuizViewModel.Factory>(
        key = "QuizVM_${book}_${chapter}_${number}_${isEasy}_${riddleIndex}"
    ) { factory ->
        factory.create(args)
    }

    val state by vm.state.collectAsStateWithLifecycle()
    var showResultDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(vm.effects) {
        vm.effects.collect { effect ->
            when (effect) {
                is RiddleEffect.Success -> onSuccess()
            }
        }
    }

    val phase = state.phase
    LaunchedEffect(phase) {
        when (phase) {
            is RiddlePhase.Result -> {
                showResultDialog = if (!phase.correct) {
                    !onShieldLoss()
                } else {
                    true
                }
            }
            RiddlePhase.Answering -> {
                showResultDialog = false
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            LandscapeQuizLayout(
                verseText = verseText,
                state = state,
                onEvent = vm::onEvent
            )
        } else {
            PortraitQuizLayout(
                verseText = verseText,
                state = state,
                onEvent = vm::onEvent
            )
        }

        when (phase) {
            is RiddlePhase.ShowingHint -> {
                FullscreenImageOverlay(phase.bitmap) { vm.onEvent(QuizEvent.DismissHint) }
            }
            is RiddlePhase.ShowingReward -> {
                FullscreenImageOverlay(phase.bitmap) { vm.onEvent(QuizEvent.DismissHint) }
            }
            is RiddlePhase.Result -> {
                if (showResultDialog) {
                    RiddleResultDialog(
                        isCorrect = phase.correct,
                        onConfirm = { vm.onEvent(QuizEvent.DismissResult) }
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
fun PortraitQuizLayout(
    verseText: String,
    state: QuizUiState,
    onEvent: (QuizEvent) -> Unit
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

        QuizAnswerArea(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.fillMaxWidth(),
            isLandscape = false
        )
    }
}

@Composable
fun LandscapeQuizLayout(
    verseText: String,
    state: QuizUiState,
    onEvent: (QuizEvent) -> Unit
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
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right side: Answers and Button
        QuizAnswerArea(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.weight(1f),
            isLandscape = true
        )
    }
}

@Composable
fun QuizAnswerArea(
    state: QuizUiState,
    onEvent: (QuizEvent) -> Unit,
    modifier: Modifier = Modifier,
    isLandscape: Boolean
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Answer Buttons Area
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 16.dp)
        ) {
            val answerButtonModifier = Modifier.height(if (isLandscape) 48.dp else 60.dp)
            val useTwoColumns = isLandscape || state.answers.size > 4

            if (useTwoColumns) {
                for (i in state.answers.indices step 2) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val answer1 = state.answers[i]
                        Box(Modifier.weight(1f)) {
                            AnswerButton(
                                answer = answer1,
                                isSelected = state.selectedAnswer == answer1,
                                onClick = { onEvent(QuizEvent.Select(answer1)) },
                                modifier = answerButtonModifier
                            )
                        }
                        if (i + 1 < state.answers.size) {
                            val answer2 = state.answers[i + 1]
                            Box(Modifier.weight(1f)) {
                                AnswerButton(
                                    answer = answer2,
                                    isSelected = state.selectedAnswer == answer2,
                                    onClick = { onEvent(QuizEvent.Select(answer2)) },
                                    modifier = answerButtonModifier
                                )
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                state.answers.forEach { answer ->
                    AnswerButton(
                        answer = answer,
                        isSelected = state.selectedAnswer == answer,
                        onClick = { onEvent(QuizEvent.Select(answer)) },
                        modifier = answerButtonModifier
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 32.dp))

        // Check Button
        RiddleCheckButton(
            enabled = state.checkEnabled,
            onCheck = { onEvent(QuizEvent.Check) },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isLandscape) 40.dp else 50.dp)
        )
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
