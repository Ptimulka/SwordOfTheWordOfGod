package io.github.ptimulka.miecz.screens.riddles.fill_whole_sigla

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.game.FullscreenImageOverlay
import io.github.ptimulka.miecz.components.game.RiddleCheckButton
import io.github.ptimulka.miecz.components.game.RiddleHint
import io.github.ptimulka.miecz.components.game.RiddleResultDialog
import io.github.ptimulka.miecz.components.game.VerseDisplay
import io.github.ptimulka.miecz.screens.riddles.base.RiddleEffect
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase

@Composable
fun FillWholeSiglaRiddleScreen(
    verseText: String,
    book: String,
    chapter: Int,
    number: String,
    sectionId: Int = 0,
    verseIndex: Int = 0,
    riddleIndex: Int = 0,
    assetName: String? = null,
    onSuccess: () -> Unit,
    onShieldLoss: () -> Boolean
) {
    val args = remember(book, chapter, number, sectionId, verseIndex, riddleIndex, assetName) {
        FillWholeSiglaArgs(
            book = book,
            chapter = chapter,
            number = number,
            sectionId = sectionId,
            verseIndex = verseIndex,
            assetName = assetName,
            hasHint = true
        )
    }

    val vm: FillWholeSiglaViewModel = hiltViewModel<FillWholeSiglaViewModel, FillWholeSiglaViewModel.Factory>(
        key = "FillWholeSiglaVM_${book}_${chapter}_${number}_${riddleIndex}"
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
            LandscapeFillWholeSiglaLayout(
                verseText = verseText,
                state = state,
                onEvent = vm::onEvent
            )
        } else {
            PortraitFillWholeSiglaLayout(
                verseText = verseText,
                state = state,
                onEvent = vm::onEvent
            )
        }

        val hint = state.hintBitmap
        if ((phase is RiddlePhase.ShowingHint || phase is RiddlePhase.ShowingReward) && hint != null) {
            FullscreenImageOverlay(hint) { vm.onEvent(FillWholeSiglaEvent.DismissHint) }
        } else if (showResultDialog && phase is RiddlePhase.Result) {
            RiddleResultDialog(
                isCorrect = phase.correct,
                onConfirm = { vm.onEvent(FillWholeSiglaEvent.DismissResult) }
            )
        }
    }
}

@Composable
private fun PortraitFillWholeSiglaLayout(
    verseText: String,
    state: FillWholeSiglaUiState,
    onEvent: (FillWholeSiglaEvent) -> Unit
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
        VerseDisplay(Modifier, verseText)
        Spacer(Modifier.height(16.dp))
        WholeSiglaInputArea(state, onEvent)
        Spacer(Modifier.height(16.dp))
        RiddleCheckButton(
            enabled = state.allFieldsFilled,
            onCheck = {
                keyboardController?.hide()
                onEvent(FillWholeSiglaEvent.Check)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}

@Composable
private fun LandscapeFillWholeSiglaLayout(
    verseText: String,
    state: FillWholeSiglaUiState,
    onEvent: (FillWholeSiglaEvent) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

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
        VerseDisplay(Modifier.weight(1f), verseText)
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WholeSiglaInputArea(state, onEvent)
            Spacer(Modifier.height(32.dp))
            RiddleCheckButton(
                enabled = state.allFieldsFilled,
                onCheck = {
                    keyboardController?.hide()
                    onEvent(FillWholeSiglaEvent.Check)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )
        }
    }
}

@Composable
private fun WholeSiglaInputArea(
    state: FillWholeSiglaUiState,
    onEvent: (FillWholeSiglaEvent) -> Unit
) {
    val chapterFocusRequester = remember { FocusRequester() }
    val verseFocusRequester = remember { FocusRequester() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(id = R.string.fill_whole_sigla_caption),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SiglaPartTextField(
                value = state.bookInput,
                onValueChange = { onEvent(FillWholeSiglaEvent.UpdateBook(it)) },
                isError = state.wrongIndices.contains(0),
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { chapterFocusRequester.requestFocus() })
            )
            Text(",", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(8.dp))
            SiglaPartTextField(
                value = state.chapterInput,
                onValueChange = { onEvent(FillWholeSiglaEvent.UpdateChapter(it)) },
                isError = state.wrongIndices.contains(1),
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { verseFocusRequester.requestFocus() }),
                focusRequester = chapterFocusRequester
            )
            Text(",", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(8.dp))
            SiglaPartTextField(
                value = state.verseInput,
                onValueChange = { onEvent(FillWholeSiglaEvent.UpdateVerse(it)) },
                isError = state.wrongIndices.contains(2),
                keyboardType = KeyboardType.Number,
                focusRequester = verseFocusRequester
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        RiddleHint(text = stringResource(id = R.string.fill_sigla_verse_range_hint))
        Spacer(modifier = Modifier.height(4.dp))
        RiddleHint(text = stringResource(id = R.string.no_diacritics_hint))
    }
}

@Composable
private fun SiglaPartTextField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .width(80.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = keyboardActions,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        ),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                    .padding(vertical = 8.dp, horizontal = 8.dp)
            ) {
                innerTextField()
            }
        }
    )
}
