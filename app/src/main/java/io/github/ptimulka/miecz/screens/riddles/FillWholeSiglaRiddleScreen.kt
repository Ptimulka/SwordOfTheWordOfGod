package io.github.ptimulka.miecz.screens.riddles

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import io.github.ptimulka.miecz.repositories.MnemonicPicturesRepository
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.helpers.BookNameNormalizer
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText

@Composable
fun FillWholeSiglaRiddleScreen(
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
    var bookInput by rememberSaveable { mutableStateOf("") }
    var chapterInput by rememberSaveable { mutableStateOf("") }
    var verseInput by rememberSaveable { mutableStateOf("") }

    var showResultDialog by rememberSaveable { mutableStateOf(false) }
    var isAnswerCorrect by rememberSaveable { mutableStateOf(false) }
    var showImagePreview by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val hintBitmap = remember(sectionId, verseIndex) {
        if (assetName != null || (sectionId > 0 && verseIndex >= 0))
            MnemonicPicturesRepository(context).loadActivePicture(sectionId, verseIndex, assetName)
        else null
    }
    val wrongInputs = rememberSaveable { mutableStateListOf<Int>() }

    val allFieldsFilled by remember {
        derivedStateOf { bookInput.isNotEmpty() && chapterInput.isNotEmpty() && verseInput.isNotEmpty() }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val onCheck = {
        wrongInputs.clear()
        val isBookCorrect = BookNameNormalizer.getCanonicalSigla(bookInput) == book
        val isChapterCorrect = chapterInput == chapter.toString()
        val isVerseCorrect = if (number.contains("-")) {
            val parts = number.split("-").mapNotNull { it.toIntOrNull() }
            if (parts.size == 2) {
                val userNum = verseInput.toIntOrNull()
                userNum != null && userNum >= parts[0] && userNum <= parts[1]
            } else {
                verseInput == number
            }
        } else {
            verseInput == number
        }

        if (!isBookCorrect) wrongInputs.add(0)
        if (!isChapterCorrect) wrongInputs.add(1)
        if (!isVerseCorrect) wrongInputs.add(2)

        isAnswerCorrect = isBookCorrect && isChapterCorrect && isVerseCorrect
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
            title = {
                Text(
                    if (isAnswerCorrect) stringResource(R.string.correct_answer) else stringResource(R.string.wrong_answer),
                    color = if (isAnswerCorrect) colorResource(R.color.correct_answer_green) else Color.Red,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (isAnswerCorrect) stringResource(R.string.success_message) else stringResource(R.string.failure_message)
                )
            },
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
                WholeSiglaInputArea(
                    bookInput,
                    chapterInput,
                    verseInput,
                    { bookInput = it },
                    { chapterInput = it },
                    { verseInput = it },
                    { wrongInputs.contains(0) },
                    { wrongInputs.contains(1) },
                    { wrongInputs.contains(2) }
                )
                Spacer(Modifier.height(32.dp))
                FillSiglaCheckButton(allFieldsFilled, onCheck)
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
            WholeSiglaInputArea(
                bookInput,
                chapterInput,
                verseInput,
                { bookInput = it },
                { chapterInput = it },
                { verseInput = it },
                { wrongInputs.contains(0) },
                { wrongInputs.contains(1) },
                { wrongInputs.contains(2) }
            )
            Spacer(Modifier.height(16.dp))
            FillSiglaCheckButton(allFieldsFilled, onCheck)
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
private fun WholeSiglaInputArea(
    bookInput: String,
    chapterInput: String,
    verseInput: String,
    onBookInputChanged: (String) -> Unit,
    onChapterInputChanged: (String) -> Unit,
    onVerseInputChanged: (String) -> Unit,
    isBookError: () -> Boolean,
    isChapterError: () -> Boolean,
    isVerseError: () -> Boolean,
) {

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
                value = bookInput,
                onValueChange = onBookInputChanged,
                isError = isBookError()
            )
            Text(",", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(8.dp))
            SiglaPartTextField(
                value = chapterInput,
                onValueChange = onChapterInputChanged,
                isError = isChapterError(),
                keyboardType = KeyboardType.Number
            )
            Text(",", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(8.dp))
            SiglaPartTextField(
                value = verseInput,
                onValueChange = onVerseInputChanged,
                isError = isVerseError(),
                keyboardType = KeyboardType.Number
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = R.string.fill_sigla_verse_range_hint),
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

    }
}

@Composable
private fun FillSiglaCheckButton(enabled: Boolean, onCheck: () -> Unit) {
    Button(
        onClick = onCheck,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.game_button_yellow_dark))
    ) {
        Text(stringResource(id = R.string.check_button), fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SiglaPartTextField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text
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
        modifier = Modifier.width(80.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
