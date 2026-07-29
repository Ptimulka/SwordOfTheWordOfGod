package io.github.ptimulka.miecz.components.game

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign

/**
 * Small italic gray hint shown below riddle inputs (e.g. verse-range note,
 * "no diacritics needed" note). Keeps the styling consistent across riddle screens.
 */
@Composable
fun RiddleHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontStyle = FontStyle.Italic,
        color = Color.Gray,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}
