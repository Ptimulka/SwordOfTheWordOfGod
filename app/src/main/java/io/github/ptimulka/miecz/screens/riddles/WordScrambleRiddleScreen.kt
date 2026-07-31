package io.github.ptimulka.miecz.screens.riddles

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.game.FullscreenImageOverlay
import io.github.ptimulka.miecz.components.game.RiddleCheckButton
import io.github.ptimulka.miecz.components.game.RiddleResultDialog
import io.github.ptimulka.miecz.components.game.rememberMnemonicPicture
import io.github.ptimulka.miecz.data.WordItem
import java.util.Collections
import java.util.regex.Pattern

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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val hintBitmap = rememberMnemonicPicture(sectionId, verseIndex, assetName)
    var showHintDialog by remember { mutableStateOf(false) }

    val correctWords = remember(verseText, isEasy) { 
        val words = mutableListOf<String>()
        val textToParse = if (isEasy) verseText else verseText.replace('_', ' ')
        val matcher = Pattern.compile("\\*(.*?)\\*|\\S+").matcher(textToParse)
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                words.add(matcher.group(1)!!)
            } else {
                var word = matcher.group()
                if (isEasy) {
                    word = word.replace('_', ' ')
                }
                words.add(word)
            }
        }
        words
    }
    
    val initialShuffledWords = remember {
        correctWords.mapIndexed { index, word -> WordItem(index, word) }.shuffled()
    }

    val placedWords = rememberSaveable(saver = listSaver(
        save = { stateList -> stateList.toList() },
        restore = { it.toMutableStateList() }
    )) { mutableStateListOf<WordItem>() }

    val availableWords = rememberSaveable(saver = listSaver(
        save = { stateList -> stateList.toList() },
        restore = { it.toMutableStateList() }
    )) {
       val placedIds = placedWords.map { it.id }
        initialShuffledWords.filter { it.id !in placedIds }.toMutableStateList()
    }

    val wrongWords = rememberSaveable(saver = listSaver(
        save = { stateList -> stateList.toList() },
        restore = { it.toMutableStateList() }
    )) { mutableStateListOf<WordItem>() }

    var showResultDialog by rememberSaveable { mutableStateOf(false) }
    var isAnswerCorrect by rememberSaveable { mutableStateOf(false) }
    var selectedWordForReorder by rememberSaveable { mutableStateOf<WordItem?>(null) }

    if (showResultDialog) {
        RiddleResultDialog(
            isCorrect = isAnswerCorrect,
            onConfirm = {
                showResultDialog = false
                if (isAnswerCorrect) onSuccess()
            }
        )
    }

    val answerArea = @Composable { modifier: Modifier ->
        AnswerArea(modifier, placedWords, selectedWordForReorder, wrongWords,
            onWordClick = {
                if (selectedWordForReorder == it) {
                    // Deselect if clicked while in moving mode
                    selectedWordForReorder = null
                } else {
                    // Otherwise, move back to available words
                    placedWords.remove(it)
                    availableWords.add(it)
                    wrongWords.clear()
                }
            },
            onLongClick = {
                if (selectedWordForReorder == it) {
                    // Deselect if clicked while in moving mode
                    selectedWordForReorder = null
                } else {
                    // Otherwise select for moving mode
                    selectedWordForReorder = it
                    wrongWords.clear()
                }
            },
            onMoveLeft = { index ->
                if (index > 0) {
                    Collections.swap(placedWords, index, index - 1)
                    wrongWords.clear()
                }
            },
            onMoveRight = { index ->
                if (index < placedWords.size - 1) {
                    Collections.swap(placedWords, index, index + 1)
                    wrongWords.clear()
                }
            },
            onBackgroundClick = { selectedWordForReorder = null }
        )
    }

    val selectionArea = @Composable { modifier: Modifier ->
        SelectionArea(modifier, availableWords) {
            availableWords.remove(it)
            placedWords.add(it)
            wrongWords.clear()
        }
    }

    val controls = @Composable { modifier: Modifier ->
        Controls(modifier, availableWords.isEmpty(),
            onReset = {
                placedWords.clear()
                availableWords.clear()
                availableWords.addAll(initialShuffledWords)
                selectedWordForReorder = null
                wrongWords.clear()
            },
            onCheck = {
                wrongWords.clear()
                var allCorrect = true
                if (placedWords.size != correctWords.size) {
                    allCorrect = false
                } else {
                    placedWords.forEachIndexed { index, wordItem ->
                        if (wordItem.text != correctWords[index]) {
                            wrongWords.add(wordItem)
                            allCorrect = false
                        }
                    }
                }
                isAnswerCorrect = allCorrect
                showResultDialog = if(!isAnswerCorrect) { !onShieldLoss() } else true
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Common Top Bar
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$book $chapter,$number",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                    color = colorResource(id = R.color.game_button_yellow_dark)
                )
                if (hintBitmap != null) {
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { showHintDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_hint),
                            contentDescription = stringResource(R.string.image_hint),
                            tint = colorResource(id = R.color.game_button_yellow_dark)
                        )
                    }
                }
            }

            if (isLandscape) {
                Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    answerArea(Modifier.weight(1f).fillMaxHeight())
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        selectionArea(Modifier.weight(1f))
                        Spacer(Modifier.height(16.dp))
                        controls(Modifier.fillMaxWidth())
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    answerArea(Modifier.weight(1f))
                    Spacer(Modifier.height(16.dp))
                    selectionArea(Modifier.weight(1f))
                    Spacer(Modifier.height(16.dp))
                    controls(Modifier.fillMaxWidth())
                }
            }
        } // end inner Column

        // Full-screen image hint overlay — tap anywhere to dismiss
        if (showHintDialog && hintBitmap != null) {
            FullscreenImageOverlay(hintBitmap) { showHintDialog = false }
        }
    } // end outer Box
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnswerArea(
    modifier: Modifier,
    placedWords: List<WordItem>,
    selectedWord: WordItem?,
    wrongWords: List<WordItem>,
    onWordClick: (WordItem) -> Unit,
    onLongClick: (WordItem) -> Unit,
    onMoveLeft: (Int) -> Unit,
    onMoveRight: (Int) -> Unit,
    onBackgroundClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(8.dp)
            .clickable(onClick = onBackgroundClick)
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                placedWords.forEachIndexed { index, wordItem ->
                    WordChip(
                        text = wordItem.text,
                        isSelected = selectedWord == wordItem,
                        isWrong = wrongWords.contains(wordItem),
                        onClick = { onWordClick(wordItem) },
                        onLongClick = { onLongClick(wordItem) },
                        showReorderControls = selectedWord == wordItem,
                        onMoveLeft = { onMoveLeft(index) },
                        onMoveRight = { onMoveRight(index) },
                        onControlLongClick = { onLongClick(wordItem) }
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
        Button(onClick = onReset, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.game_button_yellow_dark))) {
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
        modifier = Modifier.padding(2.dp).clip(RoundedCornerShape(8.dp)).background(backgroundColor).combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 8.dp, vertical = 4.dp)
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
