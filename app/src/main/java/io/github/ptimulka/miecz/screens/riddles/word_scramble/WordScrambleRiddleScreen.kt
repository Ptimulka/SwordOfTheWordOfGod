package io.github.ptimulka.miecz.screens.riddles.word_scramble

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import io.github.ptimulka.miecz.components.game.rememberMnemonicPicture
import io.github.ptimulka.miecz.data.WordItem

@Composable
fun WordScrambleRiddleScreen(
    verseText: String,
    book: String,
    chapter: Int,
    number: String,
    isEasy: Boolean,
    sectionId: Int,
    verseIndex: Int,
    assetName: String? = null,
    onSuccess: () -> Unit,
    onShieldLoss: () -> Boolean
) {
    val vm: WordScrambleViewModel = viewModel(
        key = "WordScrambleVM_${book}_${chapter}_${number}_${isEasy}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WordScrambleViewModel(
                    WordScrambleArgs(
                        verseText = verseText,
                        book = book,
                        chapter = chapter,
                        number = number,
                        isEasy = isEasy
                    )
                ) as T
            }
        }
    )

    val state by vm.state.collectAsStateWithLifecycle()
    val hintBitmap = rememberMnemonicPicture(sectionId, verseIndex, assetName)
    var showResultDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(vm.effects) {
        vm.effects.collect { effect ->
            when (effect) {
                WordScrambleEffect.Success -> onSuccess()
            }
        }
    }

    val phase = state.phase
    LaunchedEffect(phase) {
        when (phase) {
            is WordScrambleUiState.Phase.Result -> {
                showResultDialog = if (!phase.correct) {
                    !onShieldLoss()
                } else {
                    true
                }
            }
            WordScrambleUiState.Phase.Answering -> {
                showResultDialog = false
            }
            else -> {}
        }
    }

    Box(Modifier.fillMaxSize()) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            LandscapeWordScrambleLayout(
                state = state,
                hintBitmap = hintBitmap,
                onEvent = vm::onEvent
            )
        } else {
            PortraitWordScrambleLayout(
                state = state,
                hintBitmap = hintBitmap,
                onEvent = vm::onEvent
            )
        }

        if (phase is WordScrambleUiState.Phase.ShowingHintImage && hintBitmap != null) {
            FullscreenImageOverlay(hintBitmap) { vm.onEvent(WordScrambleEvent.DismissHint) }
        } else if (showResultDialog && phase is WordScrambleUiState.Phase.Result) {
            RiddleResultDialog(
                isCorrect = phase.correct,
                onConfirm = { vm.onEvent(WordScrambleEvent.DismissResult) }
            )
        }
    }
}

@Composable
private fun PortraitWordScrambleLayout(
    state: WordScrambleUiState,
    hintBitmap: android.graphics.Bitmap?,
    onEvent: (WordScrambleEvent) -> Unit
) {
    var showHintDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScrambleTopBar(state.book, state.chapter, state.number, hintBitmap) { onEvent(WordScrambleEvent.ShowHint) }

        Column(Modifier.fillMaxSize().padding(16.dp)) {
            AnswerArea(Modifier.weight(1f), state, onEvent)
            Spacer(Modifier.height(16.dp))
            SelectionArea(Modifier.weight(1f), state.availableWords) { onEvent(WordScrambleEvent.PlaceWord(it)) }
            Spacer(Modifier.height(16.dp))
            Controls(Modifier.fillMaxWidth(), state.checkEnabled, 
                onReset = { onEvent(WordScrambleEvent.Reset) }, 
                onCheck = { onEvent(WordScrambleEvent.Check) }
            )
        }
    }

    if (showHintDialog && hintBitmap != null) {
        FullscreenImageOverlay(hintBitmap) { showHintDialog = false }
    }
}

@Composable
private fun LandscapeWordScrambleLayout(
    state: WordScrambleUiState,
    hintBitmap: android.graphics.Bitmap?,
    onEvent: (WordScrambleEvent) -> Unit
) {
    var showHintDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScrambleTopBar(state.book, state.chapter, state.number, hintBitmap) { onEvent(WordScrambleEvent.ShowHint) }

        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
            AnswerArea(Modifier.weight(1f).fillMaxHeight(), state, onEvent)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                SelectionArea(Modifier.weight(1f), state.availableWords) { onEvent(WordScrambleEvent.PlaceWord(it)) }
                Spacer(Modifier.height(16.dp))
                Controls(Modifier.fillMaxWidth(), state.checkEnabled, 
                    onReset = { onEvent(WordScrambleEvent.Reset) }, 
                    onCheck = { onEvent(WordScrambleEvent.Check) }
                )
            }
        }
    }

    if (showHintDialog && hintBitmap != null) {
        FullscreenImageOverlay(hintBitmap) { showHintDialog = false }
    }
}

@Composable
private fun ScrambleTopBar(
    book: String,
    chapter: Int,
    number: String,
    hintBitmap: android.graphics.Bitmap?,
    onHintClick: () -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "$book $chapter,$number",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            color = colorResource(id = R.color.game_button_yellow_dark)
        )
        if (hintBitmap != null) {
            Spacer(Modifier.width(4.dp))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnswerArea(
    modifier: Modifier,
    state: WordScrambleUiState,
    onEvent: (WordScrambleEvent) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(8.dp)
            .clickable { onEvent(WordScrambleEvent.SelectForReorder(null)) }
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                state.placedWords.forEachIndexed { index, wordItem ->
                    WordChip(
                        text = wordItem.text,
                        isSelected = state.selectedWordForReorder == wordItem,
                        isWrong = state.wrongWords.contains(wordItem),
                        onClick = {
                            if (state.selectedWordForReorder == wordItem) {
                                onEvent(WordScrambleEvent.SelectForReorder(null))
                            } else {
                                onEvent(WordScrambleEvent.UnplaceWord(wordItem))
                            }
                        },
                        onLongClick = {
                            if (state.selectedWordForReorder == wordItem) {
                                onEvent(WordScrambleEvent.SelectForReorder(null))
                            } else {
                                onEvent(WordScrambleEvent.SelectForReorder(wordItem))
                            }
                        },
                        showReorderControls = state.selectedWordForReorder == wordItem,
                        onMoveLeft = { onEvent(WordScrambleEvent.MoveLeft(index)) },
                        onMoveRight = { onEvent(WordScrambleEvent.MoveRight(index)) },
                        onControlLongClick = { onEvent(WordScrambleEvent.SelectForReorder(null)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectionArea(modifier: Modifier, availableWords: List<WordItem>, onWordClick: (WordItem) -> Unit) {
    Box(modifier) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                availableWords.forEach { wordItem ->
                    WordChip(text = wordItem.text, onClick = { onWordClick(wordItem) })
                }
            }
        }
    }
}

@Composable
private fun Controls(modifier: Modifier, checkEnabled: Boolean, onReset: () -> Unit, onCheck: () -> Unit) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = onReset, 
            modifier = Modifier.weight(1f).height(50.dp), 
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.game_button_yellow_dark))
        ) {
            Text(stringResource(id = R.string.reset_button), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        RiddleCheckButton(checkEnabled, onCheck, Modifier.weight(1f).height(50.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WordChip(
    text: String, 
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    isWrong: Boolean = false,
    showReorderControls: Boolean = false,
    onMoveLeft: (() -> Unit)? = null,
    onMoveRight: (() -> Unit)? = null,
    onControlLongClick: (() -> Unit)? = null
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isWrong -> colorResource(R.color.wrong_answer_highlight)
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        isWrong -> Color.Black
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        if (showReorderControls) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .combinedClickable(
                        onClick = { onMoveLeft?.invoke() },
                        onLongClick = { onControlLongClick?.invoke() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.move_left_desc),
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(text, color = contentColor, fontSize = 16.sp)
        if (showReorderControls) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .combinedClickable(
                        onClick = { onMoveRight?.invoke() },
                        onLongClick = { onControlLongClick?.invoke() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = stringResource(id = R.string.move_right_desc),
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
