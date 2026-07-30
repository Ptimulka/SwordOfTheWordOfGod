package io.github.ptimulka.miecz.components.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText

/**
 * Centered verse text with annotated formatting, shown next to the input in the
 * fill-sigla riddle screens.
 */
@Composable
fun VerseDisplay(modifier: Modifier, verseText: String) {
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
