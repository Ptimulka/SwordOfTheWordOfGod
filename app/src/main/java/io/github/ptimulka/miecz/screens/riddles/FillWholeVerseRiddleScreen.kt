package io.github.ptimulka.miecz.screens.riddles

import android.app.Activity
import android.content.res.Configuration
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import io.github.ptimulka.miecz.repositories.MnemonicPicturesRepository
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.helpers.calculateWordSimilarity
import io.github.ptimulka.miecz.helpers.createPolishSpeechIntent
import io.github.ptimulka.miecz.helpers.normalizeVerseText

// Data structures for the diffing algorithm
private enum class DiffType { INSERT, DELETE, EQUAL }
private data class DiffPart(val type: DiffType, val text: String)

@Composable
fun FillWholeVerseRiddleScreen(
    verseText: String,
    book: String,
    chapter: Int,
    number: String,
    sectionId: Int,
    verseIndex: Int,
    assetName: String? = null,
    onSuccess: () -> Unit,
    onShieldLoss: () -> Boolean
) {
    var userInput by rememberSaveable { mutableStateOf("") }
    var showResultDialog by rememberSaveable { mutableStateOf(false) }
    var isAnswerCorrect by rememberSaveable { mutableStateOf(false) }
    var similarityScore by rememberSaveable { mutableStateOf(0.0f) }
    var diffs by rememberSaveable { mutableStateOf<List<DiffPart>>(emptyList()) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current
    val hintBitmap = remember(sectionId, verseIndex) {
        if (assetName != null || (sectionId > 0 && verseIndex >= 0)) {
            val repo = MnemonicPicturesRepository(context)
            repo.loadActivePicture(sectionId, verseIndex, assetName)
        } else null
    }
    var showHintDialog by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            userInput = results?.get(0) ?: ""
        }
    }

    fun checkAnswer() {
        val userWords = normalizeVerseText(userInput).split(' ').filter { it.isNotEmpty() }

        val verseWithSpaces = verseText.replace("_", " ")

        // Version 1: With text in asterisks (the optional parts)
        val wordsWithOptional = normalizeVerseText(verseWithSpaces.replace("*", "")).split(' ').filter { it.isNotEmpty() }

        // Version 2: Without text in asterisks
        val wordsWithoutOptional = normalizeVerseText(verseWithSpaces.replace(Regex("\\*.*?\\*"), "")).split(' ').filter { it.isNotEmpty() }

        // Calculate similarity for both versions
        val similarity1 = calculateWordSimilarity(userWords, wordsWithOptional)
        val similarity2 = calculateWordSimilarity(userWords, wordsWithoutOptional)

        // Choose the best match
        val bestCorrectWords: List<String>
        if (similarity1 > similarity2) {
            similarityScore = similarity1
            bestCorrectWords = wordsWithOptional
        } else {
            similarityScore = similarity2
            bestCorrectWords = wordsWithoutOptional
        }

        isAnswerCorrect = similarityScore >= 90.0f
        if (similarityScore < 100.0f) {
            diffs = diffWords(bestCorrectWords.joinToString(" "), userWords.joinToString(" "))
        } else {
            diffs = emptyList()
        }
        showResultDialog = if(!isAnswerCorrect) { !onShieldLoss() } else true
    }

    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = {
                // Do nothing to prevent dismissal on click outside or back press
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = {
                Text(
                    if (isAnswerCorrect) stringResource(R.string.correct_answer) else stringResource(R.string.wrong_answer),
                    color = if (isAnswerCorrect) colorResource(R.color.correct_answer_green) else Color.Red,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        if (isAnswerCorrect) {
                            stringResource(R.string.success_message)
                        } else  {
                            stringResource(R.string.failure_message)
                        }
                    )
                    if (similarityScore < 100f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.similarity_score, similarityScore))
                        Text(
                            text = stringResource(id = R.string.required_similarity_info),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        DiffView(diffs = diffs)
                    }
                }
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

    val textFieldHeight = if(isLandscape) 130.dp else 290.dp

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
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
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

        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(textFieldHeight),
            label = { Text(stringResource(id = R.string.fill_whole_verse_caption)) }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {

            Text(
                text = stringResource(id = R.string.skip_possibility_info),
                modifier = Modifier
                    .weight(0.9f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color.Gray
            )

            val extraPrompt = stringResource(R.string.speak_now)
            IconButton(onClick = {
                speechLauncher.launch(createPolishSpeechIntent(prompt = extraPrompt))
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.microphone),
                    contentDescription = stringResource(R.string.speak_now)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = ::checkAnswer,
            enabled = userInput.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.game_button_yellow_dark))
        ) {
            Text(
                stringResource(id = R.string.check_button),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
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


private fun diffWords(original: String, revised: String): List<DiffPart> {
    val originalWords = original.split(' ').filter { it.isNotEmpty() }
    val revisedWords = revised.split(' ').filter { it.isNotEmpty() }
    val n = originalWords.size
    val m = revisedWords.size

    val lcs = Array(n + 1) { IntArray(m + 1) }
    for (i in 0..n) {
        for (j in 0..m) {
            if (i == 0 || j == 0) {
                lcs[i][j] = 0
            } else if (originalWords[i - 1] == revisedWords[j - 1]) {
                lcs[i][j] = lcs[i - 1][j - 1] + 1
            } else {
                lcs[i][j] = maxOf(lcs[i - 1][j], lcs[i][j - 1])
            }
        }
    }

    val diffs = mutableListOf<DiffPart>()
    var i = n
    var j = m
    while (i > 0 || j > 0) {
        if (i > 0 && j > 0 && originalWords[i - 1] == revisedWords[j - 1]) {
            diffs.add(DiffPart(DiffType.EQUAL, originalWords[i - 1]))
            i--
            j--
        } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
            diffs.add(DiffPart(DiffType.INSERT, revisedWords[j - 1]))
            j--
        } else if (i > 0 && (j == 0 || lcs[i][j - 1] < lcs[i - 1][j])) {
            diffs.add(DiffPart(DiffType.DELETE, originalWords[i - 1]))
            i--
        }
    }
    return diffs.reversed()
}
