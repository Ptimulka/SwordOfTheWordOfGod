package io.github.ptimulka.miecz.screens.riddles.fill_whole_verse

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.game.FullscreenImageOverlay
import io.github.ptimulka.miecz.components.game.RiddleCheckButton
import io.github.ptimulka.miecz.components.game.RiddleHint
import io.github.ptimulka.miecz.components.game.RiddleResultDialog
import io.github.ptimulka.miecz.helpers.DiffPart
import io.github.ptimulka.miecz.helpers.DiffType
import io.github.ptimulka.miecz.helpers.rememberSpeechLauncher
import io.github.ptimulka.miecz.screens.riddles.base.RiddleEffect
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase

@Composable
fun FillWholeVerseRiddleScreen(
    verseText: String,
    book: String,
    chapter: Int,
    number: String,
    sectionId: Int,
    verseIndex: Int,
    riddleIndex: Int = 0,
    assetName: String? = null,
    onSuccess: () -> Unit,
    onShieldLoss: () -> Boolean
) {
    val args = remember(verseText, book, chapter, number, sectionId, verseIndex, riddleIndex, assetName) {
        FillWholeVerseArgs(
            verseText = verseText,
            book = book,
            chapter = chapter,
            number = number,
            sectionId = sectionId,
            verseIndex = verseIndex,
            assetName = assetName,
            hasHint = true
        )
    }

    val vm: FillWholeVerseViewModel = hiltViewModel<FillWholeVerseViewModel, FillWholeVerseViewModel.Factory>(
        key = "FillWholeVerseVM_${book}_${chapter}_${number}_${riddleIndex}"
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

    val speechLauncher = rememberSpeechLauncher { recognized -> 
        vm.onEvent(FillWholeVerseEvent.UpdateInput(recognized)) 
    }

    Box(Modifier.fillMaxSize()) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            LandscapeFillWholeVerseLayout(
                state = state,
                onEvent = vm::onEvent,
                speechLauncher = speechLauncher
            )
        } else {
            PortraitFillWholeVerseLayout(
                state = state,
                onEvent = vm::onEvent,
                speechLauncher = speechLauncher
            )
        }

        when (phase) {
            is RiddlePhase.ShowingHint -> {
                FullscreenImageOverlay(phase.bitmap) { vm.onEvent(FillWholeVerseEvent.DismissHint) }
            }
            is RiddlePhase.ShowingReward -> {
                FullscreenImageOverlay(phase.bitmap) { vm.onEvent(FillWholeVerseEvent.DismissHint) }
            }
            is RiddlePhase.Result -> {
                if (showResultDialog) {
                    RiddleResultDialog(
                        isCorrect = phase.correct,
                        dismissable = false,
                        onConfirm = { vm.onEvent(FillWholeVerseEvent.DismissResult) },
                        extraContent = {
                            if (state.similarityScore < 100f) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(R.string.similarity_score, state.similarityScore))
                                Text(
                                    text = stringResource(id = R.string.required_similarity_info),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                if (state.similarityScore >= 50f) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    DiffView(diffs = state.diffs)
                                }
                            }
                        }
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun PortraitFillWholeVerseLayout(
    state: FillWholeVerseUiState,
    onEvent: (FillWholeVerseEvent) -> Unit,
    speechLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(
                WindowInsets.ime.only(WindowInsetsSides.Bottom).asPaddingValues()
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        VerseInfoArea(state.book, state.chapter, state.number, state.hintBitmap != null) { 
            onEvent(FillWholeVerseEvent.ShowHint) 
        }

        InputArea(
            userInput = state.userInput,
            onValueChange = { onEvent(FillWholeVerseEvent.UpdateInput(it)) },
            isLandscape = false,
            speechLauncher = speechLauncher
        )

        Spacer(modifier = Modifier.height(8.dp))
        RiddleHint(text = stringResource(id = R.string.skip_possibility_info))
        Spacer(modifier = Modifier.height(8.dp))
        RiddleHint(text = stringResource(id = R.string.no_diacritics_hint))
        Spacer(modifier = Modifier.height(8.dp))
        RiddleCheckButton(
            enabled = state.userInput.isNotBlank(),
            onCheck = {
                keyboardController?.hide()
                onEvent(FillWholeVerseEvent.Check)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}

@Composable
private fun LandscapeFillWholeVerseLayout(
    state: FillWholeVerseUiState,
    onEvent: (FillWholeVerseEvent) -> Unit,
    speechLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(
                WindowInsets.ime.only(WindowInsetsSides.Bottom).asPaddingValues()
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        VerseInfoArea(state.book, state.chapter, state.number, state.hintBitmap != null) { 
            onEvent(FillWholeVerseEvent.ShowHint) 
        }

        InputArea(
            userInput = state.userInput,
            onValueChange = { onEvent(FillWholeVerseEvent.UpdateInput(it)) },
            isLandscape = true,
            speechLauncher = speechLauncher
        )

        Spacer(modifier = Modifier.height(8.dp))
        RiddleHint(text = stringResource(id = R.string.skip_possibility_info))
        Spacer(modifier = Modifier.height(8.dp))
        RiddleHint(text = stringResource(id = R.string.no_diacritics_hint))
        Spacer(modifier = Modifier.height(8.dp))
        RiddleCheckButton(
            enabled = state.userInput.isNotBlank(),
            onCheck = {
                keyboardController?.hide()
                onEvent(FillWholeVerseEvent.Check)
            },
            Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}

@Composable
private fun VerseInfoArea(
    book: String,
    chapter: Int,
    number: String,
    hasHint: Boolean,
    onHintClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Text(
            text = "$book $chapter,$number",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            color = colorResource(id = R.color.game_button_yellow_dark)
        )
        if (hasHint) {
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onHintClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_hint),
                    contentDescription = stringResource(R.string.image_hint),
                    tint = colorResource(id = R.color.game_button_yellow_dark)
                )
            }
        }
    }
}

@Composable
private fun InputArea(
    userInput: String,
    onValueChange: (String) -> Unit,
    isLandscape: Boolean,
    speechLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>
) {
    val textFieldHeight = if(isLandscape) 130.dp else 290.dp
    val extraPrompt = stringResource(R.string.speak_now)
    
    OutlinedTextField(
        value = userInput,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(textFieldHeight),
        label = { Text(stringResource(id = R.string.fill_whole_verse_caption)) },
        trailingIcon = {
            IconButton(onClick = {
                speechLauncher.launch(io.github.ptimulka.miecz.helpers.createPolishSpeechIntent(prompt = extraPrompt))
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.microphone),
                    contentDescription = stringResource(R.string.speak_now)
                )
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiffView(diffs: List<DiffPart>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        diffs.forEach { part ->
            Text(
                text = part.text,
                color = Color.Black,
                modifier = Modifier
                    .background(
                        when (part.type) {
                            DiffType.INSERT -> colorResource(R.color.wrong_answer_highlight)
                            DiffType.DELETE -> colorResource(R.color.diff_delete_yellow)
                            DiffType.EQUAL -> colorResource(R.color.diff_correct_green)
                        }
                    )
                    .padding(horizontal = 3.dp, vertical = 1.dp),
                textDecoration = if (part.type == DiffType.INSERT) TextDecoration.LineThrough else null
            )
        }
    }
}
