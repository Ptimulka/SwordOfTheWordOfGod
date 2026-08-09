package io.github.ptimulka.miecz.screens.riddles.multi_quiz

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.game.FullscreenImageOverlay
import io.github.ptimulka.miecz.components.game.RiddleCheckButton
import io.github.ptimulka.miecz.components.game.RiddleResultDialog
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText
import io.github.ptimulka.miecz.repositories.UserMnemonicPicturesRepository
import io.github.ptimulka.miecz.screens.riddles.base.RiddleEffect
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase

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
    val context = LocalContext.current

    val vm: MultiQuizViewModel = viewModel(
        key = "MultiQuizVM_${book}_${chapter}_${number}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MultiQuizViewModel(
                    MultiQuizArgs(
                        book = book,
                        chapter = chapter,
                        number = number,
                        sectionId = sectionId,
                        verseIndex = verseIndex,
                        assetName = assetName,
                        hasHint = true
                    ),
                    UserMnemonicPicturesRepository(context)
                ) as T
            }
        }
    )

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

    val annotatedVerseText = remember(verseText) {
        buildAnnotatedVerseText(verseText)
    }

    Box(Modifier.fillMaxSize()) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            LandscapeMultiQuizLayout(
                annotatedVerseText = annotatedVerseText,
                state = state,
                onEvent = vm::onEvent
            )
        } else {
            PortraitMultiQuizLayout(
                annotatedVerseText = annotatedVerseText,
                state = state,
                onEvent = vm::onEvent
            )
        }

        if ((phase is RiddlePhase.ShowingHint || phase is RiddlePhase.ShowingReward) && state.hintBitmap != null) {
            FullscreenImageOverlay(state.hintBitmap!!) { vm.onEvent(MultiQuizEvent.DismissHint) }
        } else if (showResultDialog && phase is RiddlePhase.Result) {
            RiddleResultDialog(
                isCorrect = phase.correct,
                onConfirm = { vm.onEvent(MultiQuizEvent.DismissResult) }
            )
        }
    }
}

@Composable
fun PortraitMultiQuizLayout(
    annotatedVerseText: androidx.compose.ui.text.AnnotatedString,
    state: MultiQuizUiState,
    onEvent: (MultiQuizEvent) -> Unit
) {
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
            state = state,
            onEvent = onEvent,
            isLandscape = false
        )
        Spacer(modifier = Modifier.height(32.dp))
        CheckButton(state.checkEnabled, { onEvent(MultiQuizEvent.Check) }, isLandscape = false)
    }
}

@Composable
fun LandscapeMultiQuizLayout(
    annotatedVerseText: androidx.compose.ui.text.AnnotatedString,
    state: MultiQuizUiState,
    onEvent: (MultiQuizEvent) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
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
                state = state,
                onEvent = onEvent,
                isLandscape = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            CheckButton(state.checkEnabled, { onEvent(MultiQuizEvent.Check) }, isLandscape = true)
        }
    }
}

@Composable
fun AnswerColumns(
    state: MultiQuizUiState,
    onEvent: (MultiQuizEvent) -> Unit,
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
            state.bookAnswers.forEach { bookAnswer ->
                AnswerButton(
                    text = bookAnswer,
                    isSelected = state.selectedBook == bookAnswer,
                    onClick = { onEvent(MultiQuizEvent.SelectBook(it)) },
                    isWrong = bookAnswer == state.wrongBook,
                    modifier = answerButtonModifier
                )
            }
        }
        // Chapter Column
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(id = R.string.chapter_caption), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            state.chapterAnswers.forEach { chapterAnswer ->
                AnswerButton(
                    text = chapterAnswer,
                    isSelected = state.selectedChapter == chapterAnswer,
                    onClick = { onEvent(MultiQuizEvent.SelectChapter(it)) },
                    isWrong = chapterAnswer == state.wrongChapter,
                    modifier = answerButtonModifier
                )
            }
        }
        // Verse Column
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(id = R.string.verse_caption), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            state.verseAnswers.forEach { verseAnswer ->
                AnswerButton(
                    text = verseAnswer,
                    isSelected = state.selectedVerse == verseAnswer,
                    onClick = { onEvent(MultiQuizEvent.SelectVerse(it)) },
                    isWrong = verseAnswer == state.wrongVerse,
                    modifier = answerButtonModifier
                )
            }
        }
    }
}

@Composable
private fun CheckButton(enabled: Boolean, onClick: () -> Unit, isLandscape: Boolean) {
    RiddleCheckButton(
        enabled = enabled,
        onCheck = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) 40.dp else 50.dp)
    )
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
