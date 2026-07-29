package io.github.ptimulka.miecz.screens.riddles

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.game.RiddleHint
import io.github.ptimulka.miecz.helpers.BookNameNormalizer
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText
import io.github.ptimulka.miecz.repositories.MnemonicPicturesRepository

enum class FillSiglaType {
    BOOK,
    CHAPTER,
    VERSE
}

@Composable
fun FillSiglaRiddleScreen(
    verseText: String,
    book: String,
    chapter: Int,
    number: String,
    fillType: FillSiglaType,
    sectionId: Int = 0,
    verseIndex: Int = 0,
    assetName: String? = null,
    onSuccess: () -> Unit,
    onShieldLoss: () -> Boolean
) {
    var userInput by rememberSaveable { mutableStateOf("") }
    var showResultDialog by rememberSaveable { mutableStateOf(false) }
    var isAnswerCorrect by rememberSaveable { mutableStateOf(false) }
    var showImagePreview by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val hintBitmap = remember(sectionId, verseIndex) {
        if (assetName != null || (sectionId > 0 && verseIndex >= 0))
            MnemonicPicturesRepository(context).loadActivePicture(sectionId, verseIndex, assetName)
        else null
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val onCheck = {
        val correct = when (fillType) {
            FillSiglaType.BOOK -> BookNameNormalizer.getCanonicalSigla(userInput) == book
            FillSiglaType.CHAPTER -> userInput == chapter.toString()
            FillSiglaType.VERSE -> {
                if (number.contains("-")) {
                    val parts = number.split("-").mapNotNull { it.toIntOrNull() }
                    if (parts.size == 2) {
                        val userNum = userInput.toIntOrNull()
                        userNum != null && userNum >= parts[0] && userNum <= parts[1]
                    } else {
                        userInput == number
                    }
                } else {
                    userInput == number
                }
            }
        }
        isAnswerCorrect = correct
        if (isAnswerCorrect && hintBitmap != null) {
            showImagePreview = true
        } else {
            showResultDialog = if (!isAnswerCorrect) { !onShieldLoss() } else true
        }
    }

    if (showImagePreview && hintBitmap != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { showImagePreview = false; showResultDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = hintBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.85f)
            )
        }
        return
    }

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

    val scrollState = rememberScrollState()

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
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
                SiglaInputArea(fillType, userInput, { userInput = it }, book, chapter, number)
                Spacer(Modifier.height(32.dp))
                FillSiglaCheckButton(userInput.isNotBlank(), onCheck)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
                .padding(
                    WindowInsets.ime.only(WindowInsetsSides.Bottom).asPaddingValues()
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            VerseDisplay(Modifier, verseText)
            Spacer(Modifier.height(16.dp))
            SiglaInputArea(fillType, userInput, { userInput = it }, book, chapter, number)
            Spacer(Modifier.height(16.dp))
            FillSiglaCheckButton(userInput.isNotBlank(), onCheck)
        }
    }
}

@Composable
private fun VerseDisplay(modifier: Modifier, verseText: String) {
    val annotatedVerseText = remember(verseText) {
        buildAnnotatedVerseText(verseText)
    }
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = annotatedVerseText,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
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
private fun FillSiglaCheckButton(enabled: Boolean, onCheck: () -> Unit) {
    Button(
        onClick = onCheck,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.game_button_yellow_dark))
    ) {
        Text(stringResource(id = R.string.check_button), fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
