package io.github.ptimulka.miecz.screens.riddles.fill_words

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.game.FullscreenImageOverlay
import io.github.ptimulka.miecz.components.game.RiddleCheckButton
import io.github.ptimulka.miecz.components.game.RiddleHint
import io.github.ptimulka.miecz.components.game.RiddleResultDialog
import io.github.ptimulka.miecz.screens.riddles.base.RiddleEffect
import io.github.ptimulka.miecz.screens.riddles.base.RiddlePhase

@Composable
fun FillWordsRiddleScreen(
    verseText: String,
    book: String,
    chapter: Int,
    number: String,
    isEasy: Boolean,
    moreWords: Boolean,
    sectionId: Int,
    verseIndex: Int,
    riddleIndex: Int = 0,
    assetName: String? = null,
    onSuccess: () -> Unit,
    onShieldLoss: () -> Boolean
) {
    val args = remember(verseText, book, chapter, number, isEasy, moreWords, sectionId, verseIndex, riddleIndex, assetName) {
        FillWordsArgs(
            verseText = verseText,
            book = book,
            chapter = chapter,
            number = number,
            isEasy = isEasy,
            moreWords = moreWords,
            sectionId = sectionId,
            verseIndex = verseIndex,
            assetName = assetName,
            hasHint = true
        )
    }

    val vm: FillWordsViewModel = hiltViewModel<FillWordsViewModel, FillWordsViewModel.Factory>(
        key = "FillWordsVM_${book}_${chapter}_${number}_${isEasy}_${moreWords}_${riddleIndex}"
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
        FillWordsContent(
            state = state,
            isEasy = isEasy,
            onEvent = vm::onEvent
        )

        when (phase) {
            is RiddlePhase.ShowingHint -> {
                FullscreenImageOverlay(phase.bitmap) { vm.onEvent(FillWordsEvent.DismissHint) }
            }
            is RiddlePhase.ShowingReward -> {
                FullscreenImageOverlay(phase.bitmap) { vm.onEvent(FillWordsEvent.DismissHint) }
            }
            is RiddlePhase.Result -> {
                if (showResultDialog) {
                    RiddleResultDialog(
                        isCorrect = phase.correct,
                        onConfirm = { vm.onEvent(FillWordsEvent.DismissResult) }
                    )
                }
            }
            else -> {}
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FillWordsContent(
    state: FillWordsUiState,
    isEasy: Boolean,
    onEvent: (FillWordsEvent) -> Unit
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = "${state.book} ${state.chapter},${state.number}",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                color = colorResource(id = R.color.game_button_yellow_dark)
            )
            if (state.hintBitmap != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { onEvent(FillWordsEvent.ShowHint) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_hint),
                        contentDescription = stringResource(R.string.image_hint),
                        tint = colorResource(id = R.color.game_button_yellow_dark)
                    )
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            var inputIndex = 0
            state.verseParts.forEach { part ->
                when (part) {
                    is VersePart.StaticText -> {
                        Text(
                            text = " ${part.text}",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 6.dp),
                            color = if (part.isGreyed) Color.Gray else Color.Unspecified
                        )
                    }
                    is VersePart.WordToFill -> {
                        val currentIndex = inputIndex
                        CompactOutlinedTextField(
                            value = state.userInputs.getOrElse(currentIndex) { "" },
                            onValueChange = { onEvent(FillWordsEvent.UpdateInput(currentIndex, it)) },
                            placeholder = {
                                if (isEasy) Text(
                                    part.hint,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            isError = state.wrongInputIndices.contains(currentIndex),
                            modifier = Modifier
                                .width((part.correctWord.length * 12).dp + 32.dp)
                                .height(40.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        inputIndex++
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        RiddleHint(text = stringResource(id = R.string.no_diacritics_hint))

        Spacer(modifier = Modifier.height(8.dp))

        RiddleCheckButton(
            enabled = state.allFieldsFilled,
            onCheck = { onEvent(FillWordsEvent.Check) }
        )
    }
}

@Composable
private fun CompactOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean,
    placeholder: @Composable () -> Unit,
    keyboardOptions: KeyboardOptions
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
        modifier = modifier,
        singleLine = true,
        keyboardOptions = keyboardOptions,
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
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            ) {
                if (value.isEmpty() && !isFocused) {
                    placeholder()
                }
                innerTextField()
            }
        }
    )
}
