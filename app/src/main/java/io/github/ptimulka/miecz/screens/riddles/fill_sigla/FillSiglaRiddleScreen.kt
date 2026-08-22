package io.github.ptimulka.miecz.screens.riddles.fill_sigla

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.game.FullscreenImageOverlay
import io.github.ptimulka.miecz.components.game.RiddleCheckButton
import io.github.ptimulka.miecz.components.game.RiddleHint
import io.github.ptimulka.miecz.components.game.RiddleResultDialog
import io.github.ptimulka.miecz.components.game.VerseDisplay
import io.github.ptimulka.miecz.screens.riddles.base.RiddleEffect
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase

@Composable
fun FillSiglaRiddleScreen(
    verseText: String,
    book: String,
    chapter: Int,
    number: String,
    fillType: FillSiglaType,
    sectionId: Int = 0,
    verseIndex: Int = 0,
    riddleIndex: Int = 0,
    assetName: String? = null,
    onSuccess: () -> Unit,
    onShieldLoss: () -> Boolean
) {
    val args = remember(book, chapter, number, fillType, sectionId, verseIndex, riddleIndex, assetName) {
        FillSiglaArgs(
            book = book,
            chapter = chapter,
            number = number,
            fillType = fillType,
            sectionId = sectionId,
            verseIndex = verseIndex,
            assetName = assetName,
            hasHint = true
        )
    }

    val vm: FillSiglaViewModel = hiltViewModel<FillSiglaViewModel, FillSiglaViewModel.Factory>(
        key = "FillSiglaVM_${book}_${chapter}_${number}_${fillType}_${riddleIndex}"
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

    Box(Modifier.fillMaxSize()) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            LandscapeFillSiglaLayout(
                verseText = verseText,
                fillType = fillType,
                book = book,
                chapter = chapter,
                number = number,
                state = state,
                onEvent = vm::onEvent
            )
        } else {
            PortraitFillSiglaLayout(
                verseText = verseText,
                fillType = fillType,
                book = book,
                chapter = chapter,
                number = number,
                state = state,
                onEvent = vm::onEvent
            )
        }

        when (phase) {
            is RiddlePhase.ShowingHint -> {
                FullscreenImageOverlay(phase.bitmap) { vm.onEvent(FillSiglaEvent.DismissHint) }
            }
            is RiddlePhase.ShowingReward -> {
                FullscreenImageOverlay(phase.bitmap) { vm.onEvent(FillSiglaEvent.DismissHint) }
            }
            is RiddlePhase.Result -> {
                if (showResultDialog) {
                    RiddleResultDialog(
                        isCorrect = phase.correct,
                        onConfirm = { vm.onEvent(FillSiglaEvent.DismissResult) }
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun PortraitFillSiglaLayout(
    verseText: String,
    fillType: FillSiglaType,
    book: String,
    chapter: Int,
    number: String,
    state: FillSiglaUiState,
    onEvent: (FillSiglaEvent) -> Unit
) {
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
        VerseDisplay(Modifier, verseText)
        Spacer(Modifier.height(16.dp))
        SiglaInputArea(fillType, state.userInput, { onEvent(FillSiglaEvent.UpdateInput(it)) }, book, chapter, number)
        Spacer(Modifier.height(16.dp))
        RiddleCheckButton(state.userInput.isNotBlank(), { onEvent(FillSiglaEvent.Check) })
    }
}

@Composable
private fun LandscapeFillSiglaLayout(
    verseText: String,
    fillType: FillSiglaType,
    book: String,
    chapter: Int,
    number: String,
    state: FillSiglaUiState,
    onEvent: (FillSiglaEvent) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(
                WindowInsets.ime.only(WindowInsetsSides.Bottom).asPaddingValues()
            ),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center
    ) {
        VerseDisplay(Modifier.weight(2f), verseText)
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SiglaInputArea(fillType, state.userInput, { onEvent(FillSiglaEvent.UpdateInput(it)) }, book, chapter, number)
            Spacer(Modifier.height(32.dp))
            RiddleCheckButton(state.userInput.isNotBlank(), { onEvent(FillSiglaEvent.Check) })
        }
    }
}

@Composable
private fun SiglaInputArea(
    fillType: FillSiglaType,
    userInput: String,
    onUserInputChanged: (String) -> Unit,
    book: String,
    chapter: Int,
    number: String
) {
    val captionRes = when (fillType) {
        FillSiglaType.BOOK -> R.string.fill_book_caption
        FillSiglaType.CHAPTER -> R.string.fill_chapter_caption
        FillSiglaType.VERSE -> R.string.fill_verse_caption
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(id = captionRes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            when (fillType) {
                FillSiglaType.BOOK -> {
                    SiglaTextField(value = userInput, onValueChange = onUserInputChanged)
                    Text(", $chapter,$number", style = MaterialTheme.typography.headlineSmall)
                }
                FillSiglaType.CHAPTER -> {
                    Text("$book ", style = MaterialTheme.typography.headlineSmall)
                    SiglaTextField(value = userInput, onValueChange = onUserInputChanged, keyboardType = KeyboardType.Number)
                    Text(",$number", style = MaterialTheme.typography.headlineSmall)
                }
                FillSiglaType.VERSE -> {
                    Text("$book $chapter,", style = MaterialTheme.typography.headlineSmall)
                    SiglaTextField(value = userInput, onValueChange = onUserInputChanged, keyboardType = KeyboardType.Number)
                }
            }
        }
        if (fillType == FillSiglaType.VERSE) {
            Spacer(modifier = Modifier.height(4.dp))
            RiddleHint(text = stringResource(id = R.string.fill_sigla_verse_range_hint))
        }
        if (fillType == FillSiglaType.BOOK) {
            Spacer(modifier = Modifier.height(4.dp))
            RiddleHint(text = stringResource(id = R.string.no_diacritics_hint))
        }
    }
}

@Composable
fun SiglaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.width(100.dp),
        textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true
    )
}
