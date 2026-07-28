package io.github.ptimulka.miecz.screens.riddles

import io.github.ptimulka.miecz.helpers.foldPolishChars
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import io.github.ptimulka.miecz.repositories.MnemonicPicturesRepository
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.R

sealed class VersePart
private data class StaticText(val text: String, val isGreyed: Boolean = false) : VersePart()
private data class WordToFill(val correctWord: String, val hint: String) : VersePart()

@OptIn(ExperimentalLayoutApi::class)
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
    assetName: String? = null,
    onSuccess: () -> Unit,
    onShieldLoss: () -> Boolean
) {
    val verseParts = remember(verseText, isEasy, moreWords) {
        val parts = mutableListOf<VersePart>()
        val normalizedText = verseText.replace('_', ' ')
        val segments = normalizedText.split('*')

        var wordCounter = 0
        segments.forEachIndexed { index, segment ->
            val isInsideAsterisks = index % 2 != 0

            if (isInsideAsterisks) {
                parts.add(StaticText(segment, isGreyed = true))
            } else {
                val words = segment.split(' ').filter { it.isNotBlank() }
                for (wordWithPunctuation in words) {
                    val coreWord = wordWithPunctuation.filter { it.isLetterOrDigit() }
                    val punctuation = wordWithPunctuation.filterNot { it.isLetterOrDigit() }
                    val isEligible = coreWord.length >= 3

                    if (isEligible) {
                        wordCounter++
                    }

                    val shouldBeFilled = if (moreWords) {
                        if (isEasy) {
                            wordCounter % 3 == 1 || wordCounter % 3 == 2
                        } else { // normal
                            wordCounter % 3 == 0 || wordCounter % 3 == 2
                        }
                    } else { // not moreWords
                        if (isEasy) {
                            wordCounter % 3 == 0
                        } else { // normal
                            wordCounter % 3 == 1
                        }
                    }

                    if (isEligible && shouldBeFilled) {
                        val hint = if (isEasy) coreWord.first().toString() else ""
                        parts.add(WordToFill(coreWord, hint))
                        if (punctuation.isNotEmpty()) {
                            parts.add(StaticText(punctuation))
                        }
                    } else {
                        parts.add(StaticText(wordWithPunctuation))
                    }
                }
            }
        }
        parts
    }

    val userInputs = rememberSaveable(verseParts, saver = listSaver(
        save = { it.toList() },
        restore = { it.toMutableStateList() }
    )) {
        verseParts.filterIsInstance<WordToFill>().map { "" }.toMutableStateList()
    }

    var showResultDialog by rememberSaveable { mutableStateOf(false) }
    var isAnswerCorrect by rememberSaveable { mutableStateOf(false) }
    val wrongInputs = rememberSaveable(saver = listSaver(
        save = { it.toList() },
        restore = { it.toMutableStateList() }
    )) { mutableStateListOf<Int>() }

    val allFieldsFilled by remember {
        derivedStateOf { userInputs.all { it.isNotEmpty() } }
    }

    val context = LocalContext.current
    val hintBitmap = remember(sectionId, verseIndex) {
        if (assetName != null || (sectionId > 0 && verseIndex >= 0)) {
            val repo = MnemonicPicturesRepository(context)
            repo.loadActivePicture(sectionId, verseIndex, assetName)
        } else null
    }
    var showHintDialog by remember { mutableStateOf(false) }

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

    Box(Modifier.fillMaxSize()) {
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
                text = "$book $chapter,$number",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                color = colorResource(id = R.color.game_button_yellow_dark)
            )
            if (hintBitmap != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { showHintDialog = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_hint),
                        contentDescription = stringResource(R.string.image_hint),
                        tint = colorResource(id = R.color.game_button_yellow_dark)
                    )
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
            var inputIndex = 0
            verseParts.forEach {
                when (it) {
                    is StaticText -> {
                        Text(
                            text = " ${it.text}",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 6.dp),
                            color = if (it.isGreyed) Color.Gray else Color.Unspecified
                        )
                    }
                    is WordToFill -> {
                        val currentIndex = inputIndex
                        CompactOutlinedTextField(
                            value = userInputs[currentIndex],
                            onValueChange = { userInputs[currentIndex] = it },
                            placeholder = {
                                if (isEasy) Text(
                                    it.hint,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            isError = wrongInputs.contains(currentIndex),
                            modifier = Modifier
                                .width((it.correctWord.length * 12).dp + 32.dp)
                                .height(40.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            singleLine = true,
                        )
                        inputIndex++
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                wrongInputs.clear()
                var allCorrect = true
                var i = 0
                verseParts.filterIsInstance<WordToFill>().forEach { wordToFill ->
                    if (foldPolishChars(userInputs[i].trim()).equals(foldPolishChars(wordToFill.correctWord), ignoreCase = true)) {
                        // Correct
                    } else {
                        wrongInputs.add(i)
                        allCorrect = false
                    }
                    i++
                }
                isAnswerCorrect = allCorrect
                showResultDialog = if(!isAnswerCorrect) { !onShieldLoss() } else true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = allFieldsFilled,
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.game_button_yellow_dark))
        ) {
            Text(stringResource(id = R.string.check_button), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    } // end Column

    // Full-screen image hint overlay — tap anywhere to dismiss
    if (showHintDialog && hintBitmap != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { showHintDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = hintBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.85f)
            )
        }
    }
    } // end Box
}


@Composable
private fun CompactOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean,
    placeholder: @Composable () -> Unit,
    keyboardOptions: KeyboardOptions,
    singleLine: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Determine border color based on state
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
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
                    .padding(vertical = 4.dp, horizontal = 8.dp) // Custom inner padding!
            ) {
                if (value.isEmpty() && !isFocused) {
                    placeholder()
                }
                innerTextField()
            }
        }
    )
}
